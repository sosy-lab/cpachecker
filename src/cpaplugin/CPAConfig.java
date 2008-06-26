package cpaplugin;

public class CPAConfig
{
    public static final boolean showDebugOutput = false;
    public static final boolean exportDOTfiles = true;
    public static final String DOTOutputPath = "/home/erkan/cpa";
    public static final boolean simplifyCFA = false;
    // Do we want to combine multiple assignments and declerations if simplifyCFA == true
    // note: if simplifyCFA = true and combineBlockStatements = false
    // only blank edges will be removed while simplfying
    public static final boolean combineBlockStatements = false;
    //
    public static boolean isAnalysisInterprocedural = false;
}
