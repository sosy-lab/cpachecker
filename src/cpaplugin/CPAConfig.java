package cpaplugin;

import java.util.logging.Level;

public class CPAConfig
{
    public static final String LogPath = "/media/sda7/cpa/CPALog.txt";
    // FINEST, OFF
    public static final Level LogLevel = Level.OFF;
    public static final boolean exportDOTfiles = true;
    public static final String DOTOutputPath = "/home/erkan/cpa/";
    public static final boolean simplifyCFA = false;
    // Do we want to combine multiple assignments and declerations if simplifyCFA == true
    // note: if simplifyCFA = true and combineBlockStatements = false
    // only blank edges will be removed while simplfying
    public static final boolean combineBlockStatements = false;
    // set to true if you want to run an interprocedural analysis
    public static boolean isAnalysisInterprocedural = true;
    // where to put files that has predicates
	public static final String predicateListPath = "/home/erkan/cpa/";
	// path of csisat
	public static final String csisatPath = "/home/erkan/csisat/bin/csisat";
	// entry function
	public static final String entryFunction = "main";
	public static final String workspacedata = "/home/erkan/cpa/runtime-config/";
}
