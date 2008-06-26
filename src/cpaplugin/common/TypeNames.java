package cpaplugin.common;

/*
 * NOTE: This class is not used presently, and is here as part of an un-submitted experiment
 *  to make the CPA algorithm completely language independent.  However, in the end I decided it
 *  better to save effort now, and to only create my object model if it is ever desired to support
 *  languages other than C for this CPA Plugin.
 */
public class TypeNames
{
    public static final String BoolStr = "bool";
    public static final String CharStr = "char";
    public static final String DoubleStr = "double";
    public static final String FloatStr = "float";
    public static final String IntStr = "int";
    public static final String VoidStr = "void";
    public static final String UnknownStr = "unknown";
    
    private TypeNames () {}
}
