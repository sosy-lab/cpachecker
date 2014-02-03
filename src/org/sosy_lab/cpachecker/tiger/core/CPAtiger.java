/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.tiger.core.interfaces.FQLCoverageAnalyser;
import org.sosy_lab.cpachecker.tiger.core.interfaces.FQLTestGenerator;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.testcases.BuggyExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.LoggingTestSuite;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;
import org.sosy_lab.cpachecker.tiger.testcases.TestSuite;
import org.sosy_lab.cpachecker.tiger.testgen.IncrementalARTReusingFQLTestGenerator;
import org.sosy_lab.cpachecker.tiger.util.FeasibilityInformation;
import org.sosy_lab.cpachecker.tiger.util.LoggingFeasibilityInformation;
import org.sosy_lab.cpachecker.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

import com.google.common.base.Joiner;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 */

public class CPAtiger implements FQLTestGenerator, FQLCoverageAnalyser {

  /*private final NonincrementalFQLTestGenerator mNonincrementalTestGenerator;
  private final IncrementalFQLTestGenerator mIncrementalTestGenerator;
  private final IncrementalAndAlternatingFQLTestGenerator mIncrementalAndAlternatingTestGenerator;
  private final StandardFQLCoverageAnalyser mCoverageAnalyser;*/

  private final IncrementalARTReusingFQLTestGenerator mIncrementalARTReusingTestGenerator;

  private String mFeasibilityInformationOutputFile = null;
  private String mFeasibilityInformationInputFile = null;
  private String mTestSuiteOutputFile = null;
  private int mMinIndex = 0;
  private int mMaxIndex = Integer.MAX_VALUE;
  private boolean mDoLogging = false;
  private boolean mDoAppendingLogging = false;
  private long mRestartBound = 100000000; // 100 MB
  private PrintStream mOutput;
  // type of analysis
  private AnalysisType aType = AnalysisType.PREDICATE;

  private ShutdownNotifier shutdownNotifier;

  public CPAtiger(String pSourceFileName, String pEntryFunction, ShutdownNotifier pShutdownNotifier, PrintStream pOutput, AnalysisType pAType, long pTimelimit, boolean pStopOnImpreciseExecution) {
    mOutput = pOutput;
    aType = pAType;
    shutdownNotifier = pShutdownNotifier;
    mIncrementalARTReusingTestGenerator = new IncrementalARTReusingFQLTestGenerator(pSourceFileName, pEntryFunction, shutdownNotifier, pOutput, aType, pTimelimit, pStopOnImpreciseExecution);
  }

  public void doRestart() {
  }

  public void setRestartBound(long pRestartBound) {
    mRestartBound = pRestartBound;
  }

  public void setFeasibilityInformationOutputFile(String pFile) {
    mFeasibilityInformationOutputFile = pFile;
  }

  public void setFeasibilityInformationInputFile(String pFile) {
    mFeasibilityInformationInputFile = pFile;
  }

  public void setTestSuiteOutputFile(String pFile) {
    mTestSuiteOutputFile = pFile;
  }

  public void setGoalIndices(int pMinIndex, int pMaxIndex) {
    setMinIndex(pMinIndex);
    setMaxIndex(pMaxIndex);
  }

  public void setMinIndex(int pIndex) {
    mMinIndex = pIndex;
  }

  public void setMaxIndex(int pIndex) {
    mMaxIndex = pIndex;
  }

  public void doLogging() {
    mDoLogging = true;
  }

  public void doAppendingLogging() {
    mDoAppendingLogging = true;
  }

  public void seed(Collection<TestCase> pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException, BuggyExecutionException {
    mIncrementalARTReusingTestGenerator.seed(pTestSuite);
  }

  public CPAtigerResult run(String pFQLSpecification) {
    return run(pFQLSpecification, true, false, false, false, true, false);
  }

  public CPAtigerResult run(String pFQLSpecification, int pGoalIndex) {
    mMinIndex = pGoalIndex;
    mMaxIndex = pGoalIndex;
    return run(pFQLSpecification, true, false, false, false, true, false);
  }

  @Override
  public CPAtigerResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating) {
    mOutput.println(pFQLSpecification);

    /*if (pGenerateTestGoalAutomataInAdvance) {
      return mNonincrementalTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);
    }
    else {
      if (pAlternating) {
        return mIncrementalAndAlternatingTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);
      }
      else {
        // TODO make configurable
        if (!pAlternating) {
          */
          mIncrementalARTReusingTestGenerator.setGoalIndices(mMinIndex, mMaxIndex);

          FeasibilityInformation lFeasibilityInformation;
          TestSuite lTestSuite;

          if (mFeasibilityInformationInputFile != null) {
            try {
              lFeasibilityInformation = FeasibilityInformation.load(mFeasibilityInformationInputFile);

              if (!lFeasibilityInformation.hasTestsuiteFilename()) {
                throw new RuntimeException();
              }

              lTestSuite = TestSuite.load(lFeasibilityInformation.getTestsuiteFilename());

              mIncrementalARTReusingTestGenerator.setTestSuite(lTestSuite);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
          else {
            lFeasibilityInformation = new FeasibilityInformation();
            lTestSuite = new TestSuite();
          }

          if (mDoLogging) {
            if (mFeasibilityInformationOutputFile != null) {
              try {
                if (mTestSuiteOutputFile != null) {
                  lTestSuite = new LoggingTestSuite(lTestSuite, mTestSuiteOutputFile, mDoAppendingLogging);

                  lFeasibilityInformation.setTestsuiteFilename(mTestSuiteOutputFile);
                }
                else {
                  File lCWD = new java.io.File( "." );
                  File lTestSuiteFile = File.createTempFile("testsuite", ".tst", lCWD);
                  lTestSuite = new LoggingTestSuite(lTestSuite, lTestSuiteFile.getCanonicalPath(), mDoAppendingLogging);

                  lFeasibilityInformation.setTestsuiteFilename(lTestSuiteFile.getCanonicalPath());
                }

                lFeasibilityInformation = new LoggingFeasibilityInformation(lFeasibilityInformation, mFeasibilityInformationOutputFile, mDoAppendingLogging);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }

          mIncrementalARTReusingTestGenerator.setFeasibilityInformation(lFeasibilityInformation);
          try {
            mIncrementalARTReusingTestGenerator.setTestSuite(lTestSuite);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }

          /*if (mDoRestart) {
            mIncrementalARTReusingTestGenerator.doRestart();
            mIncrementalARTReusingTestGenerator.setRestartBound(mRestartBound);
          }*/

          CPAtigerResult lResult = mIncrementalARTReusingTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);

          if (mDoLogging) {
            ((LoggingTestSuite)lTestSuite).close();
            ((LoggingFeasibilityInformation)lFeasibilityInformation).close();
          }
          else {
            if (mFeasibilityInformationOutputFile != null) {
              try {
                if (mTestSuiteOutputFile != null) {
                  lTestSuite.write(mTestSuiteOutputFile);
                  lFeasibilityInformation.setTestsuiteFilename(mTestSuiteOutputFile);
                }
                else {
                  File lCWD = new java.io.File( "." );
                  File lTestSuiteFile = File.createTempFile("testsuite", ".tst", lCWD);
                  lTestSuite.write(lTestSuiteFile);
                  lFeasibilityInformation.setTestsuiteFilename(lTestSuiteFile.getCanonicalPath());
                }

                lFeasibilityInformation.write(mFeasibilityInformationOutputFile);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }

          return lResult;
        /*}
        else {
          return mIncrementalTestGenerator.run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pGenerateTestGoalAutomataInAdvance, pCheckCorrectnessOfCoverageCheck, pPedantic, pAlternating);
        }
      }
    }*/
  }

  @Override
  public void checkCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    //mCoverageAnalyser.checkCoverage(pFQLSpecification, pTestSuite, pPedantic);
  }

  public static CFA getCFA(String pSourceFileName, Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    CFACreator lCFACreator = new CFACreator(pConfiguration, pLogManager, ShutdownNotifier.create());

    CFA cfa = null;

    // parse code file
    try {
      cfa = lCFACreator.cpatiger_parseFileAndCreateCFA(Collections.singletonList(pSourceFileName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return cfa;
  }

  public static ThreeValuedAnswer accepts(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, CFAEdge[] pCFAPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(pAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pCFAPath) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (pAutomaton.getFinalStates().contains(lCurrentState)) {
          return ThreeValuedAnswer.ACCEPT;
        }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          }
          else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.clear();

      Set<NondeterministicFiniteAutomaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (pAutomaton.getFinalStates().contains(lCurrentState)) {
        return ThreeValuedAnswer.ACCEPT;
      }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    }
    else {
      return ThreeValuedAnswer.REJECT;
    }
  }

  public static Configuration createConfiguration(String pSourceFile, String pEntryFunction) throws InvalidConfigurationException {
    List<String> additionalOpts = new Vector<>();
    File lPropertiesFile = CPAtiger.createPropertiesFile(pEntryFunction, additionalOpts);
    return createConfiguration(Collections.singletonList(pSourceFile), lPropertiesFile.getAbsolutePath());
  }

  public static Configuration createConfiguration(String pSourceFile, String pEntryFunction, Collection<String> additionalOpts) throws InvalidConfigurationException {
    File lPropertiesFile = CPAtiger.createPropertiesFile(pEntryFunction, additionalOpts);
    return createConfiguration(Collections.singletonList(pSourceFile), lPropertiesFile.getAbsolutePath());
  }

  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) throws InvalidConfigurationException {
    Map<String, String> lCommandLineOptions = new HashMap<>();

    lCommandLineOptions.put("analysis.programNames", Joiner.on(", ").join(pSourceFiles));
    //lCommandLineOptions.put("output.path", "test/output");

    Configuration lConfiguration = null;
    try {
      lConfiguration = Configuration.builder()
                                    .loadFromFile(pPropertiesFile)
                                    .setOptions(lCommandLineOptions)
                                    .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return lConfiguration;
  }

  private static File createPropertiesFile(String pEntryFunction, Collection<String> pAdditionalOpts) {
    if (pEntryFunction == null) {
      throw new IllegalArgumentException("Parameter pEntryFunction is null!");
    }

    File lPropertiesFile = null;

    try {

      lPropertiesFile = File.createTempFile("fshell.", ".properties");
      lPropertiesFile.deleteOnExit();

      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");

      //lWriter.println("log.consoleLevel = ALL");
      // Logging information
      lWriter.println("log.level = OFF");
      lWriter.println("log.consoleLevel = OFF");
      lWriter.println("cfa.export = false");
      lWriter.println("cfa.exportPerFunction = false");
      lWriter.println("cfa.callgraph.export = false");

      lWriter.println("cpa.predicate.solver.useIntegers = true"); // we need exact input data.


      for (String opt : pAdditionalOpts){
        lWriter.println(opt);
      }

      lWriter.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return lPropertiesFile;
  }



  public enum AnalysisType {
    PREDICATE, EXPLICIT_SIMPLE, EXPLICIT_REF, EXPLICIT_PRED;
  }



}

