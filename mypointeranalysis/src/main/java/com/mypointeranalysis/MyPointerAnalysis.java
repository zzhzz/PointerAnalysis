package com.mypointeranalysis;

import java.io.File;

import soot.PackManager;
import soot.Transform;
import vasco.callgraph.CallGraphTransformer;

public class MyPointerAnalysis {

	public static String entryclass;
	
	public static void main(String[] args) {		
		entryclass = args[1];
		String classpath = args[0] 
				+ File.pathSeparator + args[0] + File.separator + "rt.jar"
				+ File.pathSeparator + args[0] + File.separator + "jce.jar";	
				String[] sootArgs = {
					"-cp", classpath, // "-pp", 
					"-w", // "-app", 
					//"-keep-line-number",
					// "-keep-bytecode-offset",
					//"-p", "cg", "implicit-entry:false",
					"-p", "cg.spark", "enabled",
					// "-p", "cg.spark", "simulate-natives",
					// "-p", "cg", "safe-forname",
					// "-p", "cg", "safe-newinstance",
					// "-main-class", entryclass,
					// "-f", "none", 
					entryclass 
			};
			CallGraphTransformer cgt = new CallGraphTransformer();
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.fcpa", cgt));
			soot.Main.main(sootArgs);
	}
}
