package org.sosy_lab.cpachecker.fshell.experiments.krall;
import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.FShell3Result;
import org.sosy_lab.cpachecker.efshell.Main;
public class InfoTest2 extends ExperimentalSeries  {

  @Test
  public void test002() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/data-structures/primitive/int2.cil.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }

}
