package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import java.util.IdentityHashMap;

import soot.Local;

class AssignConstraint {
	Local from, to;

	AssignConstraint(Local from, Local to) {
		this.from = from;
		this.to = to;
	}
}

class NewConstraint {
	Local to;
	int allocId;

	NewConstraint(int allocId, Local to) {
		this.allocId = allocId;
		this.to = to;
	}
}

public class Anderson {
	private List<AssignConstraint> assignConstraintList = new ArrayList<AssignConstraint>();
	private List<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();
	Map<Local, TreeSet<Integer>> pts = new HashMap<Local, TreeSet<Integer>>();

	public void addAssignConstraint_intra(String method, String from, String to) {
	}

	public void addAssignConstraint_intra_from_filed(String method, String from_base, String from, String to) {
	}

	public void addAssignConstraint_intra_to_field(String method, String from, String to_base, String to) {
	}

	public void addAssignConstraint_intra_from_static(String method, String from, String to) {
	}

	public void addAssignConstraint_intra_to_static(String method, String from, String to) {
	}

	public void addAssignConstraint_inter_fromid(String method_from, int call_id_from, String from, String method_to,
			String to) {

	}

	public void addAssignConstraint_inter_toid(String method_from, String from, String method_to, int call_id_to,
			String to) {

	}

	public void addNew(String method, String localname, int allocid) {

	}

	void addNewConstraint(int alloc, Local to) {
		newConstraintList.add(new NewConstraint(alloc, to));
	}

	void run() {
		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new TreeSet<Integer>());
			}
			pts.get(nc.to).add(nc.allocId);
		}
		for (boolean flag = true; flag;) {
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
	}

	TreeSet<Integer> getPointsToSet(Local local) {
		return pts.get(local);
	}

}
