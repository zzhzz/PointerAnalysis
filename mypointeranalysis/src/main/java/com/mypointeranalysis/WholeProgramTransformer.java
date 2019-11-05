package com.mypointeranalysis;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import soot.Kind;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

import soot.jimple.CastExpr;
import soot.jimple.ThrowStmt;
import soot.jimple.ArrayRef;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.util.queue.QueueReader;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

class TestCase {
	int id;
	String method, local;

	TestCase(int id, String method, String local) {
		this.id = id;
		this.method = method;
		this.local = local;
	}
}

class Invocation {
	int call_id;
	SootMethod sm;

	Invocation(int call_id, SootMethod sm) {
		this.call_id = call_id;
		this.sm = sm;
	}
}

public class WholeProgramTransformer extends SceneTransformer {

	boolean shouldanalysis(SootMethod sm) {
		String packagename = sm.getDeclaringClass().getPackageName();
		return (packagename.startsWith("benchmark") || packagename.startsWith("test"))
				|| consider_methods.contains(getMethodName(sm));
	}

	public static String getMethodName(SootMethod sm) {
		return sm.getDeclaringClass().getName() + "::" + sm.getName() + sm.getParameterTypes().toString();
	}

	public static String getLocalName(Local l) {
		return l.getName();
	}

	public static String getFieldName(SootField field) {
		return field.getSignature();
	}

	Anderson anderson = new Anderson();
	IdentityHashMap<Stmt, List<Invocation>> call_relation = new IdentityHashMap<>();
	IdentityHashMap<SootMethod, Object> entries = new IdentityHashMap<>();

	List<TestCase> testcases = new ArrayList<>();
	Set<String> consider_methods = new HashSet<>();

	Set<Integer> allallocids = new TreeSet<>();

	void process_edges() {
		SootClass entry_class = Scene.v().getSootClass(MyPointerAnalysis.entryclass);
		SootMethod entry_method = entry_class.getMethodByName("main");
		CallGraph cg = Scene.v().getCallGraph();
		List<SootMethod> ents = Scene.v().getEntryPoints();

		Queue<SootMethod> search_queue = new ArrayDeque<>();

		search_queue.add(entry_method);
		for (SootMethod sm : ents) {
			if (shouldanalysis(sm)) {
				search_queue.add(sm);
			}
		}

		while (!search_queue.isEmpty()) {
			SootMethod cur = search_queue.remove();
			String cur_name = getMethodName(cur);
			if (!consider_methods.add(cur_name))
				continue;
			Iterator<Edge> edges = cg.edgesOutOf(cur);
			while (edges.hasNext()) {
				Edge e = edges.next();
				search_queue.add(e.tgt());
			}
		}

		int cur_call_id = 0;
		for (SootMethod sm : ents) {
			entries.put(sm, null);
			if (!shouldanalysis(sm))
				continue;
			anderson.addFunctionCopy(getMethodName(sm), cur_call_id++);
			// System.out.println("EntryPoints: " + sm.toString());
		}

		QueueReader<Edge> edges = cg.listener();
		while (edges.hasNext()) {
			Edge e = edges.next();
			if (!shouldanalysis(e.src()) || !shouldanalysis(e.tgt()))
				continue;
			Kind k = e.kind();
			String kname = k.name();
			if (kname == "FINALIZE" || kname == "THREAD" || kname == "CLINIT" || kname == "PRIVILEGED") {
				entries.put(e.tgt(), null);
				anderson.addFunctionCopy(getMethodName(e.tgt()), -1);
			} else if (kname == "VIRTUAL" || kname == "STATIC" || kname == "SPECIAL" || kname == "INTERFACE") {
				if (!call_relation.containsKey(e.srcStmt()))
					call_relation.put(e.srcStmt(), new ArrayList<Invocation>());
				int thiscallid = cur_call_id++;
				call_relation.get(e.srcStmt()).add(new Invocation(thiscallid, e.tgt()));
				anderson.addFunctionCopy(getMethodName(e.tgt()), thiscallid);
			} else {
				System.out.println("Ignore edge: " + e.toString());
			}
		}
	}

	void process_one_invoke_expr(String method_from, InvokeExpr expr, Stmt u, Local returnlocal) {
		if (!call_relation.containsKey(u))
			return;
		for (Invocation inv : call_relation.get(u)) {
			process_one_invoke(method_from, expr, inv.sm, inv.call_id, returnlocal);
		}
	}

	void process_one_invoke(String method_from, InvokeExpr from, SootMethod method_to, int call_id_to,
			Local returnlocal) {
		String method_to_name = getMethodName(method_to);
		int argcount = from.getArgCount();
		if (argcount != method_to.getParameterCount()) {
			System.out.println("NotEqual: " + from.toString());
			MyAssert.myassert(false);
			return;
		}
		if (from instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr ine = (InstanceInvokeExpr) from;
			Local base = (Local) ine.getBase();
			anderson.addAssignConstraint_inter_toid(method_from, getLocalName(base), method_to_name, call_id_to,
					"@this");
		}
		for (int i = 0; i < argcount; ++i) {
			if (!(method_to.getParameterType(i) instanceof RefLikeType))
				continue;
			Value v = from.getArg(i);
			MyAssert.myassert(v instanceof NullConstant || v instanceof Local);
			if (v instanceof Local) {
				Local l = (Local) v;
				anderson.addAssignConstraint_inter_toid(method_from, getLocalName(l), method_to_name, call_id_to,
						"@parameter" + Integer.toString(i));
			}
		}

		if (returnlocal != null) {
			anderson.addAssignConstraint_inter_fromid(method_to_name, call_id_to, "@ret", method_from,
					getLocalName(returnlocal));
		}
	}

	void process_method(SootMethod sm) {
		String sm_name = getMethodName(sm);
		int allocid = 0;

		if (!shouldanalysis(sm))
			return;

		System.out.println("Process: " + sm_name);

		if (!sm.hasActiveBody())
			return;

		JimpleBody jb = (JimpleBody) sm.getActiveBody();

		for (Local l : jb.getLocals()) {
			Type lt = l.getType();
			if (lt instanceof RefLikeType) {
				anderson.addLocal(sm_name, l.getName(), TypeInfo.getTypeInfo((RefLikeType) lt));
			}
		}

		for (int i = 0; i < sm.getParameterCount(); ++i) {
			Type lt = sm.getParameterType(i);
			if (lt instanceof RefLikeType) {
				anderson.addLocal(sm_name, "@parameter" + Integer.toString(i), TypeInfo.getTypeInfo((RefLikeType) lt));
			}
		}

		if (!sm.isStatic()) {
			anderson.addLocal(sm_name, "@this", TypeInfo.getTypeInfo(RefType.v(sm.getDeclaringClass())));
		}

		Type rt = sm.getReturnType();
		if (rt instanceof RefLikeType) {
			anderson.addLocal(sm_name, "@ret", TypeInfo.getTypeInfo((RefLikeType) rt));
		}

		for (Unit u : jb.getUnits()) {
			System.out.println(u);
			if (u instanceof InvokeStmt) {
				InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
				SootMethod invoke_sm = ie.getMethod();
				if (invoke_sm.getDeclaringClass().getName().equals("benchmark.internal.BenchmarkN")) {
					if (invoke_sm.getName().equals("alloc")) {
						allocid = ((IntConstant) ie.getArgs().get(0)).value;
						allallocids.add(allocid);
					} else if (invoke_sm.getName().equals("test")) {
						int testcase_id = ((IntConstant) ie.getArg(0)).value;
						Local testcase_local = ((Local) ie.getArg(1));
						// if (!testcases.containsKey(testcase_id))
						// testcases.put(testcase_id, new ArrayList<TestCase>());
						// testcases.get(testcase_id).add(new TestCase(sm_name,
						// getLocalName(testcase_local)));
						testcases.add(new TestCase(testcase_id, sm_name, getLocalName(testcase_local)));
					}
				} else {
					process_one_invoke_expr(sm_name, ie, (Stmt) u, null);
				}
			} else if (u instanceof DefinitionStmt) {
				Value left = ((DefinitionStmt) u).getLeftOp();
				Value right = ((DefinitionStmt) u).getRightOp();
				if (!(left instanceof Local) && !(right instanceof Local) && !(right instanceof Constant)) {
					System.out.println("Left and right: " + u.toString());
					MyAssert.myassert(false);
				} else if (right instanceof Constant) {
					// right is value or null, do nothing.
				} else if (left instanceof Local && right instanceof Local) {
					// local to local assignment
					Local localleft = (Local) left;
					Local localright = (Local) right;
					if ((localleft.getType() instanceof RefLikeType)) {
						anderson.addAssignConstraint_intra(sm_name, getLocalName(localright), getLocalName(localleft));
					}
				} else if (left instanceof Local) {
					// to local assignment
					Local localleft = (Local) left;
					String localleft_name = getLocalName(localleft);
					if (!(localleft.getType() instanceof RefLikeType)) {
						if (right instanceof InvokeExpr) {
							InvokeExpr ie = (InvokeExpr) right;
							process_one_invoke_expr(sm_name, ie, (Stmt) u, null);
						}
					} else {
						if (right instanceof ArrayRef) {
							ArrayRef ar = (ArrayRef) right;
							Value base = ar.getBase();
							if (base instanceof Local) {
								anderson.addAssignConstraint_intra_from_filed(sm_name, getLocalName((Local) base),
										"#arrayvalue", localleft_name);
							} else {
								MyAssert.myassert(false);
							}
						} else if (right instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) right;
							Value base = ifr.getBase();
							if (base instanceof Local) {
								anderson.addAssignConstraint_intra_from_filed(sm_name, getLocalName((Local) base),
										getFieldName(ifr.getField()), localleft_name);
							} else {
								MyAssert.myassert(false);
							}
						} else if (right instanceof StaticFieldRef) {
							StaticFieldRef sfr = (StaticFieldRef) right;
							anderson.addAssignConstraint_intra_from_static(sm_name, getFieldName(sfr.getField()),
									localleft_name);
						} else if (right instanceof CastExpr) {
							CastExpr ce = (CastExpr) right;
							if (ce.getOp() instanceof Local) {
								anderson.addAssignConstraint_intra(sm_name, getLocalName((Local) ce.getOp()),
										localleft_name);
							} else if (ce.getOp() instanceof NullConstant) {
							} else {
								MyAssert.myassert(false);
							}
						} else if (right instanceof InvokeExpr) {
							InvokeExpr ie = (InvokeExpr) right;
							process_one_invoke_expr(sm_name, ie, (Stmt) u, localleft);
						} else if (right instanceof NewArrayExpr) {
							NewArrayExpr nae = (NewArrayExpr) right;
							RefLikeType t = nae.getBaseType().makeArrayType();
							anderson.addNew(sm_name, localleft_name, allocid, TypeInfo.getTypeInfo(t));
							allocid = 0;
						} else if (right instanceof NewExpr) {
							NewExpr ne = (NewExpr) right;
							RefLikeType t = ne.getBaseType();
							anderson.addNew(sm_name, localleft_name, allocid, TypeInfo.getTypeInfo(t));
							allocid = 0;
						} else if (right instanceof NewMultiArrayExpr) {
							// NewMultiArrayExpr nmae = (NewMultiArrayExpr)right;
							// nmae.getBaseType();
							// anderson.addNew(sm_name, localleft_name, allocid);
							allocid = 0;
						} else if (right instanceof CaughtExceptionRef) {
							anderson.addAssignConstraint_intra_from_static(sm_name, "@caughtexception", localleft_name);
						} else if (right instanceof ParameterRef) {
							ParameterRef pr = (ParameterRef) right;
							anderson.addAssignConstraint_intra(sm_name, "@parameter" + Integer.toString(pr.getIndex()),
									localleft_name);
						} else if (right instanceof ThisRef) {
							anderson.addAssignConstraint_intra(sm_name, "@this", localleft_name);
						} else {
							MyAssert.myassert(false);
						}
					}
				} else if (right instanceof Local) {
					// from local assignment
					Local localright = (Local) right;
					String localright_name = getLocalName(localright);
					if (localright.getType() instanceof RefLikeType) {
						if (left instanceof ArrayRef) {
							ArrayRef ar = (ArrayRef) left;
							Value base = ar.getBase();
							if (base instanceof Local) {
								anderson.addAssignConstraint_intra_to_field(sm_name, localright_name,
										getLocalName((Local) base), "#arrayvalue");
							} else {
								MyAssert.myassert(false);
							}
						} else if (left instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) left;
							Value base = ifr.getBase();
							if (base instanceof Local) {
								anderson.addAssignConstraint_intra_to_field(sm_name, localright_name,
										getLocalName((Local) base), getFieldName(ifr.getField()));
							} else {
								MyAssert.myassert(false);
							}
						} else if (left instanceof StaticFieldRef) {
							StaticFieldRef sfr = (StaticFieldRef) left;
							anderson.addAssignConstraint_intra_to_static(sm_name, localright_name,
									getFieldName(sfr.getField()));
						} else {
							MyAssert.myassert(false);
						}
					}
				} else {
					MyAssert.myassert(false);
				}
			} else if (u instanceof ThrowStmt) {
				ThrowStmt ts = (ThrowStmt) u;
				Value v = ts.getOp();
				if (v instanceof Local) {
					anderson.addAssignConstraint_intra_to_static(sm_name, "@caughtexception", getLocalName((Local) v));
				} else {
					MyAssert.myassert(false);
				}
			} else if (u instanceof ReturnStmt) {
				ReturnStmt rs = (ReturnStmt) u;
				Value v = rs.getOp();
				if (v.getType() instanceof RefLikeType) {
					if (v instanceof Local) {
						anderson.addAssignConstraint_intra(sm_name, getLocalName((Local) v), "@ret");
					} else {
						MyAssert.myassert(v instanceof NullConstant);
					}
				}
			} else if (u instanceof IfStmt) {
			} else if (u instanceof ReturnVoidStmt) {
			} else if (u instanceof GotoStmt) {
			} else if (u instanceof EnterMonitorStmt) {
			} else if (u instanceof ExitMonitorStmt) {
			} else if (u instanceof TableSwitchStmt) {
			} else if (u instanceof LookupSwitchStmt) {
			} else {
				MyAssert.myassert(false);
			}
		}
	}

	void print_testcases() {
		anderson.printall();
		StringBuilder builder = new StringBuilder();
		for (TestCase tc : testcases) {
			Set<Integer> results = anderson.getAllocIds(tc.method, tc.local);
			builder.append(Integer.toString(tc.id));
			builder.append(":");
			for (int i : results) {
				if (i != 0) {
					builder.append(" ");
					builder.append(Integer.toString(i));
				}
			}
			builder.append("\n");
		}
		AnswerPrinter.printAnswer(builder.toString());
	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		process_edges();

		QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();
		while (qr.hasNext()) {
			process_method(qr.next().method());
		}
		anderson.run();
		print_testcases();
	}
}
