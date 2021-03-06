package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class Hello {

  static int x = 0;

  public static void main2(){
    if(x != 0)
    {
      main(null);
    }
  }

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

    main2();
  }
}
