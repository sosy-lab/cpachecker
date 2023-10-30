public class Main {
    private static int i = 0;
    private static boolean entered = false;

    private static void f() {
        throw new NullPointerException(); 
    }
    private static void g() {
        throw new ArrayIndexOutOfBoundsException();
    }

    private static void h(){
        g();
        assert false;
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                h();
                f();
                i++;
            }
        } catch (NullPointerException e) {
            entered = false;
        } catch (ArrayIndexOutOfBoundsException n){
            entered = true;
        }     
        assert entered;
    }
}
