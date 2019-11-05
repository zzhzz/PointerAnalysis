package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import java.util.IdentityHashMap;

import soot.Hierarchy;
import soot.Local;
import soot.RefLikeType;
import soot.Scene;
import soot.Type;

class AssignConstraint {
	String from, to;

	AssignConstraint(String from, String to) {
		this.from = from;
		this.to = to;
	}
}

class AssignConstraintFromField {
	String frombase, from, to;

	AssignConstraintFromField(String frombase, String from, String to) {
		this.from = from;
		this.to = to;
		this.frombase = frombase;
	}
}

class AssignConstraintToField {
	String tobase, from, to;

	AssignConstraintToField(String from, String tobase, String to) {
		this.from = from;
		this.to = to;
		this.tobase = tobase;
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

class HeapObject {
	int allocid;
	TypeInfo t;
	Map<String, Set<Integer>> fields;
	HeapObject(TypeInfo t, int allocid) {
		this.t = t;
		this.allocid = allocid;
		fields = new HashMap<String, Set<Integer>>();
		for(Map.Entry<String,TypeInfo> e: t.getFields()) {
			fields.put(e.getKey(), new HashSet<Integer>());
		}
	}
}

public class Anderson {
	private List<AssignConstraint> assignConstraintList = new ArrayList<>();
	private List<AssignConstraintFromField> assignConstraintFromFieldList = new ArrayList<>();
	private List<AssignConstraintToField> assignConstraintToFieldList = new ArrayList<>();

	Map<String, List<Integer>> funccopys = new HashMap<>();

	Map<String, List<Integer>> pointto = new HashMap<>();
	List<HeapObject> heapObjects = new ArrayList<>();
	Map<String, TypeInfo> locals = new HashMap<>();

	private static String localName(String funcname, int call_id, String local) {
		return funcname + "!!" + Integer.toString(call_id) + "!!" + local;
	}

	public void addFunctionCopy(String funcname, int call_id) {
		if(!funccopys.containsKey(funcname)) {
			funccopys.put(funcname, new ArrayList<Integer>());
		}
		funccopys.get(funcname).add(call_id);
	}

	public void addAssignConstraint_intra(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			assignConstraintList.add(new AssignConstraint(
				localName(method, cpyid, from),
				localName(method, cpyid, to)
			));
		}
	}

	public void addAssignConstraint_intra_from_filed(String method, String from_base, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			assignConstraintFromFieldList.add(new AssignConstraintFromField(
				localName(method, cpyid, from_base),
				from,
				localName(method, cpyid, to)
			));
		}
	}

	public void addAssignConstraint_intra_to_field(String method, String from, String to_base, String to) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			assignConstraintToFieldList.add(new AssignConstraintToField(
				localName(method, cpyid, from),
				localName(method, cpyid, to_base),
				to
			));
		}
	}

	public void addAssignConstraint_intra_from_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			assignConstraintList.add(new AssignConstraint(
				from,
				localName(method, cpyid, to)
			));
		}
	}

	public void addAssignConstraint_intra_to_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			assignConstraintList.add(new AssignConstraint(
				localName(method, cpyid, from),
				to
			));
		}
	}

	public void addAssignConstraint_inter_fromid(String method_from, int call_id_from, String from, String method_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_to);
		for(int cpyid: copyids) {
			assignConstraintList.add(new AssignConstraint(
				localName(method_from, call_id_from, from),
				localName(method_to, cpyid, to)
			));
		}
	}

	public void addAssignConstraint_inter_toid(String method_from, String from, String method_to, int call_id_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_from);
		for(int cpyid: copyids) {
			assignConstraintList.add(new AssignConstraint(
				localName(method_from, cpyid, from),
				localName(method_to, call_id_to, to)
			));
		}
	}

	public void addLocal(String method, String localname, TypeInfo t) {
		List<Integer> copyids = funccopys.get(method);
		for(int cpyid: copyids) {
			locals.put(localName(method, cpyid, localname), t);
		}
	}

	public void addNew(String method, String localname, int allocid, TypeInfo t) {
		
	}

}
