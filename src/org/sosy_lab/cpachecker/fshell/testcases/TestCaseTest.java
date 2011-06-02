package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.MultiprocessFShell3;

public class TestCaseTest {

  @Test
  public void testFromFileString() throws IOException {
    Collection<TestCase> lTestSuite = TestCase.fromFile("test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.bb.001.tst");

    System.out.println(lTestSuite);

    for (TestCase lTestCase : lTestSuite) {
      System.out.println(lTestCase.toCFunction());
    }
  }

  @Test
  public void testToInputString() throws IOException {
    Collection<TestCase> lTestSuite = TestCase.fromFile("test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.bb.001.tst");

    System.out.println(lTestSuite);

    for (TestCase lTestCase : lTestSuite) {
      System.out.println(lTestCase.toInputString());
    }
  }

  @Test
  public void testToInputFile() throws IOException {
    Collection<TestCase> lTestSuite = TestCase.fromFile("test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.bb.001.tst");

    System.out.println(lTestSuite);

    for (TestCase lTestCase : lTestSuite) {
      File lTmpFile = File.createTempFile("input", ".txt");

      System.out.println(lTmpFile.getAbsolutePath());

      lTestCase.toInputFile(lTmpFile);
    }
  }

  @Test
  public void test() throws IOException {
    NondetToInput.replace("test/programs/fql/locks/test_locks_1.c", "test/output/test_locks_1.c");
  }

  @Test
  public void testGcov1() throws IOException, InterruptedException {
    NondetToInput.gcov("test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.cil.c", "test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.bb.001.tst");
  }

  @Test
  public void testGcov2() throws IOException, InterruptedException {
    NondetToInput.gcov("test/programs/fql/ntdrivers-simplified/cdaudio_simpl1.cil.c", "test/programs/fql/ntdrivers-simplified/cdaudio_simpl1.bb.001.tst");
  }

  @Test
  public void testGcov3() throws IOException, InterruptedException {
    NondetToInput.gcov("test/programs/fql/ntdrivers-simplified/diskperf_simpl1.cil.c", "test/programs/fql/ntdrivers-simplified/diskperf_simpl1.bb.001.tst");
  }

  @Test
  public void testCombination() throws IOException, InterruptedException, InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    NondetToInput.fshell2("test/programs/fql/locks/test_locks_15.c", "main", "COVER @BASICBLOCKENTRY", 3);
  }

  @Test
  public void testlocks15BB2() throws Exception {
    String[] lArguments = new String[9];

    lArguments[0] = Main.BASIC_BLOCK_2_COVERAGE;
    lArguments[1] = "test/programs/fql/locks/test_locks_15.c";
    lArguments[2] = "main";
    lArguments[3] = "--withoutCilPreprocessing";
    lArguments[4] = "--out=feasibility2.fs3";
    lArguments[5] = "--tout=testsuite2.tst";
    lArguments[6] = "--in=feasibility2.fs3";
    lArguments[7] = "--logging";
    lArguments[8] = "--append";

    Main.run(lArguments);
  }

  @Test
  public void restart() throws Exception {
    String[] lArguments = new String[6];

    lArguments[0] = Main.BASIC_BLOCK_2_COVERAGE;
    lArguments[1] = "test/programs/fql/locks/test_locks_15.c";
    lArguments[2] = "main";
    lArguments[3] = "--withoutCilPreprocessing";
    lArguments[4] = "--restart";
    lArguments[5] = "--restart-bound=3000000000";

    Main.run(lArguments);
  }

  @Test
  public void multiprocess() throws Exception {
    String[] lArguments = new String[6];

    lArguments[0] = Main.BASIC_BLOCK_2_COVERAGE;
    lArguments[1] = "test/programs/fql/locks/test_locks_15.c";
    lArguments[2] = "main";
    lArguments[3] = "2";

    MultiprocessFShell3.main(lArguments);
  }

}
