public class MultipleCatchEnterFirst {
    private static int i = 0;
    private static boolean entered = false;

    private static void f() {
        throw new NullPointerException(); 
    }
    private static void g() {
        throw new ArrayIndexOutOfBoundsException();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                f();
                g();
                i++;
            }
        } catch (NullPointerException e) {
            entered = true;
        } catch (ArrayIndexOutOfBoundsException n){
            entered = false;
        }     
        assert entered;
    }
}
