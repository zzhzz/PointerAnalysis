package com.mypointeranalysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

class Pointer {
	private TypeInfo t;
	private Set<Integer> pointto;
	private Anderson anderson;

	Pointer(Anderson anderson, TypeInfo t) {
		this.anderson = anderson;
		this.t = t;
		this.pointto = new TreeSet<>();
	}

	public boolean addAll(Pointer pt) {
		boolean flag = false;
		for (int i : pt.pointto) {
			if (addOne(i)) {
				flag = true;
			}
		}
		return flag;
	}

	public boolean addOne(int i) {
		if (i >= anderson.heapObjects.size()) {
			MyOutput.myassert(false);
			return false;
		}
		TypeInfo tf = anderson.heapObjects.get(i).t;
		if (!TypeInfo.canContain(tf, t)) {
			return false;
		} else {
			return pointto.add(i);
		}
	}

	public Set<Integer> getAll() {
		return pointto;
	}
}

class HeapObject {
	int allocid;
	TypeInfo t;
	Anderson anderson;
	Map<String, Pointer> fields;

	HeapObject(Anderson anderson, TypeInfo t, int allocid) {
		this.anderson = anderson;
		this.t = t;
		this.allocid = allocid;
		fields = new HashMap<String, Pointer>();
		for (Map.Entry<String, TypeInfo> e : t.getFields()) {
			fields.put(e.getKey(), new Pointer(anderson, e.getValue()));
		}
	}

	Pointer getField(String name) {
		return fields.get(name);
	}

	boolean hasField(String name) {
		return fields.containsKey(name);
	}

	boolean addToField(String name, Pointer inputs) {
		if (fields.containsKey(name)) {
			return fields.get(name).addAll(inputs);
		} else {
			MyOutput.myassert(false);
			return false;
		}
	}

	public int getAllocid() {
		return allocid;
	}

	public Map<String, Pointer> getFields() {
		return fields;
	}
}

class UnknownHeapObject extends HeapObject {
	UnknownHeapObject(Anderson anderson, int allocid) {
		super(anderson, TypeInfo.getUnknownType(), allocid);
	}

	@Override
	boolean hasField(String name) {
		return true;
	}

	@Override
	Pointer getField(String name) {
		return super.getField(Anderson.UNKNOWN_SIGN);
	}

	@Override
	boolean addToField(String name, Pointer inputs) {
		return super.addToField(Anderson.UNKNOWN_SIGN, inputs);
	}
}

// class LocalPointer {
// TypeInfo t;
// Pointer pointto;
// Anderson anderson;

// LocalPointer(Anderson anderson, TypeInfo t) {
// this.t = t;
// this.anderson = anderson;
// pointto = new Pointer(anderson, t);
// }
// }

class AssignConstraint {
	Anderson anderson;
	boolean from_field, to_field, enabled;
	String from, frombase, to, tobase;

	private AssignConstraint() {
	}

	public static AssignConstraint NewL2L(Anderson anderson, String from, String to) {
		AssignConstraint result = new AssignConstraint();
		result.anderson = anderson;
		result.from_field = false;
		result.to_field = false;
		result.enabled = true;
		result.from = null;
		result.frombase = from;
		result.to = null;
		result.tobase = to;
		return result;
	}

	public static AssignConstraint NewF2L(Anderson anderson, String frombase, String from, String to) {
		AssignConstraint result = new AssignConstraint();
		result.anderson = anderson;
		result.from_field = true;
		result.to_field = false;
		result.enabled = true;
		result.from = from;
		result.frombase = frombase;
		result.to = null;
		result.tobase = to;
		return result;
	}

	public static AssignConstraint NewL2F(Anderson anderson, String from, String tobase, String to) {
		AssignConstraint result = new AssignConstraint();
		result.anderson = anderson;
		result.from_field = false;
		result.to_field = true;
		result.enabled = true;
		result.from = null;
		result.frombase = from;
		result.to = to;
		result.tobase = tobase;
		return result;
	}

	public void check() {
		// System.out.println(toString());

		boolean from_unknown, to_unknown;
		from_unknown = !anderson.locals.containsKey(frombase);
		to_unknown = !anderson.locals.containsKey(tobase);
		if (from_unknown && to_unknown) {
			System.out.println("FROM_TO_UNKNOWN: " + toString());
			// WholeProgramTransformer.shouldprintall = true;
			enabled = false;
		} else if (from_unknown && !to_unknown) {
			System.out.println("FROM_UNKNOWN: " + toString());
			// WholeProgramTransformer.shouldprintall = true;
			from_field = false;
			from = null;
			frombase = Anderson.UNKNOWN_LOCAL;
		} else if (!from_unknown && to_unknown) {
			System.out.println("TO_UNKNOWN: " + toString());
			// WholeProgramTransformer.shouldprintall = true;
			to_field = false;
			to = null;
			tobase = Anderson.UNKNOWN_LOCAL;
		} else if (!from_unknown && !to_unknown) {
		}

		// System.out.println(toString());
	}

	public boolean UpdatePointto() {
		if (!enabled)
			return false;

		if (!from_field && !to_field) {
			return UpdatePointto_l2l();
		} else if (from_field && !to_field) {
			return UpdatePointto_f2l();
		} else if (!from_field && to_field) {
			return UpdatePointto_l2f();
		} else {
			MyOutput.myassert(false);
			return false;
		}
	}

	boolean UpdatePointto_l2l() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		if (f == null || t == null) {
			MyOutput.myassert(false);
			return false;
		}
		return t.addAll(f);
	}

	boolean UpdatePointto_f2l() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		if (f == null || t == null) {
			MyOutput.myassert(false);
			return false;
		}

		boolean flag = false;
		Set<Integer> tmpset = new TreeSet<>(); // copy set object, (x=x.t), set may be changed during iteration.
		tmpset.addAll(f.getAll());
		for (int o : tmpset) {
			HeapObject ho = anderson.heapObjects.get(o);
			if (ho.hasField(from)) {
				if (t.addAll(ho.getField(from)))
					flag = true;
			} else {
				MyOutput.myassert(false);
			}
		}

		return flag;
	}

	boolean UpdatePointto_l2f() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		if (f == null || t == null) {
			MyOutput.myassert(false);
			return false;
		}
		boolean flag = false;
		for (int o : t.getAll()) {
			if (o >= anderson.heapObjects.size()) {
				MyOutput.myassert(false);
				continue;
			}
			HeapObject ho = anderson.heapObjects.get(o);
			if (ho.hasField(to)) {
				if (ho.addToField(to, f)) {
					flag = true;
				}
			} else {
				MyOutput.myassert(false);
			}
		}
		return flag;
	}

	@Override
	public String toString() {
		if (!enabled)
			return "[Disabled]";

		if (!from_field && !to_field) {
			return "[" + tobase + " = " + frombase + "]";
		} else if (from_field && !to_field) {
			return "[" + tobase + " = " + frombase + "." + from + "]";
		} else if (!from_field && to_field) {
			return "[" + tobase + "." + to + "=" + frombase + "]";
		} else {
			MyOutput.myassert(false);
			return "[Error]";
		}
	}
}

public class Anderson {
	public static final String UNKNOWN_LOCAL = "@unknown";
	public static final String UNKNOWN_TYPE = "java.lang.Object";
	public static final String EXCEPTION_LOCAL = "@caughtexception";
	public static final String EXCEPTION_TYPE = "java.lang.Exception";
	public static final String RET_LOCAL = "@ret";
	public static final String THIS_LOCAL = "@this";
	public static final String ARRAY_FIELD = "#arrayvalue";
	public static final String PARAMETER_LOCAL_PREFIX = "@parameter";
	public static final String UNKNOWN_CLASS = "unknown__type__class_282395";
	public static final String UNKNOWN_FIELD = "unknownobj";
	public static final String UNKNOWN_SIGN = "<unknown__type__class_282395: java.lang.Object unknownobj>";

	public static String parameterLocalName(int i) {
		return Anderson.PARAMETER_LOCAL_PREFIX + Integer.toString(i);
	}

	private List<AssignConstraint> assignConstraintList;
	private Map<String, List<Integer>> funccopys;

	List<HeapObject> heapObjects;
	Map<String, Pointer> locals;

	int uho_index;

	public Anderson() {
		assignConstraintList = new ArrayList<>();
		funccopys = new HashMap<>();
		heapObjects = new ArrayList<>();
		locals = new HashMap<>();

		addStatic(UNKNOWN_LOCAL, TypeInfo.getClassTypeByName(UNKNOWN_TYPE));
		addStatic(EXCEPTION_LOCAL, TypeInfo.getClassTypeByName(EXCEPTION_TYPE));
		UnknownHeapObject uho = new UnknownHeapObject(this, -1);
		System.out.println(uho.fields.keySet());
		uho_index = heapObjects.size();
		heapObjects.add(uho);
		locals.get(UNKNOWN_LOCAL).addOne(uho_index);
	}

	private boolean updateUnknown() {
		boolean flag = false;
		Pointer l = locals.get(UNKNOWN_LOCAL);
		Pointer h = heapObjects.get(uho_index).getField(UNKNOWN_FIELD);
		Queue<Integer> q = new ArrayDeque<>();
		q.addAll(l.getAll());
		q.addAll(h.getAll());
		Set<Integer> allunknown = new TreeSet<>();
		while (!q.isEmpty()) {
			int nexti = q.remove();
			if (!allunknown.add(nexti))
				continue;
			for (Map.Entry<String, Pointer> newpt : heapObjects.get(nexti).getFields().entrySet()) {
				q.addAll(newpt.getValue().getAll());
			}
		}
		for (int toadd : allunknown) {
			if (l.addOne(toadd)) {
				flag = true;
			}
		}
		for (int toadd : allunknown) {
			if (h.addOne(toadd)) {
				flag = true;
			}
		}

		return flag;
	}

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
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList
					.add(AssignConstraint.NewL2L(this, localName(method, cpyid, from), localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_from_filed(String method, String from_base, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewF2L(this, localName(method, cpyid, from_base), from,
					localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_field(String method, String from, String to_base, String to) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2F(this, localName(method, cpyid, from),
					localName(method, cpyid, to_base), to));
		}
	}

	public void addAssignConstraint_intra_from_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, from, localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, localName(method, cpyid, from), to));
		}
	}

	public void addAssignConstraint_inter_fromid(String method_from, int call_id_from, String from, String method_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_to);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, localName(method_from, call_id_from, from),
					localName(method_to, cpyid, to)));
		}
	}

	public void addAssignConstraint_inter_toid(String method_from, String from, String method_to, int call_id_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_from);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, localName(method_from, cpyid, from),
					localName(method_to, call_id_to, to)));
		}
	}

	public boolean hasFunction(String method) {
		return funccopys.containsKey(method);
	}

	public void addLocal(String method, String localname, TypeInfo t) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		// System.out.println("AddLocal: " + method);
		for (int cpyid : copyids) {
			locals.put(localName(method, cpyid, localname), new Pointer(this, t));
			// System.out.println("AddLocal: " + localName(method, cpyid, localname));
		}
		// int numlocals = locals.size();
		// if (numlocals % 1000 == 0)
		// System.out.println("NUMLOCAL: " + Integer.toString(numlocals));
	}

	public void addNew(String method, String localname, int allocid, TypeInfo t) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			int newheapid = heapObjects.size();
			heapObjects.add(new HeapObject(this, t, allocid));
			locals.get(localName(method, cpyid, localname)).addOne(newheapid);
		}
		// int numlocals = heapObjects.size();
		// if (numlocals % 1000 == 0)
		// System.out.println("NUMHEAP: " + Integer.toString(numlocals));
	}

	public void addStatic(String name, TypeInfo t) {
		locals.put(name, new Pointer(this, t));
	}

	public void addNewMultiArray(String method, String localname, int allocid, TypeInfo t, int depth) {
		List<Integer> copyids = funccopys.get(method);
		if(copyids == null) {
			MyOutput.myassert(false);
			return;
		}
		for (int cpyid : copyids) {
			int lastheapid = -1;
			int newheapid = 0;

			for (int i = 0; i < depth; ++i) {
				newheapid = heapObjects.size();
				heapObjects.add(new HeapObject(this, t, allocid));
				if (lastheapid != -1)
					heapObjects.get(newheapid).getField(ARRAY_FIELD).addOne(lastheapid);
				lastheapid = newheapid;
				t = TypeInfo.getArrayTypeInfo(t);
			}

			locals.get(localName(method, cpyid, localname)).addOne(lastheapid);
		}
	}

	public void run() {
		// System.out.println("RUN");
		for (AssignConstraint acu : assignConstraintList) {
			acu.check();
		}

		for (boolean flag = true; flag;) {
			flag = false;
			for (AssignConstraint acu : assignConstraintList) {
				if (acu.UpdatePointto()) {
					flag = true;
				}
			}
			if (updateUnknown())
				flag = true;
		}
	}

	public Set<Integer> getAllocIds(String method, String localname) {
		Set<Integer> results = new TreeSet<>();

		// Set<Integer> heaps = new TreeSet<Integer>();
		// Queue<Integer> searchq = new ArrayDeque<>();
		for (int cpyid : funccopys.get(method)) {
			Pointer heapids = locals.get(localName(method, cpyid, localname));
			for (int heapid : heapids.getAll()) {
				results.add(heapObjects.get(heapid).allocid);
			}
			// searchq.addAll(heapids.getAll());
		}

		// while(!searchq.isEmpty()) {
		// int nexti = searchq.remove();
		// if(!heaps.add(nexti))
		// continue;
		// for(Map.Entry<String, Pointer> newpt:
		// heapObjects.get(nexti).getFields().entrySet()) {
		// searchq.addAll(newpt.getValue().getAll());
		// }
		// }

		// for(int heapi: heaps) {
		// results.add(heapObjects.get(heapi).allocid);
		// }

		return results;
	}

	public void printall() {
		for (Map.Entry<String, Pointer> p : locals.entrySet()) {
			System.out.println("Result: " + p.getKey() + ":" + p.getValue().getAll().toString());
		}
	}
}
