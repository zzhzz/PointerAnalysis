package test;

import java.util.ArrayList;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;
import benchmark.objects.I;
import benchmark.objects.P;
import benchmark.objects.Q;

public class ManyTests extends A {
    ManyTests r;
    B f;
    B newb;

    private static void test_01_overrideTest(String[] args) {
        BenchmarkN.alloc(101);
        B b1 = new B();
        BenchmarkN.alloc(102);
        B b2 = new B();
        BenchmarkN.alloc(103);
        ManyTests mt = new ManyTests();
        mt.f = b1;
        ((A) mt).f = b2;
        BenchmarkN.test(101, mt.f);
        BenchmarkN.test(102, ((A) mt).f);
    }

    private static void test_02_deliverTest(String[] args) {
        BenchmarkN.alloc(201);
        ManyTests mt1 = new ManyTests();
        BenchmarkN.alloc(202);
        B b1 = new B();
        mt1.newb = b1;
        A a1 = mt1;
        ManyTests mt2 = (ManyTests) a1;
        BenchmarkN.test(201, mt2.newb);
    }

    private static void test_03_arrayTest(String[] args) {
        BenchmarkN.alloc(301);
        A[] arr1 = new A[3];
        BenchmarkN.alloc(302);
        A[] arr2 = new A[4];
        BenchmarkN.alloc(303);
        B b1 = new B();
        BenchmarkN.alloc(304);
        A a1 = new A(b1);
        arr1[0] = a1;
        BenchmarkN.alloc(305);
        A a2 = new A();
        arr2[2] = a2;
        BenchmarkN.test(301, arr1);
        BenchmarkN.test(302, arr1[0]);
        BenchmarkN.test(303, arr1[0].f);
    }

    private static Q test_04_genQ() {
        BenchmarkN.alloc(401);
        Q result = new P(null);
        return result;
    }

    private static void test_04_castTest(String[] args) {
        P curp = (P) test_04_genQ();
        BenchmarkN.test(401, curp);
    }

    private static void test_05_exceptionTest(String[] args) {
        BenchmarkN.alloc(501);
        Exception ex = new Exception();
        Exception e0 = null;
        try {
            throw ex;
        } catch (Exception e) {
            e0 = e;
        }
        BenchmarkN.test(501, e0);
    }

    private static void test_06_multiArrayTest(String[] args) {
        BenchmarkN.alloc(601);
        A[][] multiarray = new A[3][5];
        BenchmarkN.alloc(602);
        A a = new A();
        multiarray[2][3] = a;
        A a2 = multiarray[1][4];
        BenchmarkN.test(601, a2);
    }

    private static void test_07_recArrayTest(String[] args) {
        BenchmarkN.alloc(701);
        ManyTests r1 = new ManyTests();
        BenchmarkN.alloc(702);
        ManyTests r2 = new ManyTests();
        r1.r = r2;
        r2.r = r1;
        ManyTests r = r1.r.r.r.r.r.r.r.r.r;
        BenchmarkN.test(701, r);
    }

    private static A test_08_getA(int i) {
        if (i > 0) {
            return test_08_getA(i - 1);
        } else {
            BenchmarkN.alloc(803);
            A a = new A();
            return a;
        }
    }

    private static void test_08_recTest(String[] args) {
        A a = test_08_getA(10);
        BenchmarkN.test(801, a);
    }

    @Override
    public B id(B b) {
        BenchmarkN.alloc(903);
        B b2 = new B();
        return b2;
    }

    private static A test_09_createA() {
        return new ManyTests();
    }

    private static void test_09_subCallTest(String[] args) {
        A a = test_09_createA();
        BenchmarkN.alloc(901);
        B b1 = new B();
        B b2 = a.id(b1);
        BenchmarkN.test(902, b2);
        A a2 = new A();
        if (args.length > 100) {
            a2 = new ManyTests();
        }
        B b3 = a2.id(b1);
        BenchmarkN.test(901, b3);
    }

    private static B test_10_id(B in) {
        A result = new A();
        result.f = in;
        return result.f;
    }

    private static void test_10_multiCopyTest(String[] args) {
        BenchmarkN.alloc(1001);
        B b1 = new B();
        B b1_id = test_10_id(b1);
        BenchmarkN.alloc(1002);
        B b2 = new B();
        B b2_id = test_10_id(b2);
        BenchmarkN.alloc(1003);
        B b3 = new B();
        B b3_id = test_10_id(b3);
        BenchmarkN.alloc(1004);
        B b4 = new B();
        B b4_id = test_10_id(b4);
        BenchmarkN.alloc(1005);
        B b5 = new B();
        B b5_id = test_10_id(b5);
        BenchmarkN.alloc(1006);
        B b6 = new B();
        B b6_id = test_10_id(b6);
        BenchmarkN.alloc(1007);
        B b7 = new B();
        B b7_id = test_10_id(b7);
        BenchmarkN.test(1001, b1_id);
    }

    private static void test_11_unknownTest(String[] args) {
        BenchmarkN.alloc(1101);
        A a = new A();
        BenchmarkN.alloc(1102);
        A b = new A();
        BenchmarkN.alloc(1103);
        A c = new A();
        if (args.length > 1)
            a = b;
        BenchmarkN.test(1101, a);
        BenchmarkN.test(1102, c);
        System.out.println("Hello World!\n");
        ArrayList<Object> aobj = new ArrayList<>();
        aobj.add(a);
        A t = (A) aobj.get(1);
        A t2 = (A) aobj.get(1);
        BenchmarkN.alloc(1104);
        B newb = new B();
        t.f = newb;
        BenchmarkN.test(1103, t);
        B testb = t2.f;
        BenchmarkN.test(1104, testb);
    }

    public static void main(String[] args) {
        test_01_overrideTest(args);
        test_02_deliverTest(args);
        test_03_arrayTest(args);
        test_04_castTest(args);
        test_05_exceptionTest(args);
        test_06_multiArrayTest(args);
        test_07_recArrayTest(args);
        test_08_recTest(args);
        test_09_subCallTest(args);
        test_10_multiCopyTest(args);
        test_11_unknownTest(args);
    }
}
