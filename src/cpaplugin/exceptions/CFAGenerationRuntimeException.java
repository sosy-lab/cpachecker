package cpaplugin.exceptions;

public class CFAGenerationRuntimeException extends RuntimeException
{
    // Exceptions are serializable, and warnings are generated without this.  No importance for now.
    public static final long serialVersionUID = 1L;
    
    public CFAGenerationRuntimeException ()
    {
        super ();
    }
    
    public CFAGenerationRuntimeException (String s)
    {
        super (s);
    }
    
    public CFAGenerationRuntimeException (String s, int lineNum)
    {
        super (s + " Triggered by line #: " + lineNum);
    }
}
