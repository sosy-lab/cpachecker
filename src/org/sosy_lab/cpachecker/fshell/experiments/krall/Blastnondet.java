package org.sosy_lab.cpachecker.fshell.experiments.krall;
import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;

public class Blastnondet extends ExperimentalSeries  {

  @Test
  public void test002() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/data-structures/primitive/blast.cil.c",
                                        "main",
                                        true);
    TestCase pTestCase = TestCase.fromString("p,10,0,95,44,4");
    Main.run(lArguments, pTestCase,null);
  }

}
