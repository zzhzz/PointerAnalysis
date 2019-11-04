package com.mypointeranalysis;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.TreeMap;
import java.util.TreeSet;

import soot.Kind;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.RefLikeType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootField;
import soot.SootMethod;
import soot.StmtAddressType;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.ThrowStmt;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.VirtualInvokeExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

class TestCase {
	String method, local;

	TestCase(String method, String local) {
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

	public static String getMethodName(SootMethod sm) {
		return sm.getSignature();
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

	HashMap<Integer, List<TestCase>> testcases = new HashMap<>();

	void process_edges() {
		int cur_call_id = 0;
		List<SootMethod> ents = Scene.v().getEntryPoints();
		for (SootMethod sm : ents) {
			entries.put(sm, null);
			System.out.println("EntryPoints: " + sm.toString());
		}

		CallGraph cg = Scene.v().getCallGraph();
		QueueReader<Edge> edges = cg.listener();
		while (edges.hasNext()) {
			Edge e = edges.next();
			Kind k = e.kind();
			String kname = k.name();
			if (kname == "FINALIZE" || kname == "THREAD" || kname == "CLINIT") {
				entries.put(e.tgt(), null);
			} else if (kname == "VIRTUAL" || kname == "STATIC" || kname == "SPECIAL" || kname == "INTERFACE") {
				if (!call_relation.containsKey(e.srcStmt()))
					call_relation.put(e.srcStmt(), new ArrayList<Invocation>());
				call_relation.get(e.srcStmt()).add(new Invocation(cur_call_id++, e.tgt()));
			} else {
				// System.out.println("Ignore edge: " + e.toString());
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
		// System.out.println("ArgCount:" + Integer.toString(argcount) + " " +
		// Integer.toString(method_to.getParameterCount()));
		if (argcount != method_to.getParameterCount()) {
			System.out.println("NotEqual: " + from.toString());
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
			assert (v instanceof NullConstant || v instanceof Local);
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

		if (!sm.hasActiveBody())
			return;

		for (Unit u : sm.getActiveBody().getUnits()) {
			if (u instanceof InvokeStmt) {
				InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
				SootMethod invoke_sm = ie.getMethod();
				if (invoke_sm.getDeclaringClass().getName().equals("benchmark.internal.BenchmarkN")) {
					if (invoke_sm.getName().equals("alloc")) {
						allocid = ((IntConstant) ie.getArgs().get(0)).value;
					} else if (invoke_sm.getName().equals("test")) {
						int testcase_id = ((IntConstant) ie.getArg(0)).value;
						Local testcase_local = ((Local) ie.getArg(1));
						if (!testcases.containsKey(testcase_id))
							testcases.put(testcase_id, new ArrayList<TestCase>());
						testcases.get(testcase_id).add(new TestCase(sm_name, getLocalName(testcase_local)));
					}
				} else {
					process_one_invoke_expr(sm_name, ie, (Stmt) u, null);
				}
			} else if (u instanceof DefinitionStmt) {
				Value left = ((DefinitionStmt) u).getLeftOp();
				Value right = ((DefinitionStmt) u).getRightOp();
				if (!(left instanceof Local) && !(right instanceof Local) && !(right instanceof Constant)) {
					System.out.println("Left and right: " + u.toString());
					assert false;
				}
				if (right instanceof Constant) {
					// right is value or null, do nothing.
				} else if (left instanceof Local && right instanceof Local) {
					// local to local assignment
					Local localleft = (Local) left;
					Local localright = (Local) right;
					if ((localleft.getType() instanceof RefLikeType)) {
						anderson.addAssignConstraint_intra(sm_name, getLocalName(localleft), getLocalName(localright));
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
							assert base instanceof Local;
							anderson.addAssignConstraint_intra_from_filed(sm_name, getLocalName((Local) base),
									"#arrayvalue", localleft_name);
						} else if (right instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) right;
							Value base = ifr.getBase();
							assert base instanceof Local;
							anderson.addAssignConstraint_intra_from_filed(sm_name, getLocalName((Local) base),
									getFieldName(ifr.getField()), localleft_name);
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
								assert false;
							}
						} else if (right instanceof InvokeExpr) {
							InvokeExpr ie = (InvokeExpr) right;
							process_one_invoke_expr(sm_name, ie, (Stmt) u, localleft);
						} else if (right instanceof NewArrayExpr) {
							anderson.addNew(sm_name, localleft_name, allocid);
							allocid = 0;
						} else if (right instanceof NewExpr) {
							anderson.addNew(sm_name, localleft_name, allocid);
							allocid = 0;
						} else if (right instanceof NewMultiArrayExpr) {
							anderson.addNew(sm_name, localleft_name, allocid);
							allocid = 0;
						} else if (right instanceof CaughtExceptionRef) {
							anderson.addAssignConstraint_intra_from_static(sm_name, "@caughtexception", localleft_name);
						} else if (right instanceof ParameterRef) {
							ParameterRef pr = (ParameterRef) right;
							anderson.addAssignConstraint_intra_from_static(sm_name,
									"@parameter" + Integer.toString(pr.getIndex()), localleft_name);
						} else if (right instanceof ThisRef) {
							anderson.addAssignConstraint_intra_from_static(sm_name, "@this", localleft_name);
						} else {
							assert false;
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
							assert base instanceof Local;
							anderson.addAssignConstraint_intra_to_field(sm_name, localright_name,
									getLocalName((Local) base), "#arrayvalue");
						} else if (left instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) left;
							Value base = ifr.getBase();
							assert base instanceof Local;
							anderson.addAssignConstraint_intra_to_field(sm_name, localright_name,
									getLocalName((Local) base), getFieldName(ifr.getField()));
						} else if (left instanceof StaticFieldRef) {
							StaticFieldRef sfr = (StaticFieldRef) left;
							anderson.addAssignConstraint_intra_to_static(sm_name, localright_name,
									getFieldName(sfr.getField()));
						} else {
							assert false;
						}
					}
				} else {
					assert false;
				}
			} else if (u instanceof ThrowStmt) {
				ThrowStmt ts = (ThrowStmt) u;
				Value v = ts.getOp();
				assert v instanceof Local;
				anderson.addAssignConstraint_intra_to_static(sm_name, "@caughtexception", getLocalName((Local) v));
			}
		}
	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		process_edges();

		QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();
		while (qr.hasNext()) {
			process_method(qr.next().method());
		}
	}
}
