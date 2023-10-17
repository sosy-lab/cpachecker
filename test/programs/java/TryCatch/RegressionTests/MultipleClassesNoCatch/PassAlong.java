public class PassAlong {

    private EnterException e = null;
    private Boolean entered = false;

    public PassAlong(){
        e = new EnterException();
    }

    public void passAlong() {
        e.throwException();
        //this next line should never be called
        assert entered;
    }
}
