package com.mypointeranalysis;

public class MyOutput {
    public static void myassert(boolean condition) {
        if(!condition) {
            System.out.println("ASSERT FAILED!------------------");
            new Exception().printStackTrace();
            WholeProgramTransformer.shouldprintall = true;
        }
    }

    public static void myprint(String str) {
        // System.out.println(str);
    }
}
