package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import soot.Local;

class AssignConstraint {
	String from, to;
	int local_to_filed;
	AssignConstraint(Local from, Local to, int local_to_filed) {
		this.from = from.toString();
		this.to = to.toString();
		this.local_to_filed = local_to_filed;
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
	Map<Integer, TreeSet<String>> pts_inverse = new HashMap<>();
	void addAssignConstraint(Local from, Local to, int local_to_field) {
		// System.out.println(to + " = " + from);
		assignConstraintList.add(new AssignConstraint(from, to, local_to_field));
	}
	void addNewConstraint(int alloc, Local to) {
		// System.out.println(to + " = " + alloc);
		newConstraintList.add(new NewConstraint(alloc, to));		
	}
	void run() {
		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new TreeSet<Integer>());
			}
			if (!pts_inverse.containsKey(nc.allocId)) {
				pts_inverse.put(nc.allocId, new TreeSet<String>());
			}
			pts.get(nc.to).add(nc.allocId);
			pts_inverse.get(nc.allocId).add(nc.to);
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
					TreeSet<Integer> allocs = pts.get(ac.from);
					for(Integer addr: allocs){
						pts_inverse.get(addr).add(ac.to);
					}
					flag = true;
				}
				if(ac.local_to_filed == 1){
					int split_pos = ac.to.indexOf("|||");
				    String base_name = ac.to.substring(0, split_pos);
					String field_name = ac.to.substring(split_pos + 4);
					TreeSet<Integer> allocs = pts.get(base_name);
					for(Integer addr: allocs){
						TreeSet<String> locals = pts_inverse.get(addr);
						for(String names: locals){
							String nname = names + "||| " + field_name;
							System.out.println(nname);
							System.out.println(ac.to);
							if(!pts.containsKey(nname)){
								pts.put(nname, new TreeSet<Integer>());
							}
							if(pts.get(nname).addAll(pts.get(ac.from))){
								TreeSet<Integer> alloc = pts.get(ac.from);
								for(Integer ad: alloc){
									pts_inverse.get(ad).add(ac.to);
								}
								flag = true;
							}
						}
					}
				}

			}
		}
		/*
		for(String var : pts.keySet()){
			System.out.println(var + " " + pts.get(var));
		}
		 */
	}
	TreeSet<Integer> getPointsToSet(Local local) {
		return pts.get(local.toString());
	}
}
