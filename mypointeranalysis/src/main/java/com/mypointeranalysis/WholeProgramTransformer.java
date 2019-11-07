package com.mypointeranalysis;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {
	Set<Integer> allallocids = new TreeSet<>();
	Set<Integer> testcases = new TreeSet<>();

	void process_method(SootMethod sm) {
		int allocid = -1;
		if (!sm.hasActiveBody())
			return;
		JimpleBody jb = (JimpleBody) sm.getActiveBody();

		for (Unit u : jb.getUnits()) {
			try {
				if (u instanceof InvokeStmt) {
					InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
					SootMethod invoke_sm = ie.getMethod();
					if (invoke_sm.getDeclaringClass().getName().equals("benchmark.internal.BenchmarkN")) {
						if (invoke_sm.getName().equals("alloc")) {
							if (ie.getArgCount() >= 1 && ie.getArg(0) instanceof IntConstant) {
								allocid = ((IntConstant) ie.getArg(0)).value;
								allallocids.add(allocid);
							}
						} else if (invoke_sm.getName().equals("test")) {
							if (ie.getArgCount() >= 1 && ie.getArg(0) instanceof IntConstant) {
								int testcase_id = ((IntConstant) ie.getArg(0)).value;
								testcases.add(testcase_id);
							}
						}
					}
				}
			} catch (Exception ex) {
			}
		}
	}

	void print_testcases() {
		StringBuilder builder = new StringBuilder();
		for (int tc : testcases) {
			Set<Integer> results = allallocids;
			builder.append(Integer.toString(tc));
			builder.append(":");
			for (int i : results) {
				if (i != -1) {
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
		QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();
		while (qr.hasNext()) {
			process_method(qr.next().method());
		}
		print_testcases();
	}
}
