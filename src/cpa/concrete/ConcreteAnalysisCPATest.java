package cpa.concrete;

//import static org.junit.Assert.*;

import org.junit.Test;

public class ConcreteAnalysisCPATest {

  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/concreteAnalysis.properties";  
  
  @Test
  public void test_01() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
        
    cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_02() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/loop1.c";
        
    cmdline.CPAMain.main(lArguments);
  }
  
  @Test
  public void test_03() {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    cmdline.CPAMain.main(lArguments);
  }
  
/*  @Test
  public void testConcreteAnalysisCPA() {
    String[] lArguments = new String[3];
    
    lArguments[0] = "-config";
    lArguments[1] = "test/config/concreteAnalysis.properties";
    lArguments[2] = "test/tests/single/functionCall.c";
        
    cmdline.CPAMain.main(lArguments);
    
    fail("Not yet implemented");
  }

  @Test
  public void testGetAbstractDomain() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetMergeOperator() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetStopOperator() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetTransferRelation() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetInitialElement() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetInitialPrecision() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetPrecisionAdjustment() {
    fail("Not yet implemented");
  }
*/
}
