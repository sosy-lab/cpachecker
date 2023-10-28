public class NestedInTry {
    private static int i = 0;
    private static Boolean enteredF = false;
    private static Boolean enteredG = false;

    private static void f() {
        throw new RuntimeException(); 
    }
    
        private static void g() {
        throw new ArrayIndexOutOfBoundsException(); 
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                try {
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    enteredG = true;
                } finally {
                    assert enteredG;
                }
                f();
                i++;
            }
        } catch (RuntimeException e) {
            enteredF = true;
        }
        finally {
               assert enteredF;    
        }
 
    }
}
