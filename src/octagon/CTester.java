package octagon;

public class CTester {

	public native void test ();

	static
	{
		System.loadLibrary("JOct");
	}

	public static void main(String[] args) {
		CTester c = new CTester();
		c.test();
	}

}
