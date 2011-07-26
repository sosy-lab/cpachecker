package org.sosy_lab.cpachecker.efshell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 *
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
  private Map<String, CFAFunctionDefinitionNode> lCFAMap;
  private final Map<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, Collection<NondeterministicFiniteAutomaton.State>> mInfeasibleGoals;

  public void seed(Collection<TestCase> pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
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

    mAlphaLabel = new GuardedEdgeLabel(new ECPEdgeSet(mWrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new ECPEdgeSet(mWrapper.getOmegaEdge()));


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

    mInfeasibleGoals = new HashMap<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, Collection<NondeterministicFiniteAutomaton.State>>();
  }

  @Override
  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating) {
    return run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckCorrectnessOfCoverageCheck, pPedantic);
  }

  private FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered, boolean pPedantic) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.out.println("Cache hits (1): " + mCoverageSpecificationTranslator.getOverallCacheHits());
    System.out.println("Cache misses (1): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

    ElementaryCoveragePattern lPassingClause = null;

    if (lFQLSpecification.hasPassingClause()) {
      lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(lFQLSpecification.getPathPattern());
    }

    System.out.println("Cache hits (2): " + mCoverageSpecificationTranslator.getOverallCacheHits());
    System.out.println("Cache misses (2): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

    FShell3Result.Factory lResultFactory = FShell3Result.factory();

    GuardedEdgeAutomatonCPA lPassingCPA = null;

    if (lPassingClause != null) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      lPassingCPA = new GuardedEdgeAutomatonCPA(lAutomaton);
    }

    // set up utility variables
    int lIndex = 0;

    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;

    TimeAccumulator lTimeAccu = new TimeAccumulator(2);

    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();

    IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

    int lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(lFQLSpecification.getCoverageSpecification());

    //int lNumberOfTestGoals = -1;

    System.out.println("Number of test goals: " + lNumberOfTestGoals);

    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(lFQLSpecification.getCoverageSpecification());

    int lNumberOfCFAInfeasibleGoals = 0;

    ReachedSet lPredicateReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGoalAutomaton = null;

    ReachedSet lGraphReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGraphGoalAutomaton = null;

   // while (lGoalIterator.hasNext()) {
      lIndex++;


      ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();

      System.out.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");


      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton = lGoal.getAutomaton();
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton2 = ToGuardedAutomatonTranslator.removeInfeasibleTransitions(lGoalAutomaton);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton4 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton2);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton3 = ToGuardedAutomatonTranslator.reduceEdgeSets(lGoalAutomaton4);

      //lTimeAccu.proceed();

      boolean lIsCovered = false;

      if (pApplySubsumptionCheck) {
        for (Map.Entry<TestCase, CFAEdge[]> lGeneratedTestCase : mGeneratedTestCases.entrySet()) {
          TestCase lTestCase = lGeneratedTestCase.getKey();

          if (!lTestCase.isPrecise()) {
            throw new RuntimeException();
          }

          ThreeValuedAnswer lCoverageAnswer = FShell3.accepts(lGoalAutomaton3, lGeneratedTestCase.getValue());

          if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
            lIsCovered = true;

            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

            break;
          }
          else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
            GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoalAutomaton3);

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
        System.out.println("Goal #" + lIndex + " is covered by an existing test case!");

        if (!pCheckReachWhenCovered) {
          lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);

          //continue;
        }
      }


      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoalAutomaton3);
      try {
        reconstructPath(null, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
      } catch (InvalidConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (CPAException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ImpreciseExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      //lTimeReach.proceed();



    return null;
      /*Object lCFAPath = reconstructPath(null, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
      boolean lReachableViaGraphSearch = false;

      if (!lAutomatonCPA.getAutomaton().getFinalStates().isEmpty()) {
        lReachableViaGraphSearch = reachGraphSearch(lPreviousGraphGoalAutomaton, lGraphReachedSet, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);

        lPreviousGraphGoalAutomaton = lAutomatonCPA.getAutomaton();
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
        System.out.println("Goal #" + lIndex + " is infeasible!");

        if (lIsCovered) {
          throw new RuntimeException("Inconsistent result of coverage check and reachability analysis!");
        }

        //mInfeasibleGoals.put(lGoal.getAutomaton(), mReachedAutomatonStates);

        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());

        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);
      }
      else {
        lTimeCover.proceed();

        TestCase lTestCase = TestCase.fromCounterexample(lCounterexampleTraceInfo, mLogManager);

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
            System.out.println("Goal #" + lIndex + " is feasible!");

            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

            // we only add precise test cases for coverage analysis
            mGeneratedTestCases.put(lTestCase, lCFAPath);
          }
          else {
            System.err.println("Goal #" + lIndex + " lead to an imprecise execution!");

            lResultFactory.addImpreciseTestCase(lTestCase);
          }
        }
        else {
          System.out.println("Goal #" + lIndex + " is imprecise!");

          lResultFactory.addImpreciseTestCase(lTestCase);
        }

        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
        lTimeCover.pause();
      }
    }

    System.out.println("Number of CFA infeasible test goals: " + lNumberOfCFAInfeasibleGoals);

    System.out.println("Time in reach: " + mTimeInReach.getSeconds());
    System.out.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");

    // TODO remove ... look at statistics
    //System.out.println("#abstraction elements: " + mPredicateCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    //System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);

    FShell3Result lResult = lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));

    /*if (lResult.getNumberOfTestGoals() != lNumberOfTestGoals) {
      throw new RuntimeException();
    }*x/

    System.out.println("Generated Test Cases:");

    for (TestCase lTestCase : lResultFactory.getTestCases()) {
      System.out.println(lTestCase);
    }

    System.out.println("Size of infeasibility cache: " + mInfeasibleGoals.size());

    return lResult;
    */
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
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    CPAAlgorithm lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager);

    PredicateRefiner lRefiner;
    try {
      lRefiner = new PredicateRefiner(lBasicAlgorithm.getCPA());
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

    modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());

    try {
      lAlgorithm.run(pReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException v){

    }

    mTimeInReach.pause();

    return lRefiner.getCounterexampleTraceInfo();
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
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs(),lCFAMap);
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
    }catch (InterruptedException e) {
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

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(/*pTestCase.getInputs()*/null,lCFAMap);
    lComponentAnalyses.add(lInterpreterCPA);


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
    }catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return null;
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
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }


    modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());


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
    }catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    ARTElement lLastARTElement = (ARTElement)pReachedSet.getLastElement();

    CompositeElement lLastElement = (CompositeElement)lLastARTElement.getWrappedElement();
    ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lLastElement.get(lProductAutomatonIndex);

    mTimeInReach.pause();

    return lProductAutomatonElement.isFinalState();
  }
}

