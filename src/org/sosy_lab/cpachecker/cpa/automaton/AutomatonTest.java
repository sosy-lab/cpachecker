/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.StringHandler;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;

import com.google.common.collect.ImmutableMap;

public class AutomatonTest {
  private static final String OUTPUT_FILE = "output/AutomatonExport.dot";

  // Specification Tests
  @Test
  public void CyclicInclusionTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.pointer.PointerCPA, cpa.uninitvars.UninitializedVariablesCPA, cpa.types.TypesCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "log.consoleLevel",               "INFO",
        "analysis.stopAfterError",        "FALSE"
      );

      File tmpSpc = new File("test/config/automata/tmpSpecification.spc");
      String content = "#include UninitializedVariablesTestAutomaton.txt \n" +
      "#include tmpSpecification.spc \n";
      Files.writeFile(tmpSpc, content);
      TestResults results = run(prop, "test/programs/simple/UninitVarsErrors.c");
      Assert.assertTrue(results.isSafe());
      Assert.assertTrue(results.logContains("File \"test/config/automata/tmpSpecification.spc\" was referenced multiple times."));
      Assert.assertTrue("Could not delete temporary specification",tmpSpc.delete());
  }
  @Test
  public void IncludeSpecificationTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.pointer.PointerCPA, cpa.uninitvars.UninitializedVariablesCPA, cpa.types.TypesCPA",
        "specification",     "test/config/automata/defaultSpecificationForTesting.spc",
        "log.consoleLevel",               "INFO",
        "analysis.stopAfterError",        "FALSE"
      );

      TestResults results = run(prop, "test/programs/simple/UninitVarsErrors.c");
      Assert.assertTrue(results.logContains("Automaton: Uninitialized return value"));
      Assert.assertTrue(results.logContains("Automaton: Uninitialized variable used"));

      results = run(prop, "test/programs/simple/PointerAnalysisErrors.c");
      Assert.assertTrue(results.logContains("Found a DOUBLE_FREE"));
      Assert.assertTrue(results.logContains("Found an INVALID_FREE"));
      Assert.assertTrue(results.logContains("Found a POTENTIALLY_UNSAFE_DEREFERENCE"));
      Assert.assertTrue(results.logContains("Found a Memory Leak"));
      Assert.assertTrue(results.logContains("Found an UNSAFE_DEREFERENCE"));
  }
  @Test
  public void SpecificationAndNoCompositeTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "cpa", "cpa.location.LocationCPA",
        "log.consoleLevel", "INFO",
        "specification", "test/config/automata/LockingAutomatonAll.txt");

      TestResults results = run(prop, "test/programs/simple/modificationExample.c");
      Assert.assertTrue(results.logContains("Option specification gave specification automata, but no CompositeCPA was used"));
      Assert.assertTrue(results.getCheckerResult().getResult().equals(CPAcheckerResult.Result.UNKNOWN));
  }
  @Test
  public void modificationTestWithSpecification() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.explicit.ExplicitCPA",
        "specification",     "test/config/automata/modifyingAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "cpa.explicit.threshold",       "10");

      TestResults results = run(prop, "test/programs/simple/modificationExample.c");
      Assert.assertTrue(results.logContains("MODIFIED"));
      Assert.assertTrue(results.logContains("Modification successful"));
      Assert.assertTrue(results.isSafe());
  }

  //Automaton Tests
  @Test
  public void MatchEndOfProgramTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA",
        "specification",     "test/config/automata/PrintLastStatementAutomaton.spc",
        "log.consoleLevel",               "INFO",
        "analysis.stopAfterError",        "TRUE"
      );

      TestResults results = run(prop, "test/programs/simple/loop1.c");
      Assert.assertTrue(results.logContains("Last statement is \"return (0);\""));
      Assert.assertTrue(results.logContains("Last statement is \"return (-1);\""));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void failIfNoAutomatonGiven() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.explicit.ExplicitCPA, cpa.automaton.ControlAutomatonCPA",
        "log.consoleLevel",               "INFO",
        "cpa.explicit.threshold",       "10");

      TestResults results = run(prop, "test/programs/simple/modificationExample.c");
      Assert.assertTrue(results.getCheckerResult().getResult().equals(CPAcheckerResult.Result.UNKNOWN));
      Assert.assertTrue(results.logContains("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!"));
  }

  @Test
  public void modificationTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.explicit.ExplicitCPA, cpa.automaton.ControlAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/modifyingAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "cpa.explicit.threshold",       "10");

      TestResults results = run(prop, "test/programs/simple/modificationExample.c");
      Assert.assertTrue(results.logContains("MODIFIED"));
      Assert.assertTrue(results.logContains("Modification successful"));
      Assert.assertTrue(results.isSafe());
  }

  @Test
  public void modification_in_Observer_throws_Test() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.explicit.ExplicitCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/modifyingAutomaton.txt",
        "log.consoleLevel",               "SEVERE",
        "cpa.explicit.threshold",       "10"
      );

      TestResults results = run(prop, "test/programs/simple/modificationExample.c");
      // check for stack trace
      Assert.assertTrue(results.logContains("Invalid configuration: The Transition MATCH "));
  }

  @Test
  public void setuidTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/simple_setuid.txt",
        "log.consoleLevel",               "INFO",
        "analysis.stopAfterError",        "FALSE"
      );


      TestResults results = run(prop, "test/programs/simple/simple_setuid_test.c");
      Assert.assertTrue(results.logContains("Systemcall in line 10 with userid 2"));
      Assert.assertTrue(results.logContains("going to ErrorState on edge \"system(40);\""));
      Assert.assertTrue(results.isUnsafe());
  }
  @Test
  public void uninitVarsTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.uninitvars.UninitializedVariablesCPA, cpa.types.TypesCPA",
        "cpa.automaton.inputFile",     "test/config/automata/UninitializedVariablesTestAutomaton.txt",
        "log.consoleLevel",               "FINER",
        "cpa.automaton.dotExportFile", OUTPUT_FILE,
        "analysis.stopAfterError",        "FALSE"
      );

      TestResults results = run(prop, "test/programs/simple/UninitVarsErrors.c");
      Assert.assertTrue(results.logContains("Automaton: Uninitialized return value"));
      Assert.assertTrue(results.logContains("Automaton: Uninitialized variable used"));
  }
  @Test
  public void pointerAnalyisTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.pointer.PointerCPA",
        "cpa.automaton.inputFile",     "test/config/automata/PointerAnalysisTestAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "cpa.automaton.dotExportFile", OUTPUT_FILE,
        "analysis.stopAfterError",        "FALSE"
      );

      TestResults results = run(prop, "test/programs/simple/PointerAnalysisErrors.c");
      Assert.assertTrue(results.logContains("Found a DOUBLE_FREE"));
      Assert.assertTrue(results.logContains("Found an INVALID_FREE"));
      Assert.assertTrue(results.logContains("Found a POTENTIALLY_UNSAFE_DEREFERENCE"));
      Assert.assertTrue(results.logContains("Found a Memory Leak"));
      Assert.assertTrue(results.logContains("Found an UNSAFE_DEREFERENCE"));
  }
  @Test
  public void pointerAnalyisSkeletonTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.pointer.PointerCPA",
        "cpa.automaton.inputFile",     "test/config/automata/PointerAnalysisTestSkeletonAutomaton.txt",
        "log.consoleLevel",               "INFO"
      );

      TestResults results = run(prop, "test/programs/simple/PointerAnalysisErrors.c");
      Assert.assertTrue(results.logContains("Automaton going to ErrorState on edge \"free(a);\""));
      Assert.assertTrue(results.isUnsafe());
  }

  @Test
  public void locking_correct() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/LockingAutomatonAll.txt",
        "log.consoleLevel",               "INFO",
        "cpa.automaton.dotExportFile", OUTPUT_FILE
      );

      TestResults results = run(prop, "test/programs/simple/locking_correct.c");
      Assert.assertTrue(results.isSafe());
  }

  @Test
  public void locking_incorrect() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/LockingAutomatonAll.txt",
        "log.consoleLevel",               "INFO"
      );

      TestResults results = run(prop, "test/programs/simple/locking_incorrect.c");
      Assert.assertTrue(results.isUnsafe());
  }

  @Test
  public void explicitAnalysis_observing() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.explicit.ExplicitCPA",
        "cpa.automaton.inputFile",     "test/config/automata/ExcplicitAnalysisObservingAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "cpa.explicit.threshold" , "2000"
      );

      TestResults results = run(prop, "test/programs/simple/ex2.cil.c");
      Assert.assertTrue(results.logContains("st==3 after Edge st = 3;"));
      Assert.assertTrue(results.logContains("st==1 after Edge st = 1;"));
      Assert.assertTrue(results.logContains("st==2 after Edge st = 2;"));
      Assert.assertTrue(results.logContains("st==4 after Edge st = 4;"));
      Assert.assertTrue(results.isSafe());
  }

  @Test
  public void functionIdentifying() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/FunctionIdentifyingAutomaton.txt",
        "log.consoleLevel",               "FINER"
      );

      TestResults results = run(prop, "test/programs/simple/functionCall.c");
      Assert.assertTrue(results.logContains("i'm in Main after Edge int y;"));
      Assert.assertTrue(results.logContains("i'm in f after Edge y = f()"));
      Assert.assertTrue(results.logContains("i'm in f after Edge int x;"));
      Assert.assertTrue(results.logContains("i'm in Main after Edge Return Edge to"));
      Assert.assertTrue(results.logContains("i'm in Main after Edge Label: ERROR"));
  }

  @Test
  public void transitionVariableReplacement() throws Exception {
    Map<String, AutomatonVariable> pAutomatonVariables = null;
    List<AbstractElement> pAbstractElements = null;
    CFAEdge pCfaEdge = null;

    LogManager pLogger;

      pLogger = new LogManager(Configuration.builder()
                                            .setOption("log.level", "OFF")
                                            .setOption("log.consoleLevel", "WARNING")
                                            .build());

    AutomatonExpressionArguments args = new AutomatonExpressionArguments(pAutomatonVariables, pAbstractElements, pCfaEdge, pLogger);
    args.putTransitionVariable(1, "hi");
    args.putTransitionVariable(2, "hello");
    // actual test
    String result = args.replaceVariables("$1 == $2");
    Assert.assertTrue("hi == hello".equals(result));
    result = args.replaceVariables("$1 == $1");
    Assert.assertTrue("hi == hi".equals(result));

    pLogger.log(Level.WARNING, "Warning expected in the next line (concerning $5)");
    result = args.replaceVariables("$1 == $5");
    Assert.assertTrue(result == null); // $5 has not been found
    // this test should issue a log message!
  }
  /*
  @Test
  public void testJokerReplacementInPattern() {
    // tests the replacement of Joker expressions in the AST comparison
    String result = AutomatonASTComparator.replaceJokersInPattern("$20 = $?");
    Assert.assertTrue(result.contains("CPAChecker_AutomatonAnalysis_JokerExpression_Num20  =  CPAChecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $?");
    Assert.assertTrue(result.contains("CPAChecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAChecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$? = $?");
    Assert.assertTrue(result.contains("CPAChecker_AutomatonAnalysis_JokerExpression  =  CPAChecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $5");
    Assert.assertTrue(result.contains("CPAChecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAChecker_AutomatonAnalysis_JokerExpression_Num5 "));
  }*/
  @Test
  public void testJokerReplacementInAST() throws InvalidAutomatonException {
    // tests the replacement of Joker expressions in the AST comparison
    ASTMatcher patternAST = AutomatonASTComparator.generatePatternAST("$20 = $5($1, $?);");
    IASTNode sourceAST  = AutomatonASTComparator.generateSourceAST("var1 = function(var2, egal);");
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null);

    boolean result = patternAST.matches(sourceAST, args);
    Assert.assertTrue(result);
    Assert.assertTrue(args.getTransitionVariable(20).equals("var1"));
    Assert.assertTrue(args.getTransitionVariable(1).equals("var2"));
    Assert.assertTrue(args.getTransitionVariable(5).equals("function"));
  }

  @Test
  public void interacting_Automata() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA automatonA, cpa.automaton.ObserverAutomatonCPA automatonB, cpa.explicit.ExplicitCPA",
        "automatonA.cpa.automaton.inputFile",     "test/config/automata/InteractionAutomatonA.txt",
        "automatonB.cpa.automaton.inputFile",     "test/config/automata/InteractionAutomatonB.txt",
        "log.consoleLevel", "INFO",
        "cpa.explicit.threshold" , "2000"
      );

      TestResults results = run(prop, "test/programs/simple/loop1.c");
      Assert.assertTrue(results.logContains("A: Matched i in line 9 x=2"));
      Assert.assertTrue(results.logContains("B: A increased to 2 And i followed "));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void AST_Comparison() throws InvalidAutomatonException {
    Assert.assertTrue(testAST("x=5;", "x= $?;"));
    Assert.assertFalse(testAST("x=5;", "x= 10;"));
    //AutomatonASTComparator.printAST("x=10;");
    Assert.assertFalse(testAST("x=5;", "$? =10;"));
    Assert.assertTrue(testAST("x  = 5;", "$?=$?;"));

    Assert.assertFalse(testAST("a = 5;", "b    = 5;"));

    Assert.assertFalse(testAST("init();", "init($1);"));

    Assert.assertTrue(testAST("init(a, b);", "init($?, b);"));
    Assert.assertFalse(testAST("init(a, b);", "init($?, c);"));

    Assert.assertTrue(testAST("x = 5;", "x=$?"));
    Assert.assertTrue(testAST("x = 5", "x=$?;"));


    Assert.assertTrue(testAST("f();", "f($?);"));
    Assert.assertTrue(testAST("f(x);", "f($?);"));
    Assert.assertTrue(testAST("f(x, y);", "f($?);"));

    Assert.assertFalse(testAST("f(x);", "f(x, $?);"));
    Assert.assertTrue(testAST("f(x, y);", "f(x, $?);"));
    Assert.assertFalse(testAST("f(x, y, z);", "f(x, $?);"));

    /* in the automata this is
     * not possible at the moment, because the generated pattern
     * AST has one node that is missing in the the sub-AST of the CFA
     */
//    Assert.assertTrue(testAST("int y;", "int $?;"));
//    Assert.assertTrue(testAST("int y;", "int y;"));

  }
  private boolean testAST(String src, String pattern) throws InvalidAutomatonException {
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null);
    IASTNode sourceAST;
    sourceAST = AutomatonASTComparator.generateSourceAST(src);
    ASTMatcher patternAST = AutomatonASTComparator.generatePatternAST(pattern);
    return patternAST.matches(sourceAST, args);
  }


  private TestResults run(Map<String, String> pProperties, String pSourceCodeFilePath) throws Exception {
    Configuration config = Configuration.builder().setOptions(pProperties).build();
    StringHandler stringLogHandler = new LogManager.StringHandler();
    LogManager logger = new LogManager(config, stringLogHandler);
    CPAchecker cpaChecker = new CPAchecker(config, logger);
    CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
    return new TestResults(stringLogHandler.getLog(), results);
  }

  private static class TestResults {
    private String log;
    private CPAcheckerResult checkerResult;
    public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
      super();
      log = pLog;
      checkerResult = pCheckerResult;
    }
    @SuppressWarnings("unused")
    public String getLog() {
      return log;
    }
    public CPAcheckerResult getCheckerResult() {
      return checkerResult;
    }
    boolean logContains(String pattern) {
     return log.contains(pattern);
    }
    boolean isSafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.SAFE);
    }
    boolean isUnsafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.UNSAFE);
    }
    @Override
    public String toString() {
      return log;
    }
  }
}
