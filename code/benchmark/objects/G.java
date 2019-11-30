package benchmark.objects;

public class G implements I {
	// G and H implement I

	public A a;

	public A foo(A a) {
		this.a = a;
		return a;
	}
}
