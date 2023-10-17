public class Main {
    private static int i = 0;
    private static Boolean entered = false;

    private static void f() {
        PassAlong p = new PassAlong();
        p.passAlong();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                f();
                i++;
            }
        } catch (RuntimeException e) {
            entered = true;
        }
        assert entered;     
    }
}
