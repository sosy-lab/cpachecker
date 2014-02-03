/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.AccessToUninitializedVariableException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult.Factory;
import org.sosy_lab.cpachecker.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.goals.Goal;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.tiger.testcases.BuggyExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseInputsTestCase;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;


public class TestCaseUtil {

  private Wrapper mWrapper;
  private PrintStream mOutput;
  private Map<TestCase, CFAEdge[]> mGeneratedTestCases;
  private FeasibilityInformation mFeasibilityInformation;

  private LocationCPA mLocationCPA;
  private CallstackCPA mCallStackCPA;
  private CFAPathCPA mCFAPathCPA;
  private AssumeCPA mAssumeCPA;

  private LogManager mLogManager;
  private Configuration mConfiguration;
  private ShutdownNotifier mShutdownNotifier;

  private boolean mStopOnImpreciseExecution;

  public TestCaseUtil(
      Wrapper pWrapper,
      PrintStream pOutput,
      Map<TestCase, CFAEdge[]> pGeneratedTestCases,

      LocationCPA pLocationCPA,
      CallstackCPA pCallstackCPA,
      CFAPathCPA pCFAPathCPA,
      AssumeCPA pAssumeCPA,

      LogManager pLogManager,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier,

      boolean pStopOnImpreciseExecution) {

    mWrapper = pWrapper;
    mOutput = pOutput;
    mGeneratedTestCases = pGeneratedTestCases;

    mLocationCPA = pLocationCPA;
    mCallStackCPA = pCallstackCPA;
    mCFAPathCPA = pCFAPathCPA;
    mAssumeCPA = pAssumeCPA;

    mLogManager = pLogManager;
    mConfiguration = pConfiguration;
    mShutdownNotifier = pShutdownNotifier;

    mStopOnImpreciseExecution = pStopOnImpreciseExecution;
  }

  public void setFeasibilityInformation(FeasibilityInformation pFeasibilityInformation) {
    assert(pFeasibilityInformation != null);
    assert(mFeasibilityInformation == null);

    mFeasibilityInformation = pFeasibilityInformation;
  }

  public void reconstructPathFromImpreciseTestCase(TestCase pTestCase, int pIndex, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Factory pResultFactory, Goal pGoal, Prediction[] pGoalPrediction) {
    assert (!pTestCase.isPrecise());
    assert (pTestCase instanceof ImpreciseInputsTestCase);

    TestCase lPreciseTestCase = ((ImpreciseInputsTestCase)pTestCase).toPreciseTestCase();

    try {
      reconstructPathFromPreciseTestCase(lPreciseTestCase, pIndex, pAutomatonCPA, pPassingCPA, pResultFactory, pGoal, pGoalPrediction);
      throw new RuntimeException("fix");
    } catch (MissingInputException e) {
      mOutput.println("Goal #" + pIndex + " is imprecise!");
      updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
    } catch (BuggyExecutionException e) {
      mOutput.println("Goal #" + pIndex + " reveals a bug!");
      updateBuggyTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
    }
  }

  public void enumerate(int[] pIntegerValues, double[] pDoubleValues, int pIndex, Collection<int[]> pTestCases) {
    for (int i = pIndex; i < pIntegerValues.length; i++) {
      if (pIntegerValues[i] != pDoubleValues[i]) {
        double lAbsValue = Math.abs(pDoubleValues[i]);

        // split
        int lValue1 = (int)lAbsValue;
        int lValue2 = lValue1 + 1;

        if (pDoubleValues[i] < 0) {
          lValue1 *= -1;
          lValue2 *= -1;
        }

        // we have to store the values ...
        if (lValue1 == pIntegerValues[i]) {
          // make a copy and store lValue2 in the copy
          int[] lCopy = new int[pIntegerValues.length];

          System.arraycopy(pIntegerValues, 0, lCopy, 0, pIntegerValues.length);

          lCopy[i] = lValue2;

          pTestCases.add(lCopy);

          // enumerate copy
          enumerate(lCopy, pDoubleValues, i + 1, pTestCases);
        }
        else if (lValue2 == pIntegerValues[i]) {
          // make a copy and store lValue1 in the copy
          int[] lCopy = new int[pIntegerValues.length];

          System.arraycopy(pIntegerValues, 0, lCopy, 0, pIntegerValues.length);

          lCopy[i] = lValue1;

          pTestCases.add(lCopy);

          // enumerate copy
          enumerate(lCopy, pDoubleValues, i + 1, pTestCases);
        }
        else {
          throw new RuntimeException();
        }
      }
    }
  }

  public void reconstructPathFromImpreciseTestCase2(TestCase pTestCase, int pIndex, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Factory pResultFactory, Goal pGoal, Prediction[] pGoalPrediction) {
    assert (!pTestCase.isPrecise());
    assert (pTestCase instanceof ImpreciseInputsTestCase);

    /*ImpreciseInputsTestCase lTestCase = (ImpreciseInputsTestCase)pTestCase;

    int[] lInputValues = lTestCase.getInputs();
    double[] lExactInputValues = lTestCase.getExactInputs();

    int lNumberOfRoundings = 0;

    for (int i = 0; i < lInputValues.length; i++) {
      if (lInputValues[i] != lExactInputValues[i]) {
        lNumberOfRoundings++;
      }
    }

    if (lNumberOfRoundings < 4) {
      // we will try to enumerate everything
      ArrayList<int[]> lTestCases = new ArrayList<>(8); // TODO we potentially waste space
      enumerate(lInputValues, lExactInputValues, 0, lTestCases);

      boolean lSuccess = false;

      for (int[] lInputs : lTestCases) {
        TestCase lTmpTestCase = new PreciseInputsTestCase(lInputs);

        try {
          CFAEdge[] lCFAPath = reconstructPathFromPreciseTestCase(lTmpTestCase, pIndex, pAutomatonCPA, pPassingCPA, pResultFactory, pGoal, pGoalPrediction);

          if (lCFAPath != null) {
            mOutput.println("Goal #" + pIndex + " is feasible!");
            updatePreciseTestCaseStatistics(lTmpTestCase, pIndex, pGoal, lCFAPath, pResultFactory, pGoalPrediction);

            lSuccess = true;

            break;
          }
        } catch (MissingInputException e) {
          mOutput.println("Missprediction!");
        }
      }

      if (!lSuccess) {
        mOutput.println("Goal #" + pIndex + " lead to an imprecise execution!");
        updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
      }
    }
    else {*/
      // just one shot (maybe we should enumerate a subset?)
      TestCase lPreciseTestCase = ((ImpreciseInputsTestCase)pTestCase).toPreciseTestCase();
      //TestCase lPreciseTestCase = new PreciseInputsTestCase(((ImpreciseInputsTestCase)pTestCase).getInputs());

      try {
        CFAEdge[] lCFAPath = reconstructPathFromPreciseTestCase(lPreciseTestCase, pIndex, pAutomatonCPA, pPassingCPA, pResultFactory, pGoal, pGoalPrediction);

        if (lCFAPath != null) {
          mOutput.println("Goal #" + pIndex + " is feasible!");
          updatePreciseTestCaseStatistics(lPreciseTestCase, pIndex, pGoal, lCFAPath, pResultFactory, pGoalPrediction);
        }
        else {
          mOutput.println("Goal #" + pIndex + " lead to an imprecise execution!");
          updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);

          if (mStopOnImpreciseExecution) {
            mOutput.println("Goal:");
            mOutput.println(pGoal.getAutomaton());
            mOutput.println();
            mOutput.println(pTestCase);
            mOutput.println();

            throw new RuntimeException("Imprecise simulation!");
          }
        }

      } catch (MissingInputException e) {
        mOutput.println("Goal #" + pIndex + " is imprecise!");
        updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
      }
      catch (BuggyExecutionException e) {
        mOutput.println("Goal #" + pIndex + " reveals a bug!");
        updateBuggyTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
      }
    //}
  }

  public CFAEdge[] reconstructPathFromPreciseTestCase(TestCase pTestCase, int pIndex, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Factory pResultFactory, Goal pGoal, Prediction[] pGoalPrediction) throws MissingInputException, BuggyExecutionException {
    assert (pTestCase.isPrecise());

    try {
      return reconstructPath(mWrapper.getCFA(), pTestCase, mWrapper.getEntry(), pAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
    } catch (MissingInputException e) {
      throw e;
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    } catch (ImpreciseExecutionException e) {

    }

    return null;
  }

  public void reconstructPath(TestCase pTestCase, int pIndex, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Factory pResultFactory, Goal pGoal, Prediction[] pGoalPrediction) {
    if (pTestCase.isPrecise()) {
      try {
        CFAEdge[] lCFAPath = reconstructPathFromPreciseTestCase(pTestCase, pIndex, pAutomatonCPA, pPassingCPA, pResultFactory, pGoal, pGoalPrediction);

        if (lCFAPath != null) {
          mOutput.println("Goal #" + pIndex + " is feasible!");
          updatePreciseTestCaseStatistics(pTestCase, pIndex, pGoal, lCFAPath, pResultFactory, pGoalPrediction);
        }
        else {
          mOutput.println("Goal #" + pIndex + " lead to an imprecise execution!");
          updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);

          if (mStopOnImpreciseExecution) {
            mOutput.println("Goal:");
            mOutput.println(pGoal.getAutomaton());
            mOutput.println();
            mOutput.println(pTestCase);
            mOutput.println();

            throw new RuntimeException("Imprecise simulation!");
          }
        }

      } catch (MissingInputException e1) {
        throw new RuntimeException(e1);
      } catch (BuggyExecutionException e) {
        mOutput.println("Goal #" + pIndex + " reveals a bug!");
        updateBuggyTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
      }
    }
    else {
      reconstructPathFromImpreciseTestCase2(pTestCase, pIndex, pAutomatonCPA, pPassingCPA, pResultFactory, pGoal, pGoalPrediction);
    }
  }

  private void updatePreciseTestCaseStatistics(TestCase pPreciseTestCase, int pIndex, Goal pGoal, CFAEdge[] lCFAPath, Factory pResultFactory, Prediction[] pGoalPrediction) {
    pResultFactory.addFeasibleTestCase(pGoal.getPattern(), pPreciseTestCase);

    // we only add precise test cases for coverage analysis
    mGeneratedTestCases.put(pPreciseTestCase, lCFAPath);

    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);

    assert (pGoalPrediction[pIndex - 1].equals(InfeasibilityPropagation.Prediction.UNKNOWN));
  }

  private void updateImpreciseTestCaseStatistics(TestCase pImpreciseTestCase, int pIndex, Factory pResultFactory, Prediction[] pGoalPrediction) {
    pResultFactory.addImpreciseTestCase(pImpreciseTestCase);

    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

    assert (pGoalPrediction[pIndex - 1].equals(InfeasibilityPropagation.Prediction.UNKNOWN));
  }

  private void updateBuggyTestCaseStatistics(TestCase pBuggyTestCase, int pIndex, Factory pResultFactory, Prediction[] pGoalPrediction) {
    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.BUGGY);

    assert (pGoalPrediction[pIndex - 1].equals(InfeasibilityPropagation.Prediction.UNKNOWN));
  }

  /**
   * Update statistics, when a spurious traces was reported.
   * @param pIndex
   * @param pResultFactory
   * @param pGoalPrediction
   */
  public void updateImpreciseTestCaseStatistics( int pIndex, Factory pResultFactory, Prediction[] pGoalPrediction) {

    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

    InfeasibilityPropagation.Prediction lCurrentPrediction = pGoalPrediction[pIndex - 1];

    if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
      throw new RuntimeException("missmatching prediction");
    }
  }


  private CFAEdge[] reconstructPath(CFA pCFA, TestCase pTestCase, FunctionEntryNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException, BuggyExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(pPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(pCoverAutomatonCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);

    // CFA path CPA
    int lCFAPathCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mCFAPathCPA);

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    lCPAFactory.set(pCFA, CFA.class);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    ShutdownNotifier notifier = ShutdownNotifier.create();
    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager, mConfiguration, notifier);

    AbstractState lInitialElement = lCPA.getInitialState(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (MissingInputException e) {
      throw e;
    } catch (AccessToUninitializedVariableException e) {
      throw new BuggyExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeState lEndNode = (CompositeState)lReachedSet.getLastState();

    if (lEndNode == null) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    if (!((LocationState)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    AbstractState lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

    if (!(lProductAutomatonElement instanceof Targetable)) {
      throw new RuntimeException();
    }

    Targetable lTargetable = (Targetable)lProductAutomatonElement;

    if (!lTargetable.isTarget()) {
      throw new RuntimeException();
    }

    CFAPathStandardState lPathElement = (CFAPathStandardState)lEndNode.get(lCFAPathCPAIndex);

    return lPathElement.toArray();
  }

  private FQLSpecification lIdStarFQLSpecification = null;

  public void seed(Iterable<TestCase> pTestSuite, CoverageSpecificationTranslator pCoverageSpecificationTranslator, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException, BuggyExecutionException {
    if (lIdStarFQLSpecification == null) {
      try {
        lIdStarFQLSpecification = FQLSpecification.parse("COVER \"EDGES(ID)*\" PASSING EDGES(ID)*");
      } catch (Exception e1) {
        throw new RuntimeException(e1);
      }
    }

    ElementaryCoveragePattern lIdStarPattern = pCoverageSpecificationTranslator.mPathPatternTranslator.translate(lIdStarFQLSpecification.getPathPattern());
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lIdStarPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    GuardedEdgeAutomatonCPA lIdStarCPA = new GuardedEdgeAutomatonCPA(lAutomaton);

    for (TestCase lTestCase : pTestSuite) {
      CFAEdge[] lPath = reconstructPath(mWrapper.getCFA(), lTestCase, mWrapper.getEntry(), lIdStarCPA, null, mWrapper.getOmegaEdge().getSuccessor());

      mGeneratedTestCases.put(lTestCase, lPath);
    }
  }

}
