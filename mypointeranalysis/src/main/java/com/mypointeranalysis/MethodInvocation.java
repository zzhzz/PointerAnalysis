package com.mypointeranalysis;

import soot.SootMethod;
import soot.jimple.Stmt;

public class MethodInvocation {
    SootMethod sm;
    Stmt stmt;

    public MethodInvocation(SootMethod sm, Stmt stmt) {
        this.sm = sm;
        this.stmt = stmt;
    }

    public SootMethod getMethod() {
        return this.sm;
    }

    public Stmt getStmt() {
        return this.stmt;
    }
}