public class Main {
    private static int i = 0;
    private static Boolean enteredF = false;
    private static Boolean enteredG = false;
    private static Boolean enteredH = false;

    private static void f() {
        throw new NullPointerException(); 
    }
    
    private static void g() {
        throw new ArrayIndexOutOfBoundsException(); 
    }
    
    private static void h() {
        throw new NumberFormatException();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                try {
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    assert enteredG;
                }
                f();
                i++;
            }
        } catch (NullPointerException e) {
            try {
                enteredG = false;
                try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    assert enteredG;
                }
            enteredF = true;
            System.out.println(e);
        }
        finally {
            try {
                enteredG = false;
                try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        enteredH = true;
                    } finally {
                        assert enteredH;
                    }
                    assert enteredG;
                }
            assert enteredF;    
        }
 
    }
}
