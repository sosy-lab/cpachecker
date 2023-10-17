public class Main {
    private static int i = 0;
    private static boolean entered = false;

    private static void f() {
        i++;
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
        assert !entered;
    }
}
