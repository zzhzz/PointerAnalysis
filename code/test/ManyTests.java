package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;
import benchmark.objects.I;
import benchmark.objects.P;
import benchmark.objects.Q;

public class ManyTests extends A {
    ManyTests r;
    B f;

    private static void arrayTest() {
        BenchmarkN.alloc(1);
        A[] arr1 = new A[3];
        BenchmarkN.alloc(2);
        A[] arr2 = new A[4];
        BenchmarkN.alloc(3);
        B b1 = new B();
        BenchmarkN.alloc(4);
        A a1 = new A(b1);
        arr1[0] = a1;
        BenchmarkN.alloc(5);
        A a2 = new A();
        arr2[2] = a2;
        BenchmarkN.test(1, arr1);
        BenchmarkN.test(2, arr1[0]);
        BenchmarkN.test(3, arr1[0].f);
    }

    private static Q genQ() {
        BenchmarkN.alloc(1);
        Q result = new P(null);
        return result;
    }

    private static void castTest() {
        P curp = (P)myfunc();
        BenchmarkN.test(1, curp);
    }

    private static void exceptionTest() {
        Exception ex = new Exception();
        Exception e0 = null;
        try {
            throw ex;
        } catch (Exception e) {
            e0 = e;
        }
        BenchmarkN.test(5, e0);
    }

    private static void multiArrayTest() {
        BenchmarkN.alloc(1);
        A [][] multiarray = new A[3][5];
        BenchmarkN.alloc(2);
        A a = new A();
        multiarray[2][3] = a;
        A a2 = multiarray[1][4];
        BenchmarkN.test(1, a2);
    }

    private static void recArrayTest() {
        BenchmarkN.alloc(1);
        ManyTests r1 = new ManyTests();
        BenchmarkN.alloc(2);
        ManyTests r2 = new ManyTests();
        r1.r = r2;
        r2.r = r1;
        ManyTests r = r1.r.r.r.r.r.r.r.r.r;
        BenchmarkN.test(1, r);
    }

    private static A getA(int i) {
        if(i > 0) {
            return getA(i-1);
        } else {
            BenchmarkN.alloc(3);
            A a = new A();
            return a;
        }
    }

    private static void recTest() {
        A a = getA(10);
        BenchmarkN.test(1, a);
    }

    @Override
    public B id(B b) {
        BenchmarkN.alloc(10);
        B b2 = new B();
        return b2;
    }

    private static A createA() {
        return new ManyTests();
    }

    private static void subCallTest() {
        A a = createA();
        BenchmarkN.alloc(11);
        B b1 = new B();
        B b2 = a.id(b1);
        BenchmarkN.test(1, b2);
        A a2 = new A();
        if(args.length > 100) {
            a2 = new ManyTests();
        }
        B b3 = a2.id(b1);
        BenchmarkN.test(2, b3);
    }

    private static B id2(B in) {
        A result = new A(in);
        return result.f;
    }

    private static void multiCopyTest() {
        BenchmarkN.alloc(1);
        B b1 = new B();
        B b1_id = id2(b1);
        BenchmarkN.alloc(2);
        B b2 = new B();
        B b2_id = id2(b2);
        BenchmarkN.alloc(3);
        B b3 = new B();
        B b3_id = id2(b3);
        BenchmarkN.alloc(4);
        B b4 = new B();
        B b4_id = id2(b4);
        BenchmarkN.alloc(5);
        B b5 = new B();
        B b5_id = id2(b5);
        BenchmarkN.alloc(6);
        B b6 = new B();
        B b6_id = id2(b6);
        BenchmarkN.alloc(7);
        B b7 = new B();
        B b7_id = id2(b7);
        BenchmarkN.test(1, b1_id);
    }

    private static void unknownTest() {
        BenchmarkN.alloc(1); 
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();
        BenchmarkN.alloc(3);
        A c = new A();
        if (args.length > 1) a = b;
        BenchmarkN.test(1, a);
        BenchmarkN.test(2, c);
        System.out.println("Hello World!\n");
        ArrayList<Object> aobj = new ArrayList<>();
        aobj.add(a);
        aobj.add(b);
        A t = (A)aobj.get(1);
        A t2 = (A)aobj.get(1);
        BenchmarkN.alloc(4);
        B newb = new B();
        t.f = newb;
        BenchmarkN.test(3, t);
        B testb = t2.f;
        BenchmarkN.test(4, testb);
    }

    public static void main(String[] args) {
        arrayTest();
        castTest();
        exceptionTest();
        multiArrayTest();
        recArrayTest();
        recTest();
        subCallTest();
        unknownTest();
        multiCopyTest();
    }
}
