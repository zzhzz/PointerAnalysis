package com.mypointeranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.ArrayType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.util.Chain;

public class TypeInfo {
    private static HashMap<String, TypeInfo> typeinfos = new HashMap<>();
    private static SootClass unknownclass;

    public static TypeInfo getTypeInfo(RefLikeType t) {
        if (t == null)
            return null;
        // System.out.println(t.toString());
        String tname = getTypeName(t);
        if (!typeinfos.containsKey(tname)) {
            TypeInfo newtypeinfo = new TypeInfo(t);
            typeinfos.put(tname, newtypeinfo);
            newtypeinfo.init_filed();
        }
        return typeinfos.get(tname);
    }

    public static String getTypeName(RefLikeType t) {
        return t.toString();
    }

    public static TypeInfo getArrayTypeInfo(TypeInfo t) {
        return TypeInfo.getTypeInfo(t.thistype.getArrayType());
    }

    public static SootClass getUnknownClass() {
        if (unknownclass == null) {
            unknownclass = new SootClass(Anderson.UNKNOWN_CLASS);
            SootField f = new SootField(Anderson.UNKNOWN_FIELD, RefType.v(Scene.v().getSootClass("java.lang.Object")));
            unknownclass.addField(f);
        }
        return unknownclass;
    }

    public static boolean canContain(TypeInfo child, TypeInfo parent) {
        if (child.isClass() && parent.isClass()) {
            SootClass scc = ((RefType) child.thistype).getSootClass();
            SootClass scp = ((RefType) parent.thistype).getSootClass();
            if (scc.getName().equals(Anderson.UNKNOWN_CLASS) || scp.getName().equals(Anderson.UNKNOWN_CLASS))
                return true;
            return Scene.v().getFastHierarchy().canStoreClass(scc, scp);
        } else {
            return getTypeName(child.thistype).equals(getTypeName(parent.thistype));
        }
    }

    public static TypeInfo getUnknownType() {
        return getTypeInfo(RefType.v(getUnknownClass()));
    }

    public static TypeInfo getClassTypeByName(String name) {
        return TypeInfo.getTypeInfo(RefType.v(name));
    }

    private TypeInfo(RefLikeType t) {
        fields = new ArrayList<>();
        typename = getTypeName(t);
        thistype = t;

        if (typename == null) {
            throw new RuntimeException("Cannot create typeinfo for this class");
        }
    }

    private void init_filed() {
        RefLikeType t = thistype;
        if (t instanceof RefType) {
            RefType rt = (RefType) t;
            SootClass sc = rt.getSootClass();
            Chain<SootField> thisfields = sc.getFields();
            for (SootField field : thisfields) {
                Type ftype = field.getType();
                if (ftype instanceof RefLikeType) {
                    String ftname = getTypeName((RefLikeType) ftype);
                    if (ftname != null) {
                        fields.add(FieldInfo.getFieldInfo(field.getSignature(), getTypeInfo((RefLikeType) ftype)));
                    }
                }
            }
        } else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            Type subt = at.getElementType();
            if (subt instanceof RefLikeType) {
                RefLikeType eletype = (RefLikeType) subt;
                fields.add(FieldInfo.getFieldInfo(Anderson.ARRAY_FIELD + "@@" + TypeInfo.getTypeName(eletype),
                        getTypeInfo(eletype)));
            }
        }
    }

    public boolean isClass() {
        return thistype instanceof RefType;
    }

    public SootClass getTypeClass() {
        return ((RefType) thistype).getSootClass();
    }

    public RefLikeType getRefLikeType() {
        return thistype;
    }

    public Iterable<FieldInfo> getFields() {
        return fields;
    }

    public FieldInfo getField(int i) {
        return fields.get(i);
    }

    public int getFieldCount() {
        return fields.size();
    }

    private List<FieldInfo> fields;
    private String typename;
    private RefLikeType thistype;
}