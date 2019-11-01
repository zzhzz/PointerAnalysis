package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import soot.Local;

class AssignConstraint {
	String from, to;
	AssignConstraint(Local from, Local to) {
		this.from = from.toString();
		this.to = to.toString();
	}
}

class NewConstraint {
	String to;
	int allocId;
	NewConstraint(int allocId, Local to) {
		this.allocId = allocId;
		this.to = to.toString();
	}
}

public class Anderson {
	private List<AssignConstraint> assignConstraintList = new ArrayList<AssignConstraint>();
	private List<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();
	Map<String, TreeSet<Integer>> pts = new HashMap<String, TreeSet<Integer>>();
	void addAssignConstraint(Local from, Local to) {
		System.out.println(to + " = " + from);
		assignConstraintList.add(new AssignConstraint(from, to));
	}
	void addNewConstraint(int alloc, Local to) {
		System.out.println(to + " = " + alloc);
		newConstraintList.add(new NewConstraint(alloc, to));		
	}
	void run() {
		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new TreeSet<Integer>());
			}
			pts.get(nc.to).add(nc.allocId);
		}
		for (boolean flag = true; flag; ) {
			flag = false;
			for (AssignConstraint ac : assignConstraintList) {
				if (!pts.containsKey(ac.from)) {
					continue;
				}	
				if (!pts.containsKey(ac.to)) {
					pts.put(ac.to, new TreeSet<Integer>());
				}
				if (pts.get(ac.to).addAll(pts.get(ac.from))) {
					flag = true;
				}
			}
		}
		for(String var : pts.keySet()){
			System.out.println(var + " " + pts.get(var));
		}
	}
	TreeSet<Integer> getPointsToSet(Local local) {
		return pts.get(local.toString());
	}
}
