package predicateabstraction;

import cmdline.CPAMain;

public class PredAbstractionConstants {

	public static final String predicateListPath = CPAMain.cpaConfig.getProperty("predicates.path");

	public static String getFileLoc(String functionName, String fileName){
		return predicateListPath + functionName + "@" + fileName + ".predicates";
	}

	public static String getStarOperator(String variableName){
		return "__cpa___starOp__[" + variableName + "]";
	}
}
