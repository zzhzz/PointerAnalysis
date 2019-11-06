package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class SubCallTest extends A {
    @Override
    public B id(B b) {
        BenchmarkN.alloc(10);
        B b2 = new B();
        return b2;
    }

    public static A createA() {
        return new SubCallTest();
    }

    public static void main(String[] args) {
        A a = createA();
        BenchmarkN.alloc(11);
        B b1 = new B();
        B b2 = a.id(b1);
        BenchmarkN.test(1, b2);
    }
}
