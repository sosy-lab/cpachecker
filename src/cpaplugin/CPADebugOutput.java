package cpaplugin;

public class CPADebugOutput
{
    public static void debugPrintln (String s)
    {
        if (CPAConfig.showDebugOutput)
            System.out.println (s);
    }
}
