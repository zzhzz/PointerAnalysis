package com.mypointeranalysis;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.Stmt;


public class WholeProgramTransformer extends SceneTransformer {
	
	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		CallGraph cg = Scene.v().getCallGraph();
		QueueReader<Edge> edges = cg.listener();
		while(edges.hasNext())
		{
			Edge e = edges.next();
			e.kind();
			Stmt stmt = e.srcStmt();
			if(stmt == null)
			{
				System.out.println(e);
			}
		}

		// Iterator<MethodOrMethodContext> srcs = cg.sourceMethods();
		// while(srcs.hasNext())
		// {
		// 	SootMethod m = (SootMethod)srcs.next();
		// 	System.out.println(m);
		// }
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();		
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			if(cg.isEntryMethod(sm))
			{
				System.out.println(sm);
			}
		}

		// TreeMap<Integer, Local> queries = new TreeMap<Integer, Local>();
		// Anderson anderson = new Anderson(); 
		
		// ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		// QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();		
		// while (qr.hasNext()) {
		// 	SootMethod sm = qr.next().method();
		// 	//if (sm.toString().contains("Hello")) {
		// 		//System.out.println(sm);
		// 		int allocId = 0;
		// 		if (sm.hasActiveBody()) {
		// 			for (Unit u : sm.getActiveBody().getUnits()) {
		// 				//System.out.println("S: " + u);
		// 				//System.out.println(u.getClass());
		// 				if (u instanceof InvokeStmt) {
		// 					InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
		// 					if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
		// 						allocId = ((IntConstant)ie.getArgs().get(0)).value;
		// 					}
		// 					if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
		// 						Value v = ie.getArgs().get(1);
		// 						int id = ((IntConstant)ie.getArgs().get(0)).value;
		// 						queries.put(id, (Local)v);
		// 					}
		// 				}
		// 				if (u instanceof DefinitionStmt) {
		// 					if (((DefinitionStmt)u).getRightOp() instanceof NewExpr) {
		// 						//System.out.println("Alloc " + allocId);
		// 						anderson.addNewConstraint(allocId, (Local)((DefinitionStmt) u).getLeftOp());
		// 					}
		// 					if (((DefinitionStmt)u).getLeftOp() instanceof Local && ((DefinitionStmt)u).getRightOp() instanceof Local) {
		// 						anderson.addAssignConstraint((Local)((DefinitionStmt) u).getRightOp(), (Local)((DefinitionStmt) u).getLeftOp());
		// 					}
		// 				}
		// 			}
		// 		}
		// 	//}
		// }
		
		// anderson.run();
		// String answer = "";
		// for (Entry<Integer, Local> q : queries.entrySet()) {
		// 	TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
		// 	answer += q.getKey().toString() + ":";
		// 	for (Integer i : result) {
		// 		answer += " " + i;
		// 	}
		// 	answer += "\n";
		// }
		// AnswerPrinter.printAnswer(answer);
		
	}

}
