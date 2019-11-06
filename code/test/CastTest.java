package test;

// import java.util.ArrayList;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;
import benchmark.objects.P;
import benchmark.objects.Q;

public class CastTest {

    static Q myfunc() {
        BenchmarkN.alloc(1);
        return new P(null);
    }
    public static void main(String[] args) {
        P curp = (P)myfunc();
        BenchmarkN.test(1, curp); // {1}
    }
}