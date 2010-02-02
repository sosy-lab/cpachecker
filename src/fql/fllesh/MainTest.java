package fql.fllesh;

import org.junit.Test;

public class MainTest {

  @Test
  public void testMain001() throws Exception {
    String[] lArguments = new String[2];
    
    lArguments[0] = "COVER STATES(ID)";
    //lArguments[1] = "test/tests/single/functionCall.c";
    //lArguments[1] = "test/tests/single/blast_incorrect.cil.c";
    //lArguments[1] = "test/tests/single/lock-loop.cil.c";
    lArguments[1] = "test/tests/single/ex2.cil.c";
    
    Main.main(lArguments);
  }

}
