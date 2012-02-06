package org.sosy_lab.cpachecker.fshell.experiments.krall;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;


public class DiskperfTests extends ExperimentalSeries  {
  FileWriter fil;
  PrintWriter out;

  @Test
  public void test001() throws Exception {

    Main.OINTPR=1;


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/ntdrivers/diskperf.i.cil.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/diskperf.i.cil.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,1,0,0,0,1,1");
    Main.run(lArguments, pTestCase,out);





  }




}
