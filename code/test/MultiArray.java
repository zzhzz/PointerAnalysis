package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;
import benchmark.objects.G;
import benchmark.objects.I;

public class MultiArray {
    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A [][] multiarray = new A[3][5];
        BenchmarkN.alloc(2);
        A a = new A();
        multiarray[2][3] = a;
        A a2 = multiarray[1][4];
        BenchmarkN.test(1, a2);
    }
}
