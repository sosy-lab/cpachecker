package cpaplugin;

import java.util.logging.Level;

public class CPAConfig
{
    public static boolean showDebugOutput = false;
    public static String LogPath = "/media/sda7/cpa/CPALog.txt";
    // FINE, FINER, FINEST, OFF
    public static Level LogLevel = Level.OFF;
    public static final boolean exportDOTfiles = true;
    public static String DOTOutputPath =
        "/home/erkan/cpa/";
    public static final boolean simplifyCFA = false;
    // Do we want to combine multiple assignments and declerations if simplifyCFA == true
    // note: if simplifyCFA = true and combineBlockStatements = false
    // only blank edges will be removed while simplfying
    public static final boolean combineBlockStatements = false;
    // set to true if you want to run an interprocedural analysis
    public static boolean isAnalysisInterprocedural = true;
    // where to put files that has predicates
    public static String predicateListPath = 
        "/home/erkan/cpa/";
    // path of csisat
    public static String csisatPath = 
        "/home/erkan/csisat/bin/csisat";
    // entry function
    public static String entryFunction = "main";
    
    // which strategy to adopt for visiting states? DFS or BFS? BFS, unless
    // useBFSVisit is false
    public static boolean useBFSVisit = true;

    public static String workspacedata = "/home/erkan/cpa/runtime-config/";
}
