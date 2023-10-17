public class Main {
    private static int i = 0;
    private static Boolean entered = false;

    private static void f() {
        throw new RuntimeException(); 
    }
    
    private static void g() {
        f();
        entered = true;
        assert !entered;
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                g();
                i++;
            }
        } catch (RuntimeException e) {
            entered = false;
        }
        assert !entered;     
    }
}
