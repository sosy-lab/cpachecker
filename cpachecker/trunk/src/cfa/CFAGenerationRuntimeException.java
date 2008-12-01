package cfa;

/**
 * Handles runtime exceptions generated during CFA generation
 * @author erkan
 */
public class CFAGenerationRuntimeException extends RuntimeException
{
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
