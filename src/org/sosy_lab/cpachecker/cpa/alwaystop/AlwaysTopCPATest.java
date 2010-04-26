package org.sosy_lab.cpachecker.cpa.alwaystop;

import org.junit.Before;
import org.junit.Test;


public class AlwaysTopCPATest {
  
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/alwaysTopAnalysis.properties";  

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test_01() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
        
    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_02() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/loop1.c";
        
    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_03() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/uninitVars.cil.c";
        
    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }

}
