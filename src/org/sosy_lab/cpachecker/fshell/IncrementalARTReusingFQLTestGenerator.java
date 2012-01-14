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
package org.sosy_lab.cpachecker.fshell;

import java.io.IOException;
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

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.cache.CacheCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fshell.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.interfaces.FQLTestGenerator;
import org.sosy_lab.cpachecker.fshell.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.fshell.testcases.TestSuite;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

import com.google.common.collect.ImmutableSetMultimap;

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
  private final ConfigurableProgramAnalysis mPredicateCPA;

  private final TimeAccumulator mTimeInReach;
  private int mTimesInReach;
  private final GuardedEdgeLabel mAlphaLabel;
  private final GuardedEdgeLabel mOmegaLabel;
  private final GuardedEdgeLabel mInverseAlphaLabel;
  private final Map<TestCase, CFAEdge[]> mGeneratedTestCases;

  private int mMinIndex = 0;
  private int mMaxIndex = Integer.MAX_VALUE;

  private boolean mDoRestart = false;
  private long mRestartBound = 100000000; // 100 MB

  private boolean mUseAutomatonOptimization = true;
  private boolean mUseGraphCPA = false; // TODO disabled it since it causes a bug when doing FQL queries with PASSING clause 
  private boolean mReuseART = true;
  private boolean mUseInfeasibilityPropagation = true;

  private boolean mPrintPredicateStatistics = false;

  private FeasibilityInformation mFeasibilityInformation;
  private TestSuite mTestSuite;

  private PrintStream mOutput = System.out;

  public void setOutput(PrintStream pOutput) {
    mOutput = pOutput;
  }

  public void setGoalIndices(int pMinIndex, int pMaxIndex) {
    mMinIndex = pMinIndex;
    mMaxIndex = pMaxIndex;
  }

  public void doRestart() {
    mDoRestart = true;
  }

  public void setRestartBound(long pRestartBound) {
    mRestartBound = pRestartBound;
  }

  public void setFeasibilityInformation(FeasibilityInformation pFeasibilityInformation) {
    mFeasibilityInformation = pFeasibilityInformation;
  }

  public void setTestSuite(TestSuite pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    mTestSuite = pTestSuite;
    seed(pTestSuite);
  }

  public void seed(Iterable<TestCase> pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    FQLSpecification lIdStarFQLSpecification;
    try {
      lIdStarFQLSpecification = FQLSpecification.parse("COVER \"EDGES(ID)*\" PASSING EDGES(ID)*");
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      throw new RuntimeException(e1);
    }
    ElementaryCoveragePattern lIdStarPattern = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(lIdStarFQLSpecification.getPathPattern());
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lIdStarPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
    GuardedEdgeAutomatonCPA lIdStarCPA = new GuardedEdgeAutomatonCPA(lAutomaton);

    for (TestCase lTestCase : pTestSuite) {
      CFAEdge[] lPath = reconstructPath(lTestCase, mWrapper.getEntry(), lIdStarCPA, null, mWrapper.getOmegaEdge().getSuccessor());

      mGeneratedTestCases.put(lTestCase, lPath);
    }
  }

  public IncrementalARTReusingFQLTestGenerator(String pSourceFileName, String pEntryFunction) {
    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      mConfiguration = FShell3.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new LogManager(mConfiguration);

      lCFAMap = FShell3.getCFAMap(pSourceFileName, mConfiguration, mLogManager);
      lMainFunction = lCFAMap.get(pEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    /*
     * We have to instantiate mCoverageSpecificationTranslator before the wrapper
     * changes the underlying CFA. FQL specifications are evaluated against the
     * target graph generated during initialization of mCoverageSpecificationTranslator.
     */
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lMainFunction);

    mWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, mLogManager);

    try {
      mWrapper.toDot("test/output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getOmegaEdge()));


    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      mLocationCPA = (LocationCPA)LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      mCallStackCPA = (CallstackCPA)lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // assume CPA
    mAssumeCPA = AssumeCPA.getCBMCAssume();

    // cfa path CPA
    mCFAPathCPA = CFAPathCPA.getInstance();

    // TODO make configurable
    // ... cache does not work well for big examples
    boolean lUseCache = false;

    // predicate abstraction CPA
    CPAFactory lPredicateCPAFactory = PredicateCPA.factory();
    lPredicateCPAFactory.setConfiguration(mConfiguration);
    lPredicateCPAFactory.setLogger(mLogManager);
    try {
      ConfigurableProgramAnalysis lPredicateCPA = lPredicateCPAFactory.createInstance();

      if (lUseCache) {
        mPredicateCPA = new CacheCPA(lPredicateCPA);
      }
      else {
        mPredicateCPA = lPredicateCPA;
      }
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    mTimeInReach = new TimeAccumulator();
    mTimesInReach = 0;

    // we can collect test cases accross several run invocations and use them for coverage analysis
    // TODO output test cases from an earlier run
    mGeneratedTestCases = new HashMap<TestCase, CFAEdge[]>();
  }

  @Override
  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating) {
    return run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckCorrectnessOfCoverageCheck, pPedantic);
  }

  private FQLSpecification getFQLSpecification(String pFQLSpecification) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return lFQLSpecification;
  }

  private GuardedEdgeAutomatonCPA getPassingCPA(FQLSpecification pFQLSpecification) {
    if (pFQLSpecification.hasPassingClause()) {
      mOutput.println("Cache hits (1): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (1): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      ElementaryCoveragePattern lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(pFQLSpecification.getPathPattern());

      mOutput.println("Cache hits (2): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (2): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton1 = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton2 = optimizeAutomaton(lAutomaton1);

      return new GuardedEdgeAutomatonCPA(lAutomaton2);
    }
    else {
      return null;
    }
  }

  private NondeterministicFiniteAutomaton<GuardedEdgeLabel> optimizeAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    if (mUseAutomatonOptimization) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton1 = ToGuardedAutomatonTranslator.removeInfeasibleTransitions(pAutomaton);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton2 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton1);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton3 = ToGuardedAutomatonTranslator.reduceEdgeSets(lGoalAutomaton2);

      return lGoalAutomaton3;
    }

    return pAutomaton;
  }

  private FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered, boolean pPedantic) {

    int lNumberOfTestGoals;

    FQLSpecification lFQLSpecification = getFQLSpecification(pFQLSpecification);

    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (mUseInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(lFQLSpecification);
    }
    else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    ElementaryCoveragePattern[] lGoalPatterns;

    GuardedEdgeAutomatonCPA lPassingCPA = getPassingCPA(lFQLSpecification);

    // set up utility variables
    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;

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

      /*IncrementalCoverageSpecificationTranslator lTranslator2 = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);
      Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator2.translate(lFQLSpecification.getCoverageSpecification());
      for (int lGoalIndex = 0; lGoalIndex < lNumberOfTestGoals; lGoalIndex++) {
        ClusteredElementaryCoveragePattern lGoal1 = (ClusteredElementaryCoveragePattern)lGoalPatterns[lGoalIndex];
        ECPConcatenation lGoal2 = (ECPConcatenation)lGoalIterator.next();

        ECPConcatenation lGoal1Prime = (ECPConcatenation)lGoal1.getWrappedPattern();

        if (lGoal2.size() != lGoal1Prime.size()) {
          System.out.println("sizes " + lGoal2.size() + " vs. " + lGoal1Prime.size());
          //throw new RuntimeException("sizes " + lGoal2.size() + " vs. " + lGoal1Prime.size());
        }

        for (int u = 0; u < lGoal2.size(); u++) {
          if (!lGoal2.get(u).equals(lGoal1Prime.get(u))) {
            throw new RuntimeException("moohoohoo");
          }
        }

        if (!lGoal1Prime.equals(lGoal2)) {
          throw new RuntimeException("unequality at index " + lGoalIndex);
        }
      }*/

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


    int lNumberOfCFAInfeasibleGoals = 0;

    ReachedSet lPredicateReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGoalAutomaton = null;

    ReachedSet lGraphReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGraphGoalAutomaton = null;

    FShell3Result.Factory lResultFactory = FShell3Result.factory();

    int lIndex = 0;

    boolean pHadProgress = false;

    long[] lGoalRuntime = new long[lNumberOfTestGoals];
    InfeasibilityPropagation.Prediction[] lGoalPrediction = new InfeasibilityPropagation.Prediction[lNumberOfTestGoals];

    for (int i = 0; i < lGoalRuntime.length; i++) {
      lGoalRuntime[i] = -1; // value indicating invalid value
      lGoalPrediction[i] = InfeasibilityPropagation.Prediction.UNKNOWN; // value indicating unknown prediction
    }

    //while (lGoalIterator.hasNext()) {
    while (lIndex < lGoalPatterns.length) {
      if (lIndex > 0) {
        mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");
      }

      long lStartTime = System.currentTimeMillis();

      if (mDoRestart) {
        try {
          Sigar lSigar = new Sigar();

          Mem lMemory = lSigar.getMem();

          if (pHadProgress && lMemory.getFree() < mRestartBound) {
            mOutput.println("SHUTDOWN TEST GENERATION");

            lResultFactory.setUnfinished();

            break;
          }
        } catch (SigarException e) {
          throw new RuntimeException(e);
        }
      }

      lIndex++;


      if (lGoalPrediction[lIndex - 1].equals(InfeasibilityPropagation.Prediction.INFEASIBLE)) {
        mOutput.println("Predicted as infeasible!");

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

        continue;
      }


      //ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();
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

      pHadProgress = true;

      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton = optimizeAutomaton(lGoal.getAutomaton());

      lTimeAccu.proceed();

      boolean lIsCovered = false;

      if (pApplySubsumptionCheck) {
        for (Map.Entry<TestCase, CFAEdge[]> lGeneratedTestCase : mGeneratedTestCases.entrySet()) {
          TestCase lTestCase = lGeneratedTestCase.getKey();

          if (!lTestCase.isPrecise()) {
            //throw new RuntimeException();
            continue; // don't use imprecise test cases for coverage
          }

          ThreeValuedAnswer lCoverageAnswer = FShell3.accepts(lGoalAutomaton, lGeneratedTestCase.getValue());

          if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
            lIsCovered = true;

            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

            break;
          }
          else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
            GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoalAutomaton);

            try {
              if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
                lIsCovered = true;

                lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

                break;
              }
            } catch (InvalidConfigurationException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
              throw new RuntimeException(e);
            } catch (CPAException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
              throw new RuntimeException(e);
            } catch (ImpreciseExecutionException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }
        }
      }

      if (lIsCovered) {
        mOutput.println("Goal #" + lIndex + " is covered by an existing test case!");

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);

        InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

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

      lTimeReach.proceed();

      boolean lReachableViaGraphSearch = false;

      if (!lAutomatonCPA.getAutomaton().getFinalStates().isEmpty()) {
        if (mUseGraphCPA) {
          lReachableViaGraphSearch = reachGraphSearch(lPreviousGraphGoalAutomaton, lGraphReachedSet, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);

          lPreviousGraphGoalAutomaton = lAutomatonCPA.getAutomaton();
        }
        else {
          lReachableViaGraphSearch = true;
        }
      }

      CounterexampleTraceInfo lCounterexampleTraceInfo = null;

      if (lReachableViaGraphSearch) {
        lCounterexampleTraceInfo = reach(lPredicateReachedSet, lPreviousGoalAutomaton, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);

        // lPredicateReachedSet and lPreviousGoalAutomaton have to be in-sync.
        lPreviousGoalAutomaton = lAutomatonCPA.getAutomaton();
      }
      else {
        lNumberOfCFAInfeasibleGoals++;
      }

      lTimeReach.pause();

      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        mOutput.println("Goal #" + lIndex + " is infeasible!");

        if (lIsCovered) {
          throw new RuntimeException("Inconsistent result of coverage check and reachability analysis!");
        }

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());

        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);

        long lEndTime = System.currentTimeMillis();

        lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;

        if (lReachableViaGraphSearch && lInfeasibilityPropagation.getFirst()) {
          HashSet<CFAEdge> lTargetEdges = new HashSet<CFAEdge>();

          ClusteredElementaryCoveragePattern lClusteredPattern = (ClusteredElementaryCoveragePattern)lGoalPattern;

          ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          //int lTmpIndex = lIndex + 1;
          // TODO check again!
          int lTmpIndex = lIndex; // caution lIndex starts at 0

          while (lRemainingPatterns.hasNext()) {
            //System.out.println("lTmpIndex: " + lTmpIndex);

            InfeasibilityPropagation.Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
            }

            lTmpIndex++;
          }

          Collection<CFAEdge> lFoundEdges = InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(), lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);

          lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          //lTmpIndex = lIndex + 1;
          lTmpIndex = lIndex;

          int lPredictedElements = 0;

          while (lRemainingPatterns.hasNext()) {
            InfeasibilityPropagation.Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
                lGoalPrediction[lTmpIndex] = InfeasibilityPropagation.Prediction.INFEASIBLE;
                lPredictedElements++;
              }
            }

            lTmpIndex++;
          }

          mOutput.println("(" + lPredictedElements + ")");
        }
      }
      else {
        lTimeCover.proceed();

        TestCase lTestCase = TestCase.fromCounterexample(lCounterexampleTraceInfo, mLogManager);

        mTestSuite.add(lTestCase);

        if (lTestCase.isPrecise()) {
          CFAEdge[] lCFAPath = null;

          boolean lIsPrecise = true;

          try {
            lCFAPath = reconstructPath(lTestCase, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
          } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
          } catch (CPAException e) {
            throw new RuntimeException(e);
          } catch (ImpreciseExecutionException e) {
            lIsPrecise = false;
            lTestCase = e.getTestCase();

            if (pPedantic) {
              throw new RuntimeException(e);
            }
          }

          if (lIsPrecise) {
            mOutput.println("Goal #" + lIndex + " is feasible!");

            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

            // we only add precise test cases for coverage analysis
            mGeneratedTestCases.put(lTestCase, lCFAPath);

            mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);



            InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

            if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              throw new RuntimeException("missmatching prediction");
            }
          }
          else {
            mOutput.println("Goal #" + lIndex + " lead to an imprecise execution!");

            lResultFactory.addImpreciseTestCase(lTestCase);

            mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

            InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

            if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              throw new RuntimeException("missmatching prediction");
            }
          }
        }
        else {
          mOutput.println("Goal #" + lIndex + " is imprecise!");

          lResultFactory.addImpreciseTestCase(lTestCase);

          mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

          InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

          if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
            throw new RuntimeException("missmatching prediction");
          }
/*
          if (!pPedantic) {
          //if (true) {
            // TODO implement reconstruction of path
            throw new RuntimeException();
          }*/
        }

        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
        lTimeCover.pause();

        long lEndTime = System.currentTimeMillis();

        lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;
      }
    }

    mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");

    mOutput.println("Number of CFA infeasible test goals: " + lNumberOfCFAInfeasibleGoals);

    mOutput.println("Time in reach: " + mTimeInReach.getSeconds());
    mOutput.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");

    // TODO remove ... look at statistics
    //System.out.println("#abstraction elements: " + mPredicateCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    //System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);

    FShell3Result lResult = lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));

    /*if (lResult.getNumberOfTestGoals() != lNumberOfTestGoals) {
      throw new RuntimeException();
    }*/

    printStatistics(lResultFactory);

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

  private void printStatistics(FShell3Result.Factory pResultFactory) {
    mOutput.println("Generated Test Cases:");

    for (TestCase lTestCase : pResultFactory.getTestCases()) {
      mOutput.println(lTestCase);
    }

    mOutput.println("INTERN:");
    mOutput.println("#Goals: " + mFeasibilityInformation.getNumberOfTestgoals() + ", #Feasible: " + mFeasibilityInformation.getNumberOfFeasibleTestgoals() + ", #Infeasible: " + mFeasibilityInformation.getNumberOfInfeasibleTestgoals() + ", #Imprecise: " + mFeasibilityInformation.getNumberOfImpreciseTestgoals());
  }

  private Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineLocalDifference(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton, NondeterministicFiniteAutomaton.State pPreviousState, NondeterministicFiniteAutomaton.State pCurrentState) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE1 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE2 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE1.add(lEdge);
      }
    }

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE2.add(lEdge);
      }
    }

    return Pair.of(lE1, lE2);
  }

  private Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineFrontier(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF1 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF2 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();

    LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lWorklist = new LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();

    if (pPreviousAutomaton.getInitialState() != pCurrentAutomaton.getInitialState()) {
      throw new RuntimeException();
    }

    lWorklist.add(Pair.of(pPreviousAutomaton.getInitialState(), pCurrentAutomaton.getInitialState()));

    HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lVisited = new HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();

    while (!lWorklist.isEmpty()) {
      Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lCurrentPair = lWorklist.removeLast();

      if (!lVisited.contains(lCurrentPair)) {
        lVisited.add(lCurrentPair);

        // determine local difference

        Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineLocalDifference(pPreviousAutomaton, pCurrentAutomaton, lCurrentPair.getFirst(), lCurrentPair.getSecond());

        if (lFrontier.getFirst().isEmpty() && lFrontier.getSecond().isEmpty()) {
          // update worklist
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
            // was mit !lFrontier.getFirst().isEmpty() machen?
            lWorklist.add(Pair.of(lEdge.getTarget(), lEdge.getTarget()));
          }
        }
        else {
          lF1.addAll(lFrontier.getFirst());
          lF2.addAll(lFrontier.getSecond());
        }
      }
    }

    return Pair.of(lF1, lF2);
  }

  PredicatePrecision mPrecision;
  ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> mBuilder = new ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate>();
  HashSet<AbstractionPredicate> mGlobalPredicates = new HashSet<AbstractionPredicate>();

  private CounterexampleTraceInfo reach(ReachedSet pReachedSet, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, GuardedEdgeAutomatonCPA pAutomatonCPA, CFAFunctionDefinitionNode pEntryNode, GuardedEdgeAutomatonCPA pPassingCPA) {
    mTimeInReach.proceed();
    mTimesInReach++;

    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    lComponentAnalyses.add(mCallStackCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

    if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }

    lAutomatonCPAs.add(pAutomatonCPA);

    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    int lPredicateCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mPredicateCPA);

    lComponentAnalyses.add(mAssumeCPA);

    ARTCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(mConfiguration);
      lCPAFactory.setLogger(mLogManager);
      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARTCPA.factory();
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(mConfiguration);
      lARTCPAFactory.setLogger(mLogManager);

      lARTCPA = (ARTCPA)lARTCPAFactory.createInstance();
/*
      lARTCPA.precisionAdjustment.setCache(mInfeasibilityCache);
      lARTCPA.precisionAdjustment.setPredicateCPAIndex(lPredicateCPAIndex);
      lARTCPA.precisionAdjustment.setAutomatonCPAIndex(lProductAutomatonIndex);*/
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    CPAAlgorithm lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager);

    PredicateRefiner lRefiner;
    try {
      lRefiner = new PredicateRefiner(lBasicAlgorithm.getCPA(), mBuilder, mGlobalPredicates);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    CEGARAlgorithm lAlgorithm;
    try {
      lAlgorithm = new CEGARAlgorithm(lBasicAlgorithm, lRefiner, mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    ARTStatistics lARTStatistics;
    try {
      lARTStatistics = new ARTStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);

    if (mReuseART) {
      modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());

      if (mPrecision != null) {
        for (AbstractElement lWaitlistElement : pReachedSet.getWaitlist()) {
        //for (AbstractElement lWaitlistElement : pReachedSet) {
          pReachedSet.updatePrecision(lWaitlistElement, mPrecision);
        }
      }
    }
    else {
      pReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.TOPSORT);

      AbstractElement lInitialElement = lARTCPA.getInitialElement(pEntryNode);
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }

    PredicatePrecisionAdjustment lAdjustment = (PredicatePrecisionAdjustment)mPredicateCPA.getPrecisionAdjustment();
    lAdjustment.NUMBER_OF_ABSTRACTIONS = 0;

    try {
      lAlgorithm.run(pReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    mOutput.println("Number of abstractions: " + lAdjustment.NUMBER_OF_ABSTRACTIONS);

    mPrecision = new PredicatePrecision(mBuilder.build(), mGlobalPredicates);

    if (mPrintPredicateStatistics) {
      printPredicateStatistics(pReachedSet, lPredicateCPAIndex);
    }

    mTimeInReach.pause();

    CounterexampleTraceInfo lCounterexampleTraceInfo = lRefiner.getCounterexampleTraceInfo();

    /*if (lCounterexampleTraceInfo != null && lCounterexampleTraceInfo.isSpurious()) {
      // TODO automaton index
      InfeasibilityCacheEntry lEntry = new InfeasibilityCacheEntry(pAutomatonCPA.getAutomaton(), pReachedSet, lPredicateCPAIndex, lProductAutomatonIndex);

      mInfeasibilityCache.add(lEntry);
    }
    else if (lCounterexampleTraceInfo != null) {
      // TODO feasibility cache

      ARTElement lLastElement = (ARTElement)pReachedSet.getLastElement();

      // TODO how many abstraction points with state >= 5 are in the reached set?

      throw new RuntimeException();
    }*/

    /*if (lCounterexampleTraceInfo != null) {
      if (lCounterexampleTraceInfo.isSpurious()) {
        System.out.println("SPURIOUS ************************");
      }
      else {
        System.out.println("NOT SPURIOUS ********************");
      }

      for (AbstractElement lReachedElement : pReachedSet) {
        ARTElement lARTElement = (ARTElement)lReachedElement;

        CompositeElement lCompositeElement = (CompositeElement)lARTElement.getWrappedElement();

        PredicateAbstractElement lPredicateElement = (PredicateAbstractElement)lCompositeElement.get(lPredicateCPAIndex);

        if (lPredicateElement instanceof AbstractionElement) {
          AbstractionElement lAbstractionElement = (AbstractionElement)lPredicateElement;

          if (!lAbstractionElement.getAbstractionFormula().isFalse()) {
            ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lCompositeElement.get(lProductAutomatonIndex);

            // TODO generalize
            GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lProductAutomatonElement.get(0);

            if (lStateElement.getAutomatonState().ID >= 5) {
              System.out.println("ping");
            }
          }
        }
      }
    }*/

    return lCounterexampleTraceInfo;
  }

  //HashSet<InfeasibilityCacheEntry> mInfeasibilityCache = new HashSet<InfeasibilityCacheEntry>();

  private static void printPredicateStatistics(ReachedSet pReachedSet, int lPredicateCPAIndex) {

    Map<Integer, Integer> lPredicates = new HashMap<Integer, Integer>();

    int lMaxNumberOfPredicates = 0;

    for (Pair<AbstractElement, Precision> lPair : pReachedSet.getReachedWithPrecision()) {
      ARTElement lARTElement = (ARTElement)lPair.getFirst();
      CompositePrecision lPrecision = (CompositePrecision)lPair.getSecond();

      PredicatePrecision lPredicatePrecision = (PredicatePrecision)lPrecision.get(lPredicateCPAIndex);

      CFANode lLocation = ((CompositeElement)lARTElement.getWrappedElement()).retrieveLocationElement().getLocationNode();

      int lNumberOfPredicates = lPredicatePrecision.getPredicates(lLocation).size();

      if (lNumberOfPredicates > lMaxNumberOfPredicates) {
        lMaxNumberOfPredicates = lNumberOfPredicates;
      }

      int lCounter = 0;
      if (lPredicates.containsKey(lNumberOfPredicates)) {
        lCounter = lPredicates.get(lNumberOfPredicates);
      }

      lCounter++;
      lPredicates.put(lNumberOfPredicates, lCounter);

      if (lNumberOfPredicates >= 20) {
        System.out.println(lPredicatePrecision.getPredicates(lLocation));
      }
    }

    System.out.println("Max number of predicates: " + lMaxNumberOfPredicates);

    for (int i = 0; i <= lMaxNumberOfPredicates; i++) {
      int lCounter = 0;

      if (lPredicates.containsKey(i)) {
        lCounter = lPredicates.get(i);
      }

      System.out.println("" + i + " predicates: " + lCounter);
    }

  }

  private void modifyReachedSet(ReachedSet pReachedSet, CFAFunctionDefinitionNode pEntryNode, ARTCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    if (pReachedSet.isEmpty()) {
      AbstractElement lInitialElement = pARTCPA.getInitialElement(pEntryNode);
      Precision lInitialPrecision = pARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }
    else {
      if (pPreviousAutomaton == null) {
        throw new RuntimeException();
      }

      modifyART(pReachedSet, pARTCPA, pProductAutomatonIndex, pPreviousAutomaton, pCurrentAutomaton);
    }
  }

  private void modifyART(ReachedSet pReachedSet, ARTReachedSet pARTReachedSet, int pProductAutomatonIndex, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pFrontierEdges) {
    //Set<Pair<ARTElement, ARTElement>> lPathEdges = Collections.emptySet();
    //ARTStatistics.dumpARTToDotFile(new File("/home/andreas/art01.dot"), lARTCPA, pReachedSet, lPathEdges);

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pFrontierEdges) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

      for (CFAEdge lCFAEdge : lEdgeSet) {
        CFANode lCFANode = lCFAEdge.getPredecessor();

        Set<AbstractElement> lAbstractElements = pReachedSet.getReached(lCFANode);

        LinkedList<AbstractElement> lAbstractElements2 = new LinkedList<AbstractElement>();
        lAbstractElements2.addAll(lAbstractElements);

        for (AbstractElement lAbstractElement : lAbstractElements2) {
          if (!pReachedSet.contains(lAbstractElement)) {
            // lAbstractElement was removed in an earlier step
            continue;
          }

          ARTElement lARTElement = (ARTElement)lAbstractElement;

          if (lARTElement.retrieveLocationElement().getLocationNode() != lCFANode) {
            continue;
          }

          // what's the semantics of getWrappedElement*s*()?
          CompositeElement lCompositeElement = (CompositeElement)lARTElement.getWrappedElement();

          ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lCompositeElement.get(pProductAutomatonIndex);

          GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lProductAutomatonElement.get(0);

          if (lStateElement.getAutomatonState() == lEdge.getSource()) {
            if (lARTElement.getChildren().isEmpty()) {
              // re-add element to worklist
              pReachedSet.reAddToWaitlist(lARTElement);
            }
            else {
              // by removing the children, lARTElement gets added to the
              // worklist automatically

              /* TODO add removal of only non-isomorphic parts again */
              while (!lARTElement.getChildren().isEmpty()) {
                ARTElement lChildElement = lARTElement.getChildren().iterator().next();

                pARTReachedSet.removeSubtree(lChildElement);
              }
            }
          }
        }
      }
    }
  }

  private void modifyART(ReachedSet pReachedSet, ARTCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    ARTReachedSet lARTReachedSet = new ARTReachedSet(pReachedSet, pARTCPA);

    Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineFrontier(pPreviousAutomaton, pCurrentAutomaton);

    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getFirst());
    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getSecond());
  }

  private boolean checkCoverage(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

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

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();

    if (lEndNode == null) {
      return false;
    }
    else {
      if (((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
        // location of last element is at end node

        AbstractElement lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

        if (lProductAutomatonElement instanceof Targetable) {
          Targetable lTargetable = (Targetable)lProductAutomatonElement;

          return lTargetable.isTarget();
        }

        return false;
      }

      return false;
    }
  }

  private CFAEdge[] reconstructPath(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

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
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();

    if (lEndNode == null) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    if (!((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    AbstractElement lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

    if (!(lProductAutomatonElement instanceof Targetable)) {
      throw new RuntimeException();
    }

    Targetable lTargetable = (Targetable)lProductAutomatonElement;

    if (!lTargetable.isTarget()) {
      throw new RuntimeException();
    }

    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lEndNode.get(lCFAPathCPAIndex);

    return lPathElement.toArray();
  }

  private boolean reachGraphSearch(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, ReachedSet pReachedSet, GuardedEdgeAutomatonCPA pAutomatonCPA, CFAFunctionDefinitionNode pEntryNode, GuardedEdgeAutomatonCPA pPassingCPA) {
    mTimeInReach.proceed();
    mTimesInReach++;

    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    lComponentAnalyses.add(mCallStackCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<ConfigurableProgramAnalysis>(2);

    if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }

    lAutomatonCPAs.add(pAutomatonCPA);

    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    lComponentAnalyses.add(mAssumeCPA);

    ConfigurableProgramAnalysis lCPA;
    ARTCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(mConfiguration);
      lCPAFactory.setLogger(mLogManager);
      lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARTCPA.factory();
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(mConfiguration);
      lARTCPAFactory.setLogger(mLogManager);

      lARTCPA = (ARTCPA)lARTCPAFactory.createInstance();
      //lARTCPA.precisionAdjustment.deactivate();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    if (mReuseART) {
      modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());
    }
    else {
      pReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);

      AbstractElement lInitialElement = lARTCPA.getInitialElement(pEntryNode);
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }

    CPAAlgorithm lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager);


    ARTStatistics lARTStatistics;
    try {
      lARTStatistics = new ARTStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lBasicAlgorithm.collectStatistics(lStatistics);


    try {
      lBasicAlgorithm.run(pReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }

    ARTElement lLastARTElement = (ARTElement)pReachedSet.getLastElement();

    CompositeElement lLastElement = (CompositeElement)lLastARTElement.getWrappedElement();
    ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lLastElement.get(lProductAutomatonIndex);

    mTimeInReach.pause();

    return lProductAutomatonElement.isFinalState();
  }
}

