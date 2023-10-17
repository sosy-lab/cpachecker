public class Main {
    private static int i = 0;
    private static boolean entered = false;

    private static void f() {
        throw new RuntimeException(); 
    }
    private static void g() {
        throw new NullPointerException();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                f();
                g();
                i++;
            }
        } catch (RuntimeException e) {
            entered = true;
        } catch (NullPointerException n){
            entered = false;
        }     
        assert entered;
    }
}
