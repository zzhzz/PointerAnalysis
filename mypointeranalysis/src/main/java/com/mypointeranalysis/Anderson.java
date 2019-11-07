package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import soot.RefLikeType;
import soot.Scene;
import soot.SootField;

class HeapObjectInfo {
	private int allocid, id;
	private TypeInfo t;

	HeapObjectInfo(TypeInfo t, int allocid, int id) {
		this.t = t;
		this.allocid = allocid;
		this.id = id;
	}

	public int getAllocid() {
		return allocid;
	}

	public TypeInfo getT() {
		return t;
	}

	public int getId() {
		return id;
	}
}

class LocalInfo {
	private String name;
	private TypeInfo t;
	private int id;

	LocalInfo(String name, TypeInfo t, int id) {
		this.name = name;
		this.t = t;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public TypeInfo getT() {
		return t;
	}

	public int getId() {
		return id;
	}
}

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
			enabled = false;
		} else if (from_unknown && !to_unknown) {
			from_field = false;
			from = null;
			frombase = Anderson.UNKNOWN_LOCAL;
		} else if (!from_unknown && to_unknown) {
			to_field = false;
			to = null;
			tobase = Anderson.UNKNOWN_LOCAL;
		} else if (!from_unknown && !to_unknown) {
		}

		// System.out.println(toString());
	}

	public void UpdatePointto() {
		if (!enabled)
			return;

		if (!from_field && !to_field) {
			UpdatePointto_l2l();
		} else if (from_field && !to_field) {
			UpdatePointto_f2l();
		} else if (!from_field && to_field) {
			UpdatePointto_l2f();
		} else {
			MyOutput.myassert(false);
			return;
		}
	}

	void UpdatePointto_l2l() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		t.clear();
		t.addAll(f);
	}

	void UpdatePointto_f2l() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		t.clear();
		for (int o : f.getAll()) {
			HeapObjectInfo ho = anderson.heapObjects.get(o);
			if (ho.hasField(from)) {
				t.addAll(ho.getField(from));
			} else {
				MyOutput.myassert(false);
			}
		}
	}

	void UpdatePointto_l2f() {
		Pointer f = anderson.locals.get(frombase);
		Pointer t = anderson.locals.get(tobase);
		int allsize = t.getAll().size();
		for (int o : t.getAll()) {
			HeapObjectInfo ho = anderson.heapObjects.get(o);
			if (ho.hasField(to)) {
				if (allsize == 1) {
					ho.clearField(to);
				}
				ho.addToField(to, f);
			} else {
				MyOutput.myassert(false);
			}
		}
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

class ProgramStatus {
	List<Map<FieldInfo, Set<Integer>>> heapObjectValues;
	List<Set<Integer>> localValues;

	private ProgramStatus() {
	}

	public static ProgramStatus createFromInfo(VariablesInfo vi) {
		ProgramStatus result = new ProgramStatus();
		result.heapObjectValues = new ArrayList<>();
		for (HeapObjectInfo hoi : vi.heapObjectInfo) {
			Map<FieldInfo, Set<Integer>> ho = new HashMap<>();
			for (FieldInfo fi : hoi.getT().getFields()) {
				ho.put(fi, new TreeSet<Integer>());
			}
			result.heapObjectValues.add(ho);
		}
		result.localValues = new ArrayList<>();
		for (LocalInfo li : vi.localInfo) {
			result.localValues.add(new TreeSet<Integer>());
		}
		return result;
	}

	public static ProgramStatus cloneStatus(ProgramStatus ps) {
		ProgramStatus result = new ProgramStatus();
		result.heapObjectValues = new ArrayList<>();
		for (Map<FieldInfo, Set<Integer>> hoi : ps.heapObjectValues) {
			Map<FieldInfo, Set<Integer>> ho = new HashMap<>();
			for (Map.Entry<FieldInfo, Set<Integer>> fi : hoi.entrySet()) {
				Set<Integer> n = new TreeSet<>();
				n.addAll(fi.getValue());
				ho.put(fi.getKey(), n);
			}
			result.heapObjectValues.add(ho);
		}
		result.localValues = new ArrayList<>();
		for (Set<Integer> li : ps.localValues) {
			Set<Integer> l = new TreeSet<>();
			l.addAll(li);
			result.localValues.add(l);
		}
		return result;
	}
}

class VariablesInfo {
	public List<HeapObjectInfo> heapObjectInfo;
	public List<LocalInfo> localInfo;
	public Map<String, LocalInfo> localInfo_inv;

	public LocalInfo emplaceLocal(String name, TypeInfo t) {
		if(localInfo_inv.containsKey(name)) {
			MyOutput.myassert(false);
			return null;
		}
		int newid = localInfo.size();
		LocalInfo l = new LocalInfo(name, t, newid);
		localInfo.add(l);
		localInfo_inv.put(name, l);
		return l;
	}

	public HeapObjectInfo emplaceHeapObject(int allocid, TypeInfo t) {
		HeapObjectInfo hoi = ;
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

	public static final int EMPYT_ALLOCID = -1;

	public static String parameterLocalName(int i) {
		return Anderson.PARAMETER_LOCAL_PREFIX + Integer.toString(i);
	}

	private List<AssignConstraint> assignConstraintList;
	private Map<String, List<Integer>> funccopys;

	VariablesInfo vi;

	public Anderson() {
		funccopys = new HashMap<String, List<Integer>>();

		vi = new VariablesInfo();
		vi.heapObjectInfo = new ArrayList<HeapObjectInfo>();
		vi.localInfo = new ArrayList<LocalInfo>();
		vi.localInfo_inv = new HashMap<String, LocalInfo>();

		addStatic(UNKNOWN_LOCAL, TypeInfo.getClassTypeByName(UNKNOWN_TYPE));
		addStatic(EXCEPTION_LOCAL, TypeInfo.getClassTypeByName(EXCEPTION_TYPE));
		HeapObjectInfo uhoi = new HeapObjectInfo(TypeInfo.getUnknownType(), EMPYT_ALLOCID);
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
		if (l.addAll(h))
			flag = true;
		if (h.addAll(l))
			flag = true;
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
		for (int cpyid : copyids) {
			assignConstraintList
					.add(AssignConstraint.NewL2L(this, localName(method, cpyid, from), localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_from_filed(String method, String from_base, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewF2L(this, localName(method, cpyid, from_base), from,
					localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_field(String method, String from, String to_base, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2F(this, localName(method, cpyid, from),
					localName(method, cpyid, to_base), to));
		}
	}

	public void addAssignConstraint_intra_from_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, from, localName(method, cpyid, to)));
		}
	}

	public void addAssignConstraint_intra_to_static(String method, String from, String to) {
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, localName(method, cpyid, from), to));
		}
	}

	public void addAssignConstraint_inter_fromid(String method_from, int call_id_from, String from, String method_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_to);
		for (int cpyid : copyids) {
			assignConstraintList.add(AssignConstraint.NewL2L(this, localName(method_from, call_id_from, from),
					localName(method_to, cpyid, to)));
		}
	}

	public void addAssignConstraint_inter_toid(String method_from, String from, String method_to, int call_id_to,
			String to) {
		List<Integer> copyids = funccopys.get(method_from);
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
		for (int cpyid : copyids) {
			int newheapid = heapObjects.size();
			heapObjects.add(new HeapObjectInfo(this, t, allocid));
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
		for (int cpyid : copyids) {
			int lastheapid = -1;
			int newheapid = 0;

			for (int i = 0; i < depth; ++i) {
				newheapid = heapObjects.size();
				heapObjects.add(new HeapObjectInfo(this, t, allocid));
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
		List<Integer> copyids = funccopys.get(method);
		for (int cpyid : copyids) {
			Pointer heapids = locals.get(localName(method, cpyid, localname));
			for (int heapid : heapids.getAll()) {
				results.add(heapObjects.get(heapid).allocid);
			}
		}
		return results;
	}

	public void printall() {
		for (Map.Entry<String, Pointer> p : locals.entrySet()) {
			System.out.println("Result: " + p.getKey() + ":" + p.getValue().getAll().toString());
		}
	}
}
