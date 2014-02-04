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
package org.sosy_lab.cpachecker.tiger.testgen;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.tiger.core.CPAtiger;
import org.sosy_lab.cpachecker.tiger.core.CPAtiger.AnalysisType;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult.Factory;
import org.sosy_lab.cpachecker.tiger.core.interfaces.FQLTestGenerator;
import org.sosy_lab.cpachecker.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.goals.Goal;
import org.sosy_lab.cpachecker.tiger.goals.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.tiger.testcases.BuggyExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;
import org.sosy_lab.cpachecker.tiger.testcases.TestSuite;
import org.sosy_lab.cpachecker.tiger.util.ARTReuse;
import org.sosy_lab.cpachecker.tiger.util.FeasibilityInformation;
import org.sosy_lab.cpachecker.tiger.util.FeasibilityInformation.FeasibilityStatus;
import org.sosy_lab.cpachecker.tiger.util.TestCaseUtil;
import org.sosy_lab.cpachecker.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 */

public class IncrementalARTReusingFQLTestGenerator implements FQLTestGenerator {

  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final Wrapper mWrapper;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private final LocationCPA mLocationCPA;
  private final CallstackCPA mCallStackCPA;
  private final AssumeCPA mAssumeCPA;
  private final CFAPathCPA mCFAPathCPA;

  private final TimeAccumulator mTimeInReach;
  private int mTimesInReach;
  private final GuardedEdgeLabel mAlphaLabel;
  private final GuardedEdgeLabel mOmegaLabel;
  private final GuardedEdgeLabel mInverseAlphaLabel;
  private final Map<TestCase, CFAEdge[]> mGeneratedTestCases;

  private int mMinIndex = 0;
  private int mMaxIndex = Integer.MAX_VALUE;

  private boolean mUseAutomatonOptimization = true;
  private boolean mUseGraphCPA = false; // TODO disabled it since it causes a bug when doing FQL queries with PASSING clause
  private boolean mReuseART = true;
  private boolean mUseInfeasibilityPropagation = true;

  private FeasibilityInformation mFeasibilityInformation;
  private TestSuite mTestSuite;

  public PrintStream mOutput = System.out;

  private ShutdownNotifier mShutdownNotifier;

  private TestCaseUtil mTestCaseUtil;

  // type of analysis
  private AnalysisType analysisType = AnalysisType.PREDICATE;
  // underlying analysis
  private AnalysisWithReuse analysis;
  // time limit for a single reachability analysis (0=no limit)
  private long timelimit;
  // maximum time for reachability analysis
  private long timeReachMax;

  private static IncrementalARTReusingFQLTestGenerator INSTANCE = null;

  private int lFeasibleTestGoalsTimeSlot = 0;
  private int lInfeasibleTestGoalsTimeSlot = 1;
  private int lImpreciseTestGoalsTimeSlot = 1;

  public void setAnalysisType(AnalysisType paType){
    analysisType = paType;
  }

  // TODO replace this by better code architecture
  public static IncrementalARTReusingFQLTestGenerator getInstance() {
    assert (INSTANCE != null);

    return INSTANCE;
  }

  public void setGoalIndices(int pMinIndex, int pMaxIndex) {
    mMinIndex = pMinIndex;
    mMaxIndex = pMaxIndex;
  }

  public void setFeasibilityInformation(FeasibilityInformation pFeasibilityInformation) {
    mFeasibilityInformation = pFeasibilityInformation;

    mTestCaseUtil.setFeasibilityInformation(mFeasibilityInformation);
  }

  public void setTestSuite(TestSuite pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException, BuggyExecutionException {
    mTestSuite = pTestSuite;
    seed(pTestSuite);
  }

  public void seed(Iterable<TestCase> pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException, BuggyExecutionException {
    mTestCaseUtil.seed(pTestSuite, mCoverageSpecificationTranslator, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
  }

  public IncrementalARTReusingFQLTestGenerator(String pSourceFileName, String pEntryFunction, ShutdownNotifier shutdownNotifier, PrintStream pOutput, AnalysisType pAType, long pTimelimit, boolean pStopOnImpreciseExecution) {
    assert (INSTANCE == null);

    analysisType = pAType;
    timelimit = pTimelimit;
    mOutput = pOutput;
    mTimesInReach = 0;
    mTimeInReach = new TimeAccumulator();

    INSTANCE = this;

    mShutdownNotifier = shutdownNotifier;

    CFA lCFA;

    try {
      mConfiguration = CPAtiger.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new BasicLogManager(mConfiguration);

      lCFA = CPAtiger.getCFA(pSourceFileName, mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    /*
     * We have to instantiate mCoverageSpecificationTranslator before the wrapper
     * changes the underlying CFA. FQL specifications are evaluated against the
     * target graph generated during initialization of mCoverageSpecificationTranslator.
     */
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lCFA.getFunctionHead(pEntryFunction));

    mWrapper = new Wrapper(lCFA, pEntryFunction);

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getOmegaEdge()));


    /*
     * Initialize shared CPAs.
     */
    // location CPA
    GlobalInfo.getInstance().storeCFA(lCFA);
    mLocationCPA = new LocationCPA(lCFA);


    // callstack CPA
    try {
      mCallStackCPA = new CallstackCPA(mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    // assume CPA
    mAssumeCPA = AssumeCPA.getCBMCAssume();

    // cfa path CPA
    mCFAPathCPA = CFAPathCPA.getInstance();

    // choose analysis
    switch (analysisType){
    case PREDICATE :
      mOutput.println("Running predicate analysis");
      analysis = new PredicateAnalysisWithReuse(pSourceFileName, pEntryFunction, shutdownNotifier, lCFA,
          mLocationCPA, mCallStackCPA, mAssumeCPA, timelimit);
      break;

    case EXPLICIT_SIMPLE :
      mOutput.println("Running simple explicit analysis");
      analysis = new ExplicitSimpleAnalysisWithReuse(pSourceFileName, pEntryFunction, shutdownNotifier, lCFA,
          mLocationCPA, mCallStackCPA, mAssumeCPA, timelimit);
      break;

    case EXPLICIT_REF :
      mOutput.println("Running explicit analysis with refinement");
      analysis = new ExplicitAnalysisWithReuse(pSourceFileName, pEntryFunction, shutdownNotifier, lCFA,
          mLocationCPA, mCallStackCPA, mAssumeCPA, timelimit);
      break;

    case EXPLICIT_PRED :
      mOutput.println("Running explicit analysis with predicate analysis");
      analysis = new ExplicitPredWithReuse(pSourceFileName, pEntryFunction, shutdownNotifier, lCFA,
          mLocationCPA, mCallStackCPA, mAssumeCPA, timelimit);
      break;

    }

    assert analysis != null;


    // we can collect test cases accross several run invocations and use them for coverage analysis
    // TODO output test cases from an earlier run
    mGeneratedTestCases = new HashMap<>();

    mTestCaseUtil = new TestCaseUtil(mWrapper, mOutput, mGeneratedTestCases, mLocationCPA, mCallStackCPA, mCFAPathCPA, mAssumeCPA, mLogManager, mConfiguration, mShutdownNotifier, pStopOnImpreciseExecution);
  }

  @Override
  public CPAtigerResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating) {
    return run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckCorrectnessOfCoverageCheck, pPedantic);
  }

  private GuardedEdgeAutomatonCPA getPassingCPA(FQLSpecification pFQLSpecification) {
    if (pFQLSpecification.hasPassingClause()) {
      mOutput.println("Cache hits (1): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (1): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      ElementaryCoveragePattern lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(pFQLSpecification.getPathPattern());

      mOutput.println("Cache hits (2): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (2): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton1 = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton2 = FQLSpecificationUtil.optimizeAutomaton(lAutomaton1, mUseAutomatonOptimization);

      return new GuardedEdgeAutomatonCPA(lAutomaton2);
    }
    else {
      return null;
    }
  }

  private boolean applyCoverageCheck(Goal pGoal, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pGoalAutomaton, GuardedEdgeAutomatonCPA pPassingCPA, CPAtigerResult.Factory pResultFactory) {
    GuardedEdgeAutomatonCPA lAutomatonCPA = null;

    for (Map.Entry<TestCase, CFAEdge[]> lGeneratedTestCase : mGeneratedTestCases.entrySet()) {
      TestCase lTestCase = lGeneratedTestCase.getKey();

      assert (lTestCase.isPrecise());

      ThreeValuedAnswer lCoverageAnswer = CPAtiger.accepts(pGoalAutomaton, lGeneratedTestCase.getValue());

      if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
        pResultFactory.addFeasibleTestCase(pGoal.getPattern(), lTestCase);

        return true;
      }
      else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
        if (lAutomatonCPA == null) {
          // TODO reuse this CPA in run method
          lAutomatonCPA = new GuardedEdgeAutomatonCPA(pGoalAutomaton);
        }

        try {
          if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
            pResultFactory.addFeasibleTestCase(pGoal.getPattern(), lTestCase);

            return true;
          }
        } catch (InvalidConfigurationException | CPAException | ImpreciseExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return false;
  }

  private CPAtigerResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered, boolean pPedantic) {

    int lNumberOfTestGoals;

    FQLSpecification lFQLSpecification = FQLSpecificationUtil.getFQLSpecification(pFQLSpecification);

    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (mUseInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(lFQLSpecification);
    }
    else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    ElementaryCoveragePattern[] lGoalPatterns;

    GuardedEdgeAutomatonCPA lPassingCPA = getPassingCPA(lFQLSpecification);

    TimeAccumulator lTimeAccu = new TimeAccumulator(2);
    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();

    boolean lUseGraphCPAOld = mUseGraphCPA;

    if (lInfeasibilityPropagation.getFirst()) {
      // deactivate graph search ... experiments showed graph search useless when we use infeasibility propagation
      // TODO investigate that issue in more detail
      mUseGraphCPA = false;
      CFANode lInitialNode = this.mAlphaLabel.getEdgeSet().iterator().next().getSuccessor();
      ClusteringCoverageSpecificationTranslator lTranslator = new ClusteringCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator, lInfeasibilityPropagation.getSecond(), lInitialNode);
      lNumberOfTestGoals = lTranslator.getNumberOfTestGoals();

      mOutput.println("Number of Test Goals: " + lNumberOfTestGoals);
      mOutput.print("Generating Patterns ... ");

      lGoalPatterns = lTranslator.createElementaryCoveragePatternsAndClusters();

      mOutput.println("done.");
    }
    else {
      IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);
      mOutput.println("Determining the number of test goals ...");

      lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(lFQLSpecification.getCoverageSpecification());
      mOutput.println("Number of test goals: " + lNumberOfTestGoals);

      Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(lFQLSpecification.getCoverageSpecification());
      lGoalPatterns = new ElementaryCoveragePattern[lNumberOfTestGoals];

      for (int lGoalIndex = 0; lGoalIndex < lNumberOfTestGoals; lGoalIndex++) {
        lGoalPatterns[lGoalIndex] = lGoalIterator.next();
      }
    }

    CPAtigerResult.Factory lResultFactory = CPAtigerResult.factory(lNumberOfTestGoals);

    // run the loop
    try {
      runAllGoals(pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckReachWhenCovered, lInfeasibilityPropagation,
          lResultFactory, lPassingCPA, lGoalPatterns, lTimeAccu, lTimeReach, lTimeCover);
    } catch (InterruptedException e) {
      // analysis interrupted - let it print the statistics
    }

    //mOutput.println("Number of CFA infeasible test goals: " + lNumberOfCFAInfeasibleGoals);
    mOutput.println("Time in reach: " + mTimeInReach.getSeconds());
    mOutput.println("Max time in reach: " + ((double) timeReachMax)/1000 + " s");
    mOutput.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");

    CPAtigerResult lResult = lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(),
        lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));

    /*if (lResult.getNumberOfTestGoals() != lNumberOfTestGoals) {
      throw new RuntimeException();
    }*/

    printStatistics(lResultFactory, lNumberOfTestGoals);
    analysis.finish();

    // print per goal runtimes
    /*Arrays.sort(lGoalRuntime);
    for (int i = 0; i < lGoalRuntime.length; i++) {
      System.out.println("#" + (i + 1) + ": " + lGoalRuntime[i] + " ms");
    }*/

    if (lInfeasibilityPropagation.getFirst()) {
      mUseGraphCPA = lUseGraphCPAOld;
    }

    return lResult;
  }


  /**
   * Run reachability analysis on the test-goal automata.
   * @param pApplySubsumptionCheck
   * @param pCheckReachWhenCovered
   * @param pApplyInfeasibilityPropagation
   * @param lInfeasibilityPropagation
   * @param lPassingCPA
   * @param lResultFactory
   * @param lGoalPatterns
   * @param lGoalRuntime
   * @param lGoalPrediction
   * @param lTimeReach
   * @param lTimeAccu
   * @param lTimeCover
   * @throws InterruptedException
   */
  private void runAllGoals(boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered,
      Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation,
      Factory lResultFactory, GuardedEdgeAutomatonCPA lPassingCPA, ElementaryCoveragePattern[] lGoalPatterns,
      TimeAccumulator lTimeAccu, TimeAccumulator lTimeReach, TimeAccumulator lTimeCover) throws InterruptedException {
    mShutdownNotifier.shutdownIfNecessary();

    int lNumberOfTestGoals = lGoalPatterns.length;

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGoalAutomaton = null;
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGraphGoalAutomaton = null;

    //int lNumberOfCFAInfeasibleGoals = 0;
    ReachedSet lPredicateReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?
    ReachedSet lGraphReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);
    Prediction[] lGoalPrediction = new Prediction[lNumberOfTestGoals];

    long[] lGoalRuntime = new long[lNumberOfTestGoals];

    for (int i = 0; i < lNumberOfTestGoals; i++) {
      lGoalRuntime[i] = -1; // value indicating invalid value
      lGoalPrediction[i] = Prediction.UNKNOWN; // value indicating unknown prediction
    }

    // compute test goals
    Goal[] lGoals = new Goal[lNumberOfTestGoals];

    for (int i = 0; i < lNumberOfTestGoals; i++) {
      ElementaryCoveragePattern lGoalPattern = lGoalPatterns[i];
      Goal lGoal = constructGoal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,  mUseAutomatonOptimization);
      lGoals[i] = lGoal;
    }

    // process goals
    int maxIndex = lNumberOfTestGoals < mMaxIndex ? lNumberOfTestGoals : mMaxIndex;

    for(int lIndex = mMinIndex+1; lIndex<= maxIndex; lIndex++) {
      mShutdownNotifier.shutdownIfNecessary();

      long lStartTime = System.currentTimeMillis();
      mOutput.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");

      Goal lGoal = lGoals[lIndex-1];
      Prediction prediction = lGoalPrediction[lIndex-1];

      if (prediction.equals(Prediction.INFEASIBLE)) {
        mOutput.println("Predicted as infeasible!");
        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);
        continue;
      }

      // check if the result is already known
      boolean skip = false;

      FeasibilityStatus status = mFeasibilityInformation.getStatus(lIndex);

      switch (status) {

      case FEASIBLE:
        mOutput.println("Goal #" + lIndex + " is covered by an existing test case!");
        skip = true;
        break;

      case INFEASIBLE:
        // probably should come here
        mOutput.println("Goal #" + lIndex + " predicted as infeasible!");
        skip = true;
        break;


      case IMPRECISE:
        // in theory we could be here, but don't know how exactly....
        assert false;
        break;

      case BUGGY:
        // in theory we could be here, but don't know how exactly....
        assert false;
        break;

        default:   // i.e. UNKNWON
          break;
      }

      if (skip){
        continue;
      }


      lTimeAccu.proceed();

      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton());

      long timeReachThisStart = System.currentTimeMillis();
      lTimeReach.proceed();

      boolean lReachableViaGraphSearch = false;

      if (!lAutomatonCPA.getAutomaton().getFinalStates().isEmpty()) {
        if (mUseGraphCPA) {
          mTimeInReach.proceed();
          mTimesInReach++;
          lReachableViaGraphSearch = reachGraphSearch(lPreviousGraphGoalAutomaton, lGraphReachedSet, lAutomatonCPA,
              mWrapper.getEntry(), lPassingCPA);
          mTimeInReach.pause();

          lPreviousGraphGoalAutomaton = lAutomatonCPA.getAutomaton();
        }
        else {
          lReachableViaGraphSearch = true;
        }
      }

      Boolean isSound = true;
      CounterexampleInfo cex = null;


      if (lReachableViaGraphSearch) {
        //lCounterexampleTraceInfo = reach(mWrapper.getCFA(), lPredicateReachedSet, lPreviousGoalAutomaton, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
        mTimeInReach.proceed();
        mTimesInReach++;
        Pair<Boolean, CounterexampleInfo> result = analysis.analyse(mWrapper.getCFA(), lPredicateReachedSet, lPreviousGoalAutomaton,
            lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
        mTimeInReach.pause();

        isSound = result.getFirst();
        cex = result.getSecond();
        // lPredicateReachedSet and lPreviousGoalAutomaton have to be in-sync.
        lPreviousGoalAutomaton = lAutomatonCPA.getAutomaton();
      }
      else {
        //lNumberOfCFAInfeasibleGoals++;
      }

      lTimeReach.pause();
      long timeThisReach = System.currentTimeMillis() - timeReachThisStart;
      if (timeThisReach > timeReachMax) {
        timeReachMax = timeThisReach;
      }

      if (!lReachableViaGraphSearch || (isSound && cex == null)){
        // goal is unreachable
        mOutput.println("Goal #" + lIndex + " is infeasible!");

        handleUnreachable(cex, lIndex, lAutomatonCPA, lPassingCPA, lResultFactory, lGoal, lGoalPrediction,
            lReachableViaGraphSearch, lInfeasibilityPropagation);

        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);
      }
      else if (!isSound || (cex.isSpurious())){
        // analysis is imprecise
        mOutput.println("Goal #" + lIndex + " lead to an imprecise execution!");

        handleImprecise(cex, lIndex, lAutomatonCPA, lPassingCPA, lResultFactory, lGoal, lGoalPrediction);

        lTimeAccu.pause(lImpreciseTestGoalsTimeSlot);
      }
      else {
        // goal seems to be reachable
        assert isSound && !cex.isSpurious();
        lTimeCover.proceed();

        TestCase lTestCase = handleReachable(cex, lIndex,  lAutomatonCPA, lPassingCPA, lResultFactory, lGoal, lGoalPrediction);

        if (pApplySubsumptionCheck) {
          coverTestCases(lTestCase, lGoal, lGoals, lPassingCPA, lResultFactory, lIndex+1, maxIndex);
        }

        mTestSuite.add(lTestCase);
        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
        lTimeCover.pause();
      }

      long lEndTime = System.currentTimeMillis();
      lGoalRuntime[lIndex-1] = lEndTime - lStartTime;

      mOutput.println("Goal #" + lIndex + " needed " + lGoalRuntime[lIndex-1] + " ms");
    }


  }

  /**
   * Check if remaning goals are cover by the test case.
   * @param pLGoals
   * @param pLTestCase
   * @param pLGoal
   * @param pLPassingCPA
   * @param pLResultFactory
   * @param pMMaxIndex
   * @param pI
   */
  private void coverTestCases(TestCase lTestCase, Goal lGoal, Goal[] lGoals, GuardedEdgeAutomatonCPA pPassingCPA,
      Factory pResultFactory, int start, int stop) {

    assert stop <= lGoals.length && 0 <= start;
    assert (lTestCase.isPrecise());

    CFAEdge[] path = mGeneratedTestCases.get(lTestCase);
    GuardedEdgeAutomatonCPA lAutomatonCPA = null;

    mOutput.print("covers goals: ");

    for (int i=start; i<stop; i++){

      if (mFeasibilityInformation.isKnown(i)){
        continue;
      }

      Goal goal = lGoals[i-1];
      ThreeValuedAnswer lCoverageAnswer = CPAtiger.accepts(goal.getAutomaton(), path);

      if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
        pResultFactory.addFeasibleTestCase(goal.getPattern(), lTestCase);

        //mOutput.println("Goal #" + i + " is covered by an existing test case!");
        mFeasibilityInformation.setStatus(i, FeasibilityInformation.FeasibilityStatus.FEASIBLE);
        // TODO remove
        mOutput.print(i+", ");

      }
      else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {

        if (lAutomatonCPA == null) {
          // TODO reuse this CPA in run method
          lAutomatonCPA = new GuardedEdgeAutomatonCPA(goal.getAutomaton());
        }

        try {
          if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
            pResultFactory.addFeasibleTestCase(goal.getPattern(), lTestCase);
          }
        } catch (InvalidConfigurationException | CPAException | ImpreciseExecutionException e) {
          throw new RuntimeException(e);
        }
      }


    }

    mOutput.println();
  }

  /**
   * Handle information about unreachability of a test goal.
   * @param cex
   * @param lIndex
   * @param lAutomatonCPA
   * @param lPassingCPA
   * @param lResultFactory
   * @param lGoal
   * @param lGoalPrediction
   * @param lReachableViaGraphSearch
   * @param lInfeasibilityPropagation
   */
  private void handleUnreachable(CounterexampleInfo cex, int lIndex, GuardedEdgeAutomatonCPA lAutomatonCPA,
      GuardedEdgeAutomatonCPA lPassingCPA, Factory lResultFactory, Goal lGoal, Prediction[] lGoalPrediction,
      boolean lReachableViaGraphSearch, Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation) {

    // if (lIsCovered) {
    //   throw new RuntimeException("Inconsistent result of coverage check and reachability analysis!");
    // }

    mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

    lResultFactory.addInfeasibleTestCase(lGoal.getPattern());


    if (lReachableViaGraphSearch && lInfeasibilityPropagation.getFirst()) {
      HashSet<CFAEdge> lTargetEdges = new HashSet<>();

      ClusteredElementaryCoveragePattern lClusteredPattern = (ClusteredElementaryCoveragePattern)lGoal.getPattern();

      ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

      int lTmpIndex = lIndex; // caution lIndex starts at 0

      while (lRemainingPatterns.hasNext()) {
        Prediction lPrediction = lGoalPrediction[lTmpIndex];

        ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

        if (lPrediction.equals(Prediction.UNKNOWN)) {
          lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
        }

        lTmpIndex++;
      }

      Collection<CFAEdge> lFoundEdges = InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(), lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);

      lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

      lTmpIndex = lIndex;

      int lPredictedElements = 0;

      while (lRemainingPatterns.hasNext()) {
        Prediction lPrediction = lGoalPrediction[lTmpIndex];

        ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

        if (lPrediction.equals(Prediction.UNKNOWN)) {
          if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
            lGoalPrediction[lTmpIndex] = Prediction.INFEASIBLE;
            lPredictedElements++;
          }
        }

        lTmpIndex++;
      }

      mOutput.println("(" + lPredictedElements + ")");
    }
  }

  /**
   * Handle imprecise reachability information.
   * @param pCex
   * @param pLIndex
   * @param pLAutomatonCPA
   * @param pLPassingCPA
   * @param pLResultFactory
   * @param pLGoal
   * @param pLGoalPrediction
   */
  private void handleImprecise(CounterexampleInfo cex, int lIndex, GuardedEdgeAutomatonCPA lAutomatonCPA,
      GuardedEdgeAutomatonCPA lPassingCPA, Factory lResultFactory, Goal lGoal, Prediction[] lGoalPrediction) {
    mTestCaseUtil.updateImpreciseTestCaseStatistics(lIndex, lResultFactory, lGoalPrediction);

  }

  /**
   * Process path that seems to satisfy the test goal.
   * @param cex
   * @param lIndex
   * @param lAutomatonCPA
   * @param lPassingCPA
   * @param lResultFactory
   * @param lGoal
   * @param lGoalPrediction
   * @return
   */
  private TestCase handleReachable(CounterexampleInfo cex, int lIndex, GuardedEdgeAutomatonCPA lAutomatonCPA,
      GuardedEdgeAutomatonCPA lPassingCPA, Factory lResultFactory, Goal lGoal, Prediction[] lGoalPrediction) {

    TestCase lTestCase = TestCase.fromCounterexample(cex, mLogManager);
    mTestCaseUtil.reconstructPath(lTestCase, lIndex, lAutomatonCPA, lPassingCPA, lResultFactory, lGoal, lGoalPrediction);

    return lTestCase;
  }

  /**
   * Constructs a test goal from the given pattern.
   * @param pLGoalPattern
   * @param pMAlphaLabel
   * @param pMInverseAlphaLabel
   * @param pMOmegaLabel
   * @param pMUseAutomatonOptimization
   * @return
   */
  private Goal constructGoal(ElementaryCoveragePattern pGoalPattern, GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel, boolean pUseAutomatonOptimization) {

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton = ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, mUseAutomatonOptimization);

    Goal lGoal = new Goal(pGoalPattern, automaton);

    return lGoal;
  }

  /**
   * The original method
   * @param pApplySubsumptionCheck
   * @param pApplyInfeasibilityPropagation
   * @param pCheckReachWhenCovered
   * @param lInfeasibilityPropagation
   * @param lResultFactory
   * @param lPassingCPA
   * @param lGoalPatterns
   * @param lTimeAccu
   * @param lTimeReach
   * @param lTimeCover
   * @throws InterruptedException
   */
  @SuppressWarnings("unused")
  private void runAllGoalsOriginal(boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered,
      Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation,
      Factory lResultFactory, GuardedEdgeAutomatonCPA lPassingCPA, ElementaryCoveragePattern[] lGoalPatterns,
      TimeAccumulator lTimeAccu, TimeAccumulator lTimeReach, TimeAccumulator lTimeCover) throws InterruptedException {
    int lIndex = 0;
    int lNumberOfTestGoals = lGoalPatterns.length;

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGoalAutomaton = null;
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGraphGoalAutomaton = null;

    //int lNumberOfCFAInfeasibleGoals = 0;
    ReachedSet lPredicateReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?
    ReachedSet lGraphReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);
    Prediction[] lGoalPrediction = new Prediction[lNumberOfTestGoals];

    long[] lGoalRuntime = new long[lNumberOfTestGoals];

    for (int i = 0; i < lGoalRuntime.length; i++) {
      lGoalRuntime[i] = -1; // value indicating invalid value
      lGoalPrediction[i] = Prediction.UNKNOWN; // value indicating unknown prediction
    }


    while (lIndex < lGoalPatterns.length) {
      mShutdownNotifier.shutdownIfNecessary();

      if (lIndex > 0) {
        mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");
      }

      long lStartTime = System.currentTimeMillis();
      lIndex++;

      // if (lIndex == 6){
      //   System.out.println("REMOVEME");
      // }

      if (lGoalPrediction[lIndex - 1].equals(Prediction.INFEASIBLE)) {
        mOutput.println("Predicted as infeasible!");
        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);
        continue;
      }

      ElementaryCoveragePattern lGoalPattern = lGoalPatterns[lIndex - 1];
      mOutput.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");

      // TODO change while loop to for loop
      if (mMinIndex > lIndex) {
        mOutput.println("Skipped.");
        continue;
      }

      if (mMaxIndex < lIndex) {
        // we do not have to enumerate unnecessary test goals
        mOutput.println("Stop test goal enumeration.");
        break;
      }

      if (mFeasibilityInformation.isKnown(lIndex)) {
        mOutput.println("Stored information: " + mFeasibilityInformation.getStatus(lIndex));
        continue;
      }

      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton = FQLSpecificationUtil.optimizeAutomaton(lGoal.getAutomaton(), mUseAutomatonOptimization);

      lTimeAccu.proceed();

      boolean lIsCovered = false;

      if (pApplySubsumptionCheck) {
        // check whether some existing test case covers the goal lGoal
        lIsCovered = applyCoverageCheck(lGoal, lGoalAutomaton, lPassingCPA, lResultFactory);
      }

      if (lIsCovered) {
        mOutput.println("Goal #" + lIndex + " is covered by an existing test case!");

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);

        Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

        switch (lCurrentPrediction) {
        case UNKNOWN:
          break;
        default:
          throw new RuntimeException("missmatching prediction: " + lCurrentPrediction);
        }

        if (!pCheckReachWhenCovered) {
          long lEndTime = System.currentTimeMillis();

          lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;

          lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);

          continue;
        }
      }

      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoalAutomaton);

      long timeReachThisStart = System.currentTimeMillis();
      lTimeReach.proceed();

      boolean lReachableViaGraphSearch = false;

      if (!lAutomatonCPA.getAutomaton().getFinalStates().isEmpty()) {
        if (mUseGraphCPA) {
          mTimeInReach.proceed();
          mTimesInReach++;
          lReachableViaGraphSearch = reachGraphSearch(lPreviousGraphGoalAutomaton, lGraphReachedSet, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
          mTimeInReach.pause();

          lPreviousGraphGoalAutomaton = lAutomatonCPA.getAutomaton();
        }
        else {
          lReachableViaGraphSearch = true;
        }
      }

      Boolean isSound = true;
      CounterexampleInfo cex = null;


      if (lReachableViaGraphSearch) {
        //lCounterexampleTraceInfo = reach(mWrapper.getCFA(), lPredicateReachedSet, lPreviousGoalAutomaton, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
        mTimeInReach.proceed();
        mTimesInReach++;
        Pair<Boolean, CounterexampleInfo> result = analysis.analyse(mWrapper.getCFA(), lPredicateReachedSet, lPreviousGoalAutomaton, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
        mTimeInReach.pause();

        isSound = result.getFirst();
        cex = result.getSecond();
        // lPredicateReachedSet and lPreviousGoalAutomaton have to be in-sync.
        lPreviousGoalAutomaton = lAutomatonCPA.getAutomaton();
      }
      else {
        //lNumberOfCFAInfeasibleGoals++;
      }

      lTimeReach.pause();
      long timeThisReach = System.currentTimeMillis() - timeReachThisStart;
      if (timeThisReach > timeReachMax) {
        timeReachMax = timeThisReach;
      }

      if (!lReachableViaGraphSearch || (isSound && cex == null)){
        // goal is unreachable
        mOutput.println("Goal #" + lIndex + " is infeasible!");

        if (lIsCovered) {
          throw new RuntimeException("Inconsistent result of coverage check and reachability analysis!");
        }

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());

        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);

        if (lReachableViaGraphSearch && lInfeasibilityPropagation.getFirst()) {
          HashSet<CFAEdge> lTargetEdges = new HashSet<>();

          ClusteredElementaryCoveragePattern lClusteredPattern = (ClusteredElementaryCoveragePattern)lGoalPattern;

          ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          int lTmpIndex = lIndex; // caution lIndex starts at 0

          while (lRemainingPatterns.hasNext()) {
            Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(Prediction.UNKNOWN)) {
              lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
            }

            lTmpIndex++;
          }

          Collection<CFAEdge> lFoundEdges = InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(), lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);

          lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          lTmpIndex = lIndex;

          int lPredictedElements = 0;

          while (lRemainingPatterns.hasNext()) {
            Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(Prediction.UNKNOWN)) {
              if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
                lGoalPrediction[lTmpIndex] = Prediction.INFEASIBLE;
                lPredictedElements++;
              }
            }

            lTmpIndex++;
          }

          mOutput.println("(" + lPredictedElements + ")");
        }
      }
      else if (!isSound || (cex.isSpurious())){
        // analysis is imprecise
        mOutput.println("Goal #" + lIndex + " lead to an imprecise execution!");
        mTestCaseUtil.updateImpreciseTestCaseStatistics(lIndex, lResultFactory, lGoalPrediction);
        lTimeAccu.pause(lImpreciseTestGoalsTimeSlot);
      }
      else {
        // goal is reachable
        assert isSound && !cex.isSpurious();

        lTimeCover.proceed();
        TestCase lTestCase = TestCase.fromCounterexample(cex, mLogManager);
        mTestSuite.add(lTestCase);
        mTestCaseUtil.reconstructPath(lTestCase, lIndex, lAutomatonCPA, lPassingCPA, lResultFactory, lGoal, lGoalPrediction);

        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
        lTimeCover.pause();
      }

      long lEndTime = System.currentTimeMillis();
      lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;
    }

    mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");

  }

  private void printStatistics(CPAtigerResult.Factory pResultFactory, int lNumberOfTestGoals) {
    mOutput.println("Generated Test Cases:");

    for (TestCase lTestCase : pResultFactory.getTestCases()) {
      mOutput.println(lTestCase);
    }

    mOutput.println("INTERN:");
    mOutput.println("#Goals: " + lNumberOfTestGoals);
    mOutput.println("#Feasible: " + mFeasibilityInformation.getNumberOfFeasibleTestgoals());
    mOutput.println("#Infeasible: " + mFeasibilityInformation.getNumberOfInfeasibleTestgoals());
    mOutput.println("#Imprecise: " + mFeasibilityInformation.getNumberOfImpreciseTestgoals());
    mOutput.println("#BugRevealing: " + mFeasibilityInformation.getNumberOfBugRevealingTestgoals());
  }


  private boolean checkCoverage(TestCase pTestCase, FunctionEntryNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
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

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
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
      return false;
    }
    else {
      if (((LocationState)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
        // location of last element is at end node

        AbstractState lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

        if (lProductAutomatonElement instanceof Targetable) {
          Targetable lTargetable = (Targetable)lProductAutomatonElement;

          return lTargetable.isTarget();
        }

        return false;
      }

      return false;
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

  private boolean reachGraphSearch(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, ReachedSet pReachedSet, GuardedEdgeAutomatonCPA pAutomatonCPA, FunctionEntryNode pEntryNode, GuardedEdgeAutomatonCPA pPassingCPA) {

    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    lComponentAnalyses.add(mCallStackCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }

    lAutomatonCPAs.add(pAutomatonCPA);

    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    lComponentAnalyses.add(mAssumeCPA);

    ConfigurableProgramAnalysis lCPA;
    ARGCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(mConfiguration);
      lCPAFactory.setLogger(mLogManager);
      lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(mConfiguration);
      lARTCPAFactory.setLogger(mLogManager);

      lARTCPA = (ARGCPA)lARTCPAFactory.createInstance();
      //lARTCPA.precisionAdjustment.deactivate();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    if (mReuseART) {
      ARTReuse.modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());
    }
    else {
      pReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);

      AbstractState lInitialElement = lARTCPA.getInitialState(pEntryNode);
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }

    CPAAlgorithm lBasicAlgorithm;
    try {
      lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager, mConfiguration, null);
    } catch (InvalidConfigurationException e1) {
      throw new RuntimeException(e1);
    }

    ARGStatistics lARTStatistics;
    try {
      lARTStatistics = new ARGStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<>();
    lStatistics.add(lARTStatistics);
    lBasicAlgorithm.collectStatistics(lStatistics);


    try {
      lBasicAlgorithm.run(pReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    ARGState lLastARTElement = (ARGState)pReachedSet.getLastState();

    CompositeState lLastElement = (CompositeState)lLastARTElement.getWrappedState();
    ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lLastElement.get(lProductAutomatonIndex);

    return lProductAutomatonElement.isFinalState();
  }

}

