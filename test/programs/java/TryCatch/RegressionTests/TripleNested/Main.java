public class Main {
    private static int i = 0;
    private static Boolean enteredF = false;
    private static Boolean enteredG = false;
    private static Boolean enteredH = false;
    private static Boolean enteredJ = false;

    private static void f() {
        throw new NullPointerException(); 
    }
    
    private static void g() {
        throw new ArrayIndexOutOfBoundsException(); 
    }
    
    private static void h() {
        throw new NumberFormatException();
    }
    
    private static void j(){
        throw new IllegalArgumentException();
    }

    public static void main(String[] args) {
        try {
            while (i < 10) {
                try {
                    try {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException b){
                                   try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException c){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                                try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException d){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException e){
                    try {
                               try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException f){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException g){
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException h){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException i){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                                try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException j){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException k){
                                try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException l){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException m){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    assert enteredG;
                }
                f();
                i++;
            }
        } catch (NullPointerException n) {
            try {
                enteredG = false;
                try {
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException o){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException p){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException q){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException r){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException s){
                    try {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException t){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException u){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException v){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException w){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException x){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException y){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException z){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException aa){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    assert enteredG;
                }
            enteredF = true;
        }
        finally {
            try {
                enteredG = false;
                try {
                    try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ab){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException ac){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ad){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ae){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException af){
                    try {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ag){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException ah){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ai){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException aj){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    enteredG = true;
                } finally {
                    try {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException ak){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException al){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException am){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException an){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    assert enteredG;
                }
            assert enteredF;    
        }
 
    }
}
