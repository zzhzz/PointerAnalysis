package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.*;

public class VirtualTest
{
    public VirtualTest a, b, c, d, e, f;
    public static void main(String[] args) {
        // BenchmarkN.
        P p = new P(null);
        Q q = p;
        benchmark.internal.BenchmarkN.test(123, p);
        VirtualTest x = new VirtualTest();
        try{
            System.out.println(x.a.b.c.d.e.f);
        } catch(Exception ex)
        {
            System.out.println("x");
        }
        P[][] p2 = new P[3][];
        System.out.println(p2);
    }
}