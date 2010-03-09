package cpa.alwaystop;

import org.junit.Test;


public class AlwaysTopCPATest {
  
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/alwaysTopAnalysis.properties";  

  @Test
  public void test_01() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
        
    cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_02() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/loop1.c";
        
    cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_03() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/uninitVars.cil.c";
        
    cmdline.CPAMain.main(lArguments);
  }

}
