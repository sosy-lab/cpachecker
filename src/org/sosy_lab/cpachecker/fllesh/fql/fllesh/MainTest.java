package org.sosy_lab.cpachecker.fllesh.fql.fllesh;

import org.junit.Before;
import org.junit.Test;

public class MainTest {

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void testMain001() throws Exception {
    String[] lArguments = new String[2];
    
    lArguments[0] = "COVER STATES(@ENTRY(f))";
    lArguments[1] = "test/programs/simple/functionCall.c";
    
    Main.main(lArguments);
  }
  
  @Test
  public void testMain002() throws Exception {
    String[] lArguments = new String[2];
    
    lArguments[0] = "COVER EDGES(@ENTRY(f))";
    lArguments[1] = "test/programs/simple/functionCall.c";
    
    Main.main(lArguments);
  }

  @Test
  public void testMain003() throws Exception {
    String[] lArguments = new String[2];
    
    lArguments[0] = "COVER EDGES(UNION(@ENTRY(f), @ENTRY(main)))";
    lArguments[1] = "test/programs/simple/functionCall.c";
    
    Main.main(lArguments);
  }
  
}
