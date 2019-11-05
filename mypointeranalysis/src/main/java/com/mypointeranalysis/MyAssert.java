package com.mypointeranalysis;

public class MyAssert {
    public static void myassert(boolean condition) {
        if(!condition) {
            System.out.println("ASSERT FAILED!------------------");
            new Exception().printStackTrace();
        }
    }
}
