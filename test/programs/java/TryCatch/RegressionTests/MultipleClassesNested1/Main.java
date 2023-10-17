public class Main {
    private static int i = 0;
    private static Boolean enteredF = false;
    private static Boolean enteredG = false;

    private static void f() {
        EnterException e = new EnterException();
        e.throwException();
    }
    
    private static void g() {
        EnterAnotherException e = new EnterAnotherException();
        e.throwException();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                try{ 
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    enteredG = true;
                }
                assert enteredG;
                f();
                i++;
            }
        } catch (NullPointerException e) {
            enteredF = true;
        }
        assert enteredF;     
    }
}
