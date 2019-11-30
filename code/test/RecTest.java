package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class RecTest {
    public static A getA(int i) {
        if(i > 0) {
            return getA(i-1);
        } else {
            BenchmarkN.alloc(3);
            A a = new A();
            return a;
        }
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        RecTest r1 = new RecTest();
        BenchmarkN.alloc(2);
        RecTest r2 = new RecTest();
        A a = getA(10);
        BenchmarkN.test(1, a); // {3}
    }
}
