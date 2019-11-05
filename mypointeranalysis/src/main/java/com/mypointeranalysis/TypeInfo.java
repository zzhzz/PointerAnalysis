package com.mypointeranalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.util.Chain;

public class TypeInfo {
    static HashMap<String, TypeInfo> typeinfos = new HashMap<>();

    static TypeInfo getTypeInfo(RefLikeType t) {
        // System.out.println(t.toString());
        String tname = getTypeName(t);
        if (!typeinfos.containsKey(tname)) {
            TypeInfo newtypeinfo = new TypeInfo(t);
            typeinfos.put(tname, newtypeinfo);
            newtypeinfo.init_filed();
        }
        return typeinfos.get(tname);
    }

    static boolean shouldConsider(Type t) {
        return t instanceof RefLikeType;
    }

    static String getTypeName(RefLikeType t) {
        return t.toString();
    }

    static boolean canContain(TypeInfo child, TypeInfo parent) {
        if(child.isClass() && parent.isClass()) {
            SootClass scc = ((RefType)child.thistype).getSootClass();
            SootClass scp = ((RefType)parent.thistype).getSootClass();
            return Scene.v().getActiveHierarchy().isClassSubclassOf(scc, scp);
        } else {
            return getTypeName(child.thistype).equals(getTypeName(parent.thistype));
        }
    }

    TypeInfo(RefLikeType t) {
        fields = new HashMap<>();
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
                        fields.put(field.getSignature(), getTypeInfo((RefLikeType) ftype));
                    }
                }
            }
        } else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            Type subt = at.getElementType();
            if (subt instanceof RefLikeType) {
                RefLikeType eletype = (RefLikeType) subt;
                // System.out.println("T and subt: " + at.toString() + " " + subt.toString() + " " + Boolean.toString(eletype instanceof ArrayType));
                fields.put("#arrayvalue", getTypeInfo(eletype));
            }
        }
    }

    boolean isClass() {
        return thistype instanceof RefType;
    }

    SootClass getTypeClass() {
        return ((RefType) thistype).getSootClass();
    }

    RefLikeType getRefLikeType() {
        return thistype;
    }

    Set<Map.Entry<String,TypeInfo>> getFields() {
        return fields.entrySet();
    }

    HashMap<String, TypeInfo> fields;
    String typename;
    RefLikeType thistype;
}