package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class RecRef {
    RecRef r;
    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        RecRef r1 = new RecRef();
        BenchmarkN.alloc(2);
        RecRef r2 = new RecRef();
        r1.r = r2;
        r2.r = r1;
        RecRef r = r1.r.r.r.r.r.r.r.r;
        BenchmarkN.test(1, r); // {1}
    }
}