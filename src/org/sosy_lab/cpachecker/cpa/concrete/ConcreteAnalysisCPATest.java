package org.sosy_lab.cpachecker.cpa.concrete;

//import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ConcreteAnalysisCPATest {

  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/concreteAnalysis.properties";  

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
  
/*  @Test
  public void testConcreteAnalysisCPA() {
    String[] lArguments = new String[3];
    
    lArguments[0] = "-config";
    lArguments[1] = "test/config/concreteAnalysis.properties";
    lArguments[2] = "test/programs/simple/functionCall.c";
        
    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
    
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
