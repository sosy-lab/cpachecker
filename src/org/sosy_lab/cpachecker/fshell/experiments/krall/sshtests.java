package org.sosy_lab.cpachecker.fshell.experiments.krall;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;
import org.sosy_lab.cpachecker.efshell.ExperimentalSeries;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;


public class sshtests extends ExperimentalSeries  {
  FileWriter fil;
  PrintWriter out;

  @Test
  public void test001() throws Exception {




    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_clnt_1.cil.c",
                                        "main",
                                        true);

    fil = new FileWriter("test/programs/fql/result/s3_clnt_1.cil.c.data",true);
    out = new PrintWriter(fil);



    TestCase pTestCase;
     pTestCase= TestCase.fromString("p,-3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4,3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4,3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4");
    Main.run(lArguments, pTestCase,out);





  }

  @Test
  public void test002() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_clnt_2.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_clnt_2.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,-3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4");
    Main.run(lArguments, pTestCase,out);
  }
  @Test
  public void test003() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_clnt_3.cil.c.data",true);
    out = new PrintWriter(fil);


    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_clnt_3.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,-3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4,3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4");
    Main.run(lArguments, pTestCase,out);
  }

  @Test
  public void test004() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_clnt_4.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_clnt_4.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,-3,0,95,4,123,4,4,4,4,4,8,8,8,8,8,8,8,8,8,8,8,4,4,4,4,4,4,4,4,-1,5,5,5,5,5,4");
    Main.run(lArguments, pTestCase,out);
  }


  @Test
  public void test005() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_1.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_1.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }


  @Test
  public void test006() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_2.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_2.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }

  @Test
  public void test007() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_3.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_3.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }

  @Test
  public void test008() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_4.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_4.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }
  @Test
  public void test009() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_6.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_6.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }

  @Test
  public void test010() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_7.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_7.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }
  @Test
  public void test0011() throws Exception{

    fil = new FileWriter("test/programs/fql/result/s3_srvr_8.cil.c.data",true);
    out = new PrintWriter(fil);



    String[] lArguments;
    lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/s3_srvr_8.cil.c",
                                        "main",
                                        true);

    TestCase pTestCase;
    pTestCase= TestCase.fromString("p,0,0,-1,-12288,-16385,3,3,1,0,0,0,3,1,1,0,1,0,-30,0,0,0,0,0,0,0,0,3,3,3,3,3,3,3,2,0,2,3,3,3,0");
    Main.run(lArguments, pTestCase,out);
  }






}
