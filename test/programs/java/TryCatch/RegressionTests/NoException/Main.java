public class Main {
    private static int i = 0;
    private static Boolean entered = false;

    private static void f() {
        i = i + 1;
    }

    public static void main(String[] args) {
        while (i < 10) {
            f();
            i++;
            entered = true;
        }

        assert entered;     
    }
}
