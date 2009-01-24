package cpa.explicit;

import cmdline.CPAMain;

public class ExplicitAnalysisConstants {

  public static int threshold = 
    Integer.parseInt(CPAMain.cpaConfig.getProperty("explicitAnalysis.threshold"));
  
}
