package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class ExceptionTest extends Exception {
    A a;
    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        ExceptionTest ex1 = new ExceptionTest();
        BenchmarkN.alloc(2);
        ExceptionTest ex2 = new ExceptionTest();
        BenchmarkN.alloc(3);
        A a1 = new A();
        BenchmarkN.alloc(4);
        A a2 = new A();
        ex1.a = a1;
        ex2.a = a2;

        try {
            throw ex1;
        } catch(Exception e) {
            BenchmarkN.test(1, e);
            BenchmarkN.test(2, ((ExceptionTest)e).a);
        }
    }
}
