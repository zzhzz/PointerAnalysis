package test;

import java.util.ArrayList;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class UnknownTest {
    public static void main(String[] args) {
        BenchmarkN.alloc(1); 
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();
        BenchmarkN.alloc(3);
        A c = new A();
        if (args.length > 1) a = b;
        BenchmarkN.test(1, a);  // {1,2}
        BenchmarkN.test(2, c);  // {3}
        System.out.println("Hello World!\n");
        ArrayList<Object> aobj = new ArrayList<>();
        aobj.add(a);
        aobj.add(b);
        A t = (A)aobj.get(1);
        A t2 = (A)aobj.get(1);
        BenchmarkN.alloc(4);
        B newb = new B();
        t.f = newb;
        BenchmarkN.test(3, t); // {1,2}
        B testb = t2.f;
        BenchmarkN.test(4, testb); // {4}
    }
}