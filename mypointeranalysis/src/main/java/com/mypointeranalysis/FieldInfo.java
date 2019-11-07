package com.mypointeranalysis;

import java.util.HashMap;

class FieldInfo {
    private static HashMap<String, FieldInfo> fieldinfos = new HashMap<>();
    public static FieldInfo getFieldInfo(String name, TypeInfo t) {
        if(!fieldinfos.containsKey(name)) {
            fieldinfos.put(name, new FieldInfo(name, t));
        }
        return fieldinfos.get(name);
    }

    private FieldInfo(String name, TypeInfo t) {
        this.name = name;
        this.t = t;
    }

    private String name;
    private TypeInfo t;

    public String getName() {
        return name;
    }

    public TypeInfo getT() {
        return t;
    }
}
