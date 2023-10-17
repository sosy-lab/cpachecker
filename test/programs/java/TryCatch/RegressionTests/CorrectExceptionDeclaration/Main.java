public class Main {
    private static int i = 0;

    private static void f() {
        throw new RuntimeException(); 
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                f();
                i++;
            }
        } catch (RuntimeException e) {
            assert e instanceof RuntimeException;
        }     
    }
}
