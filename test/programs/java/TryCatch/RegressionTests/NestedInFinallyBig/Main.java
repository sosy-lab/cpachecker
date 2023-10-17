public class Main {
    private static int i = 0;
    private static Boolean enteredF = false;
    private static Boolean enteredG = false;

    private static void f() {
        throw new RuntimeException(); 
    }
    
        private static void g() {
        int number = 1;
        for(int x = 0; x < 700; x++){
            if (number > 350) {
                throw new ArrayIndexOutOfBoundsException(); 
            } else if(x % 5 == 0){
                number = number * 3;
            } else if (x % 3 == 0){
                number = number * 2;
            } else if( x%2 == 0) {
                number = number -3;
            } else{
                number = number -1;
            } 
        }
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                f();
                i++;
            }
        } catch (RuntimeException e) {
            enteredF = true;
        }
        finally {
                try {
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
                    enteredG = true;
                } finally {
                    assert enteredG;
                }
               assert enteredF;    
        }
 
    }
}
