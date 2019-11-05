package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import soot.Local;

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
		for (Map.Entry<String, TypeInfo> e : t.getFields()) {
			fields.put(e.getKey(), new HashSet<Integer>());
		}
	}
}

class LocalPointer {
	TypeInfo t;
	Set<Integer> pointto;

	LocalPointer(TypeInfo t) {
		this.t = t;
		pointto = new TreeSet<Integer>();
	}
}

public class Anderson {

	interface AssignConstraintUpdate {
		boolean UpdatePointto();
	}

	class AssignConstraint implements AssignConstraintUpdate {
		String from, to;
		boolean enabled;

		AssignConstraint(String from, String to) {
			this.from = from;
			this.to = to;
			enabled = true;
			// System.out.println(toString());
		}

		@Override
		public boolean UpdatePointto() {
			if (!locals.containsKey(from) || !locals.containsKey(to)) {
				enabled = false;
				MyOutput.myprint("Warning: Disable Rule " + toString());
			}
			if (!enabled)
				return false;
			LocalPointer f = locals.get(from);
			LocalPointer t = locals.get(to);
			return t.pointto.addAll(f.pointto);
		}

		@Override
		public String toString() {
			return "[" + to + "=" + from + "]";
		}
	}

	class AssignConstraintFromField implements AssignConstraintUpdate {
		String frombase, from, to;
		boolean enabled;

		AssignConstraintFromField(String frombase, String from, String to) {
			this.from = from;
			this.to = to;
			this.frombase = frombase;
			enabled = true;
			// System.out.println(toString());
		}

		@Override
		public boolean UpdatePointto() {
			if (!locals.containsKey(frombase) || !locals.containsKey(to)) {
				enabled = false;
				MyOutput.myprint("Warning: Disable Rule " + toString());
			}
			if (!enabled)
				return false;
			LocalPointer f = locals.get(frombase);
			LocalPointer t = locals.get(to);
			boolean flag = false;
			for (int o : f.pointto) {
				Map<String, Set<Integer>> fields = heapObjects.get(o).fields;
				if (fields.containsKey(from)) {
					if (t.pointto.addAll(fields.get(from))) {
						flag = true;
					}
				}
			}
			return flag;
		}

		@Override
		public String toString() {
			return "[" + to + "=" + frombase + "." + from + "]";
		}
	}

	class AssignConstraintToField implements AssignConstraintUpdate {
		String tobase, from, to;
		boolean enabled;

		AssignConstraintToField(String from, String tobase, String to) {
			this.from = from;
			this.to = to;
			this.tobase = tobase;
			enabled = true;
			// System.out.println(toString());
		}

		@Override
		public boolean UpdatePointto() {
			if (!locals.containsKey(from) || !locals.containsKey(tobase)) {
				enabled = false;
				MyOutput.myprint("Warning: Disable Rule " + toString());
			}
			if (!enabled)
				return false;
			LocalPointer f = locals.get(from);
			LocalPointer t = locals.get(tobase);
			boolean flag = false;
			for (int o : t.pointto) {
				Map<String, Set<Integer>> fields = heapObjects.get(o).fields;
				if (fields.containsKey(to)) {
					if (fields.get(to).addAll(f.pointto)) {
						flag = true;
					}
				}
			}
			return flag;
		}

		@Override
		public String toString() {
			return "[" + tobase + "." + to + "=" + from + "]";
		}
	}

	private List<AssignConstraintUpdate> assignConstraintList = new ArrayList<>();

	Map<String, List<Integer>> funccopys = new HashMap<>();

	List<HeapObject> heapObjects = new ArrayList<>();
	Map<String, LocalPointer> locals = new HashMap<>();

	private static String localName(String funcname, int call_id, String local) {
		return funcname + "!!" + Integer.toString(call_id) + "!!" + local;
	}

	public void addFunctionCopy(String funcname, int call_id) {
		if (!funccopys.containsKey(funcname)) {
			// System.out.println("FuncCopy: " + funcname);
			funccopys.put(funcname, new ArrayList<Integer>());
		}
		funccopys.get(funcname).add(call_id);
	}

	public void addAssignConstraint_intra(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList
					.add(new AssignConstraint(localName(method, cpyid, from), localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_from_filed(String method, String from_base, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(new AssignConstraintFromField(localName(method, cpyid, from_base), from,
					localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_field(String method, String from, String to_base, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(
					new AssignConstraintToField(localName(method, cpyid, from), localName(method, cpyid, to_base), to));
		}
	}

	public void addAssignConstraint_intra_from_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(new AssignConstraint(from, localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(new AssignConstraint(localName(method, cpyid, from), to));
		}
	}

	public void addAssignConstraint_inter_fromid(String method_from, int call_id_from, String from, String method_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_to);
		for (int cpyid : copyids) {
			assignConstraintList.add(
					new AssignConstraint(localName(method_from, call_id_from, from), localName(method_to, cpyid, to)));
		}
	}

	public void addAssignConstraint_inter_toid(String method_from, String from, String method_to, int call_id_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_from);
		for (int cpyid : copyids) {
			assignConstraintList.add(
					new AssignConstraint(localName(method_from, cpyid, from), localName(method_to, call_id_to, to)));
		}
	}

	public void addLocal(String method, String localname, TypeInfo t) {
		List<Integer> copyids = funccopys.get(method);
		// System.out.println("AddLocal: " + method);
		for (int cpyid : copyids) {
			locals.put(localName(method, cpyid, localname), new LocalPointer(t));
			// System.out.println("AddLocal: " + localName(method, cpyid, localname));
		}
		// int numlocals = locals.size();
		// if (numlocals % 1000 == 0)
			// System.out.println("NUMLOCAL: " + Integer.toString(numlocals));
	}

	public void addNew(String method, String localname, int allocid, TypeInfo t) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			int newheapid = heapObjects.size();
			heapObjects.add(new HeapObject(t, allocid));
			locals.get(localName(method, cpyid, localname)).pointto.add(newheapid);
		}
		// int numlocals = heapObjects.size();
		// if (numlocals % 1000 == 0)
			// System.out.println("NUMHEAP: " + Integer.toString(numlocals));
	}

	public void addStatic(String name, TypeInfo t) {
		locals.put(name, new LocalPointer(t));
	}

	public void addNewMultiArray(String method, String localname, int allocid, TypeInfo t, int depth) {

	}

	public void run() {
		// System.out.println("RUN");
		for (boolean flag = true; flag;) {
			flag = false;
			for (AssignConstraintUpdate acu : assignConstraintList) {
				if (acu.UpdatePointto()) {
					flag = true;
				}
			}
		}
	}

	public Set<Integer> getAllocIds(String method, String localname) {
		Set<Integer> results = new TreeSet<>();
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			// System.out.println(localName(method, cpyid, localname));
			Set<Integer> heapids = locals.get(localName(method, cpyid, localname)).pointto;
			for (int heapid : heapids) {
				results.add(heapObjects.get(heapid).allocid);
			}
		}
		return results;
	}

	public void printall() {
		for(Map.Entry<String, LocalPointer> p: locals.entrySet()) {
			// System.out.println("Result: " + p.getKey() + ":" + p.getValue().pointto.toString());
		}
	}
}
