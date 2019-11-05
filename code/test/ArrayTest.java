package test;

// import java.util.ArrayList;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class ArrayTest {

  public static void main(String[] args) {
    A item[] = new A[3];
    // ArrayList<A> list = new ArrayList<>();
    BenchmarkN.alloc(1); 
    item[0] = new A();
    // list.add(item[0]);
    BenchmarkN.alloc(2);
    item[1] = new A();
    BenchmarkN.alloc(3); 
    item[2] = new A();
    BenchmarkN.test(1, item);
    BenchmarkN.test(2, item[0]);
    // BenchmarkN.test(3, list.get(0));
    A b = ArrayTest.a;
    BenchmarkN.test(4, b);
  }

  static A a;
  static{
      BenchmarkN.alloc(4); 
      ArrayTest.a = new A();
  }
}
