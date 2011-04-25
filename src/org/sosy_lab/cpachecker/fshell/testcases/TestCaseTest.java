package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

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

}
