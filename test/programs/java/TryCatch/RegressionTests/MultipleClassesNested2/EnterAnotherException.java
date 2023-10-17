public class EnterAnotherException {

    private EnterException e;
    private Boolean entered = false;

    public EnterAnotherException(){
        e = new EnterException();
    }

    public void throwException() {
        
        try{
            e.throwException();
        } catch(NullPointerException e){
            entered = true;
        }
        assert entered;
    
        throw new ArrayIndexOutOfBoundsException(); 
    }
}
