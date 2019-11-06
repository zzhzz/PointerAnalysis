package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;
import benchmark.objects.G;
import benchmark.objects.I;

public class ArrayTest {

  public static void main(String[] args) {
    A item[] = new A[3];
    BenchmarkN.alloc(1); 
    item[0] = new A();
    BenchmarkN.alloc(2);
    item[1] = new A();
    BenchmarkN.alloc(3); 
    item[2] = new A();
    BenchmarkN.test(1, item); // {}
    BenchmarkN.test(2, item[0]); // {1,2,3}
    A b = ArrayTest.a;
    BenchmarkN.test(4, b); // {4}

    BenchmarkN.alloc(7);
    A ax = new A();
    G gx = new G();
    I fx = gx;
    A cx = fx.foo(ax);
    A bx = gx.a;
    BenchmarkN.test(5, bx); // {7}
    BenchmarkN.test(6, cx); // {7}

    ArrayTest.a0 = b;
    BenchmarkN.test(7, ArrayTest.a0); // {4}
  }

  static A a;
  static A a0;
  static{
      BenchmarkN.alloc(4); 
      ArrayTest.a = new A();
  }
}
