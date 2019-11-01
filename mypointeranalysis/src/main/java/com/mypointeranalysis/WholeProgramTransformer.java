package com.mypointeranalysis;

import java.util.*;
import java.util.Map.Entry;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.Chain;
import soot.util.queue.QueueReader;
import sun.security.jca.GetInstance;

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
	private Map<SootMethod, Integer> call_times = new HashMap<>();
	private Anderson anderson = new Anderson();

	private void add_assign(Local left, Local right, String left_name, String right_name) {
		Local left_temp, right_temp;
		Type left_type = left.getType();
		Chain<SootField> fields = Scene.v().getSootClass(left_type.toQuotedString()).getFields();
		for(SootField field : fields){
		    left_temp = (Local) left.clone();
		    right_temp = (Local) right.clone();
			change_name(left_temp, field.toString(), left_name);
			change_name(right_temp, field.toString(), right_name);
			anderson.addAssignConstraint(right_temp, left_temp);
		}
		left_temp = (Local) left.clone();
		right_temp = (Local) right.clone();
		change_name(left_temp, left_name);
		change_name(right_temp, right_name);
		anderson.addAssignConstraint(right_temp, left_temp);
	}



	private void dfs_ongraph(SootMethod method){
		if(!isInProject(method.getDeclaringClass().getName())){
			return;
		}
		String method_name = method.toString() + " " + call_times.get(method);
		int allocId = 0;
		if (method.hasActiveBody()) {
			for (Unit u : method.getActiveBody().getUnits()) {
				if (u instanceof InvokeStmt) {
					InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
					if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
						allocId = ((IntConstant)ie.getArgs().get(0)).value;
					} else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
						Local v = (Local) ie.getArgs().get(1).clone();
						change_name(v, method_name);
						int id = ((IntConstant)ie.getArgs().get(0)).value;
						queries.put(id, v);
					} else if(callerUnits.containsKey(u)) {
						SootMethod callee = callerUnits.get(u).method();
						List<Local> local_list = callee.getActiveBody().getParameterLocals();
						List<Value> args = ie.getArgs();
						int len = args.size();
						Integer times = call_times.get(callee);
						String callee_name = callee.toString() + " " + (times + 1);
						call_times.put(callee, times+1);
						if(ie instanceof InstanceInvokeExpr) {
							add_assign(callee.getActiveBody().getThisLocal(),
									(Local) ((InstanceInvokeExpr) ie).getBase(),
									callee_name, method_name);
						}
					    for(int i = 0; i < len; i++){
					    	add_assign((Local) args.get(i), local_list.get(i), callee_name, method_name);
						}
						dfs_ongraph(callerUnits.get(u).method());
						if(ie instanceof InstanceInvokeExpr){
						    add_assign((Local) ((InstanceInvokeExpr) ie).getBase(),
									callee.getActiveBody().getThisLocal(),
									method_name, callee_name);
						}
						for(int i = 0; i < len; i++){
							add_assign(local_list.get(i), (Local) args.get(i), method_name, callee_name);
						}
					}
				}
				if (u instanceof DefinitionStmt) {
				    DefinitionStmt def_stmt = (DefinitionStmt) u;
					if (def_stmt.getRightOp() instanceof NewExpr) {
						Local lop = (Local) ((DefinitionStmt) u).getLeftOp().clone();
						change_name(lop, method_name);
						anderson.addNewConstraint(allocId, lop);
					}
					if (def_stmt.getLeftOp() instanceof Local && def_stmt.getRightOp() instanceof Local) {
						add_assign((Local) def_stmt.getLeftOp(), (Local) def_stmt.getRightOp(), method_name, method_name);
					}
					if (def_stmt.getLeftOp() instanceof JInstanceFieldRef && def_stmt.getRightOp() instanceof Local) {
                        JInstanceFieldRef fieldRef = (JInstanceFieldRef) def_stmt.getLeftOp();
                       	String field = fieldRef.getField().toString();
						Local base = (Local) fieldRef.getBase().clone();
						Local right = (Local) def_stmt.getRightOp().clone();
						change_name(base, field, method_name);
						change_name(right, method_name);
						anderson.addAssignConstraint(right, base);
					}
					if (def_stmt.getLeftOp() instanceof Local && def_stmt.getRightOp() instanceof JInstanceFieldRef) {
						JInstanceFieldRef fieldRef = (JInstanceFieldRef) def_stmt.getRightOp();
						String field = fieldRef.getField().toString();
						Local base = (Local) fieldRef.getBase().clone();
						Local left = (Local) def_stmt.getLeftOp().clone();
						change_name(base, field, method_name);
						change_name(left, method_name);
						anderson.addAssignConstraint(base, left);
					}
					if (def_stmt.getLeftOp() instanceof JInstanceFieldRef && def_stmt.getRightOp() instanceof JInstanceFieldRef) {
						JInstanceFieldRef left = (JInstanceFieldRef) def_stmt.getLeftOp();
						JInstanceFieldRef right = (JInstanceFieldRef) def_stmt.getRightOp();
						Local left_base = (Local) left.getBase().clone(), right_base = (Local) right.getBase().clone();
						change_name(left_base, left.getField().toString(), method_name);
						change_name(right_base, right.getField().toString(), method_name);
						anderson.addAssignConstraint(right_base, left_base);
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
			    call_times.put(edge.getTgt().method(), 0);
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
