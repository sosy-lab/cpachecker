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
                    } catch(NumberFormatException a){
                                   try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                                try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
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
                    } catch(NumberFormatException a){
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                                try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                            try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
        } catch (NullPointerException e) {
            try {
                enteredG = false;
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
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
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
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        assert enteredH;
                    }
                    g();
                } catch(ArrayIndexOutOfBoundsException a){
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
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = false;
                        h();
                    } catch(NumberFormatException a){
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
                            enteredJ = true;
                        } finally {
                            assert enteredJ;
                        }
                        enteredH = true;
                    } finally {
                        try {
                            enteredJ = false;
                            j();
                        } catch(IllegalArgumentException a){
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
