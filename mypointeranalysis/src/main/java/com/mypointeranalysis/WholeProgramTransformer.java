package com.mypointeranalysis;

import java.util.*;
import java.util.Map.Entry;

import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;
import sun.reflect.generics.tree.Tree;

public class WholeProgramTransformer extends SceneTransformer {

	private boolean isInProject(String name) {
		return !name.startsWith("java") && !name.startsWith("org")
				&& !name.startsWith("jdk") && !name.startsWith("com") && !name.startsWith("sun");
	}

	private void change_name(Local v, String method_name){
		String v_name = v.getName();
		v.setName(method_name + " " + v_name);
	}

	private void change_name(Local v, String field_name, String method_name) {
		String v_name = v.getName();
		v.setName(method_name + " " + v_name + " " + field_name);
	}

	private Map<Unit, MethodOrMethodContext> callerUnits = new HashMap<>();
	private TreeMap<Integer, Local> queries = new TreeMap<Integer, Local>();
	private Anderson anderson = new Anderson();

	private void dfs_ongraph(SootMethod method){
		if(!isInProject(method.getDeclaringClass().getName())){
			return;
		}
		String method_name = method.getDeclaringClass().getName() + " " + method.getName();
		int allocId = 0;
		if (method.hasActiveBody()) {
			for (Unit u : method.getActiveBody().getUnits()) {
				if (u instanceof InvokeStmt) {
					InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
					if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
						allocId = ((IntConstant)ie.getArgs().get(0)).value;
					} else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
						Local v = (Local) ie.getArgs().get(1);
						change_name(v, method_name);
						int id = ((IntConstant)ie.getArgs().get(0)).value;
						queries.put(id, v);
					} else if(callerUnits.containsKey(u)){
						dfs_ongraph(callerUnits.get(u).method());
					}
				}
				if (u instanceof DefinitionStmt) {
				    DefinitionStmt def_stmt = (DefinitionStmt) u;
				    if (def_stmt.getRightOp() instanceof InvokeExpr) {
						System.out.println(u);
					}
					if (def_stmt.getRightOp() instanceof NewExpr) {
						Local lop = (Local) ((DefinitionStmt) u).getLeftOp();
						change_name(lop, method_name);
						anderson.addNewConstraint(allocId, lop);
					}
					if (def_stmt.getLeftOp() instanceof Local && def_stmt.getRightOp() instanceof Local) {
						Local left = (Local) def_stmt.getLeftOp();
						Local right = (Local) def_stmt.getRightOp();
						change_name(left, method_name);
						change_name(right, method_name);
						anderson.addAssignConstraint(right, left);
					}
					if (def_stmt.getLeftOp() instanceof JInstanceFieldRef && def_stmt.getRightOp() instanceof Local) {
                        JInstanceFieldRef fieldRef = (JInstanceFieldRef) def_stmt.getLeftOp();
                       	String field = fieldRef.getField().toString();
						Local base = (Local) fieldRef.getBase();
						Local right = (Local) def_stmt.getRightOp();
						change_name(base, field, method_name);
						change_name(right, method_name);
						anderson.addAssignConstraint(base, right);
					}
					if (def_stmt.getLeftOp() instanceof Local && def_stmt.getRightOp() instanceof JInstanceFieldRef) {
						JInstanceFieldRef fieldRef = (JInstanceFieldRef) def_stmt.getRightOp();
						String field = fieldRef.getField().toString();
						Local base = (Local) fieldRef.getBase();
						Local left = (Local) def_stmt.getLeftOp();
						change_name(base, field, method_name);
						change_name(left, method_name);
						anderson.addAssignConstraint(left, base);
					}
					if (def_stmt.getLeftOp() instanceof JInstanceFieldRef && def_stmt.getRightOp() instanceof JInstanceFieldRef) {
						JInstanceFieldRef left = (JInstanceFieldRef) def_stmt.getLeftOp();
						JInstanceFieldRef right = (JInstanceFieldRef) def_stmt.getRightOp();
						Local left_base = (Local) left.getBase(), right_base = (Local) right.getBase();
						change_name(left_base, left.getField().toString(), method_name);
						change_name(right_base, right.getField().toString(), method_name);
						anderson.addAssignConstraint(left_base, right_base);
					}
				}
			}
		}

	}

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		CallGraph callGraph = Scene.v().getCallGraph();
		QueueReader<Edge> qedges = callGraph.listener();
		while(qedges.hasNext()){
			Edge edge = qedges.next();
			String src_name = edge.getSrc().method().getDeclaringClass().getName(),
					dst_name = edge.getTgt().method().getDeclaringClass().getName();
			if(isInProject(src_name) && isInProject(dst_name)) {
			    callerUnits.put(edge.srcUnit(), edge.getTgt().method());
			}
		}
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			if(!isInProject(sm.getDeclaringClass().getName())){
				continue;
			}
			if(callGraph.isEntryMethod(sm)){
			    dfs_ongraph(sm);
			    System.exit(0);
			}
		}
		anderson.run();
		String answer = "";
		for (Entry<Integer, Local> q : queries.entrySet()) {
			TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			for (Integer i : result) {
				answer += " " + i;
			}
			answer += "\n";
		}
		AnswerPrinter.printAnswer(answer);

	}

}
