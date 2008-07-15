package predicateabstraction;

import cpaplugin.CPAConfig;

public class PredAbstractionConstants {

	public static final String predicateListPath = CPAConfig.predicateListPath;
	
	public static String getFileLoc(String functionName){
		return predicateListPath + functionName + ".predicates";
	}
	
	public static String getStarOperator(String variableName){
		return "__cpa___starOp__[" + variableName + "]";
	}
}
