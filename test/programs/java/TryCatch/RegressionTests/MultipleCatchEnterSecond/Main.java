public class Main {
    private static int i = 0;
    private static boolean entered = false;

    private static void f() {
        i++;
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
        } catch (ArrayIndexOutOfBoundsException e) {
            entered = false;
        } catch (NullPointerException n){
            entered = true;
        }
        assert entered;     
    }
}
