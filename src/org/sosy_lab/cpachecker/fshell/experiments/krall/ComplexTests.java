package org.sosy_lab.cpachecker.fshell.experiments.krall;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;


public class ComplexTests extends ExperimentalSeries  {
  FileWriter fil;
  PrintWriter out;

  @Test
  public void test001() throws Exception {

    Main.OINTPR=1;


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/data-structures/primitive/dint.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/dint.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,-3");
    Main.run(lArguments, pTestCase,out);





  }


  @Test
  public void test002() throws Exception {

    Main.OINTPR=1;


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/ntdrivers/kbfiltr.i.cil.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/kbfiltr.i.cil.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,-3,-4,-7,3,6,8,9,9,9");
    Main.run(lArguments, pTestCase,out);





  }


  @Test
  public void test003() throws Exception {

    Main.OINTPR=1;


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/ssh/s3_clnt.blast.01.i.cil.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/s3_clnt.blast.01.i.cil.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,-3,-4,-7,3,6,8,9,9,9");
    Main.run(lArguments, pTestCase,out);





  }
  @Test
  public void test004() throws Exception {

    Main.OINTPR=1;


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/data-structures/primitive/drecursive.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/drecursive.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,-3,-4,-7,3,6,8,9,9,9");
    Main.run(lArguments, pTestCase,out);





  }


}
