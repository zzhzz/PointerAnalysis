package com.mypointeranalysis;

import java.io.File;

import soot.PackManager;
import soot.Transform;

public class MyPointerAnalysis {

	public static String entryclass;
	
	// args[0] = "/root/workspace/code"
	// args[1] = "test.Hello"	
	public static void main(String[] args) {		
		System.err.println("SJPA (Simple Jimple Pointer Analyzer)");
		entryclass = args[1];
		//String jdkLibPath = System.getProperty("java.home")+"/lib/"; // "/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/";
		String classpath = args[0] 
				+ File.pathSeparator + args[0] + File.separator + "rt.jar"
				+ File.pathSeparator + args[0] + File.separator + "jce.jar";	
		// System.out.println(classpath);
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
		soot.Main.main(new String[] {
			"-w",
			"-p", "cg.spark", "enabled:true",
			"-p", "wjtp.mypta", "enabled:true",
			"-soot-class-path", classpath,
			args[1]				
		});
	}
}
