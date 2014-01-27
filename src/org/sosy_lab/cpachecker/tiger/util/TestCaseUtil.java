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
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult.Factory;
import org.sosy_lab.cpachecker.tiger.goals.Goal;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseInputsTestCase;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;


public class TestCaseUtil {

  private Wrapper mWrapper;
  private PrintStream mOutput;
  private Map<TestCase, CFAEdge[]> mGeneratedTestCases;
  private FeasibilityInformation mFeasibilityInformation;
  //private Prediction[] mGoalPrediction;

  private LocationCPA mLocationCPA;
  private CallstackCPA mCallStackCPA;
  private CFAPathCPA mCFAPathCPA;
  private AssumeCPA mAssumeCPA;

  private LogManager mLogManager;
  private Configuration mConfiguration;
  private ShutdownNotifier mShutdownNotifier;

  public TestCaseUtil(
      Wrapper pWrapper,
      PrintStream pOutput,
      Map<TestCase, CFAEdge[]> pGeneratedTestCases,
      //FeasibilityInformation pFeasibilityInformation,
      //Prediction[] pGoalPrediction,

      LocationCPA pLocationCPA,
      CallstackCPA pCallstackCPA,
      CFAPathCPA pCFAPathCPA,
      AssumeCPA pAssumeCPA,

      LogManager pLogManager,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier) {

    mWrapper = pWrapper;
    mOutput = pOutput;
    mGeneratedTestCases = pGeneratedTestCases;
    //mFeasibilityInformation = pFeasibilityInformation;
    //mGoalPrediction = pGoalPrediction;

    mLocationCPA = pLocationCPA;
    mCallStackCPA = pCallstackCPA;
    mCFAPathCPA = pCFAPathCPA;
    mAssumeCPA = pAssumeCPA;

    mLogManager = pLogManager;
    mConfiguration = pConfiguration;
    mShutdownNotifier = pShutdownNotifier;

  }

  public void setOutput(PrintStream pOutput) {
    mOutput = pOutput;
  }

  public void setFeasibilityInformation(FeasibilityInformation pFeasibilityInformation) {
    assert(pFeasibilityInformation != null);
    assert(mFeasibilityInformation == null);

    mFeasibilityInformation = pFeasibilityInformation;
  }

  public void reconstructPath(TestCase pTestCase, int pIndex, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Factory pResultFactory, Goal pGoal, Prediction[] pGoalPrediction) {

    // TODO is this useful?
    if (!pTestCase.isPrecise()) {
      // derive a precise test case
      pTestCase = ((ImpreciseInputsTestCase)pTestCase).toPreciseTestCase();
    }

    if (pTestCase.isPrecise()) {
      CFAEdge[] lCFAPath = null;

      boolean lIsPrecise = true;

      try {
        lCFAPath = reconstructPath(mWrapper.getCFA(), pTestCase, mWrapper.getEntry(), pAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
      } catch (InvalidConfigurationException | CPAException e) {
        throw new RuntimeException(e);
      } catch (ImpreciseExecutionException e) {
        lIsPrecise = false;
        pTestCase = e.getTestCase();
      }

      if (lIsPrecise) {
        mOutput.println("Goal #" + pIndex + " is feasible!");
        updatePreciseTestCaseStatistics(pTestCase, pIndex, pGoal, lCFAPath, pResultFactory, pGoalPrediction);
      }
      else {
        mOutput.println("Goal #" + pIndex + " lead to an imprecise execution!");
        updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
      }
    }
    else {
      mOutput.println("Goal #" + pIndex + " is imprecise!");
      updateImpreciseTestCaseStatistics(pTestCase, pIndex, pResultFactory, pGoalPrediction);
    }
  }

  private void updatePreciseTestCaseStatistics(TestCase pPreciseTestCase, int pIndex, Goal pGoal, CFAEdge[] lCFAPath, Factory pResultFactory, Prediction[] pGoalPrediction) {
    pResultFactory.addFeasibleTestCase(pGoal.getPattern(), pPreciseTestCase);

    // we only add precise test cases for coverage analysis
    mGeneratedTestCases.put(pPreciseTestCase, lCFAPath);

    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);

    InfeasibilityPropagation.Prediction lCurrentPrediction = pGoalPrediction[pIndex - 1];

    if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
      throw new RuntimeException("missmatching prediction");
    }
  }

  private void updateImpreciseTestCaseStatistics(TestCase pImpreciseTestCase, int pIndex, Factory pResultFactory, Prediction[] pGoalPrediction) {
    pResultFactory.addImpreciseTestCase(pImpreciseTestCase);

    mFeasibilityInformation.setStatus(pIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

    InfeasibilityPropagation.Prediction lCurrentPrediction = pGoalPrediction[pIndex - 1];

    if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
      throw new RuntimeException("missmatching prediction");
    }
  }

  private CFAEdge[] reconstructPath(CFA pCFA, TestCase pTestCase, FunctionEntryNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
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

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager, mConfiguration, mShutdownNotifier);

    AbstractState lInitialElement = lCPA.getInitialState(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
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

}
