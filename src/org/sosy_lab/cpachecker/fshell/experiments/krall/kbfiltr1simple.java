package org.sosy_lab.cpachecker.fshell.experiments.krall;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;


public class kbfiltr1simple extends ExperimentalSeries  {
  FileWriter fil;
  PrintWriter out;
  @Test
  public void test002() throws Exception {






    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/kbfiltr_simpl1.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/kbfiltr_simpl1.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95");
    Main.run(lArguments, pTestCase,out);





  }

  @Test
  public void test003() throws Exception{

    fil = new FileWriter("test/programs/fql/result/kbfiltr_simpl2.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/kbfiltr_simpl2.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95");
    Main.run(lArguments, pTestCase,out);
  }
  @Test
  public void test004() throws Exception{

    fil = new FileWriter("test/programs/fql/result/floppy_simpl3.cil.c.data",true);
    out = new PrintWriter(fil);


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/floppy_simpl3.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95");
    Main.run(lArguments, pTestCase,out);
  }

  @Test
  public void test005() throws Exception{

    fil = new FileWriter("test/programs/fql/result/floppy_simpl4.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/floppy_simpl4.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95");
    Main.run(lArguments, pTestCase,out);
  }


  @Test
  public void test006() throws Exception{

    fil = new FileWriter("test/programs/fql/result/cdaudio_simpl1.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/cdaudio_simpl1.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95,33,44,66");
    Main.run(lArguments, pTestCase,out);
  }


  @Test
  public void test007() throws Exception{

    fil = new FileWriter("test/programs/fql/result/diskperf_simpl1.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/diskperf_simpl1.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,10,0,95,44,55,66");
    Main.run(lArguments, pTestCase,out);
  }



}
