package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.fllesh.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.fllesh.cpa.composite.CompoundCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.composite.CompoundElement;
import org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.fllesh.cpa.productautomaton.ProductAutomatonAcceptingElement;
import org.sosy_lab.cpachecker.fllesh.cpa.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.SymbPredAbsCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.SymbPredAbsRefiner;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.trace.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;
import org.sosy_lab.cpachecker.fllesh.util.profiling.TimeAccumulator;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time 
 * measurements when invoking FlleSh several times in a unit test.
 * 
 * TODO Passing predicates from one reachability analysis to the next.
 * 
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals. 
 * 
 */

public class FlleSh {
  
  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final ModifiedCPAchecker mCPAchecker;
  private final Wrapper mWrapper;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private final ConfigurableProgramAnalysis mLocationCPA;
  private final ConfigurableProgramAnalysis mCallStackCPA;
  private final AssumeCPA mAssumeCPA;
  private final CFAPathCPA mCFAPathCPA;
  private final ProductAutomatonCPA mProductAutomatonCPA;
  private final SymbPredAbsCPA mSymbPredAbsCPA;
  private final TimeAccumulator mTimeInReach;
  private int mTimesInReach;
  private final GuardedEdgeLabel mAlphaLabel;
  private final GuardedEdgeLabel mOmegaLabel;
  private final GuardedEdgeLabel mInverseAlphaLabel;
  
  public FlleSh(String pSourceFileName, String pEntryFunction) {
    mConfiguration = FlleSh.createConfiguration(pSourceFileName, pEntryFunction);
    
    try {
      mLogManager = new LogManager(mConfiguration);
      mCPAchecker = new ModifiedCPAchecker(mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    
    CFAFunctionDefinitionNode lMainFunction = mCPAchecker.getMainFunction();
    
    /*
     * We have to instantiate mCoverageSpecificationTranslator before the wrapper
     * changes the underlying CFA. FQL specifications are evaluated against the 
     * target graph generated during initialization of mCoverageSpecificationTranslator.
     */
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lMainFunction);

    mWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, mCPAchecker.getCFAMap(), mLogManager);
    
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
    mLocationCPA = new LocationCPA();
    
    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      mCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    // assume CPA
    mAssumeCPA = AssumeCPA.getCBMCAssume();
    
    // cfa path CPA
    mCFAPathCPA = CFAPathCPA.getInstance();
    
    // product automaton CPA
    mProductAutomatonCPA = ProductAutomatonCPA.getInstance();
    
    // symbolic predicate abstraction CPA
    CPAFactory lSymbPredAbsCPAFactory = SymbPredAbsCPA.factory();
    lSymbPredAbsCPAFactory.setConfiguration(mConfiguration);
    lSymbPredAbsCPAFactory.setLogger(mLogManager);
    try {
      mSymbPredAbsCPA = (SymbPredAbsCPA)lSymbPredAbsCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    mTimeInReach = new TimeAccumulator();
    mTimesInReach = 0;
  }
  
  public FlleShResult run(String pFQLSpecification) {
    return run(pFQLSpecification, true, false);
  }
  
  public FlleShResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation) {
    System.out.println("#Location instances: " + LocationElement.NUMBER_OF_INSTANCES);
    
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    System.out.println("Cache hits (1): " + mCoverageSpecificationTranslator.getOverallCacheHits());
    System.out.println("Cache misses (1): " + mCoverageSpecificationTranslator.getOverallCacheMisses());
    
    Task lTask = Task.create(lFQLSpecification, mCoverageSpecificationTranslator);
    
    System.out.println("Cache hits (2): " + mCoverageSpecificationTranslator.getOverallCacheHits());
    System.out.println("Cache misses (2): " + mCoverageSpecificationTranslator.getOverallCacheMisses());
    
    System.out.println("Number of test goals: " + lTask.getNumberOfTestGoals());
    
    FlleShResult.Factory lResultFactory = FlleShResult.factory(lTask);
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (lTask.hasPassingClause()) {
      Automaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lTask.getPassingClause(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      lPassingCPA = new GuardedEdgeAutomatonCPA(lAutomaton, null);
    }
    
    // TODO
    // reorganize test goal enumeration ?
    // create test goal automaton when goal is processed
    // check for coverage at this point of time
    
    TimeAccumulator lToGoalsTime = new TimeAccumulator();
    lToGoalsTime.proceed();
    
    Deque<Goal> lGoals = lTask.toGoals(mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
    
    lToGoalsTime.pause();
    
    System.out.println("Time for creating goals: " + lToGoalsTime.getSeconds() + " s");
        
    int lIndex = 0;
    
    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;
    
    TimeAccumulator lTimeAccu = new TimeAccumulator(2);
    
    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();
    
    //lGoals = GoalReordering.reorder(lGoals);
    
    while (!lGoals.isEmpty()) {
      lTimeAccu.proceed();
      
      Goal lGoal = lGoals.poll();
      
      int lCurrentGoalNumber = ++lIndex;
      System.out.println("Goal #" + lCurrentGoalNumber);
      
      HashSet<Automaton.State> mReachedAutomatonStates = new HashSet<Automaton.State>();
      
      //System.out.println(lGoal.getAutomaton());
      
      //removeInfeasibleTransitions(lGoal.getAutomaton());
      
      //System.out.println(lGoal.getAutomaton());
      
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), mReachedAutomatonStates);
      
      lTimeReach.proceed();
      
      CounterexampleTraceInfo lCounterexampleTraceInfo = reach(lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
      
      lTimeReach.pause();
      
      boolean lIsFeasible;
      
      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        lIsFeasible = false;
        
        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());
        System.out.println("Goal #" + lCurrentGoalNumber + " is infeasible!");
        
        if (pApplyInfeasibilityPropagation) {
          // propagate infeasibility information
          removeTransitiveInfeasibleGoals(lGoal.getAutomaton(), lGoals, mReachedAutomatonStates);
        }
      }
      else {
        lTimeCover.proceed();
        
        lIsFeasible = true;
        
        TestCase lTestCase = TestCase.fromCounterexample(lCounterexampleTraceInfo, mLogManager);
        
        if (lTestCase.isPrecise()) {
          lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);
          System.out.println("Goal #" + lCurrentGoalNumber + " is feasible!");
          
          if (pApplySubsumptionCheck) {
            removeCoveredGoals(lGoals, lResultFactory, lTestCase, mWrapper, lAutomatonCPA, lPassingCPA);
          }
        }
        else {
          lResultFactory.addImpreciseTestCase(lTestCase);
        }
        
        lTimeCover.pause();
      }
      
      if (lIsFeasible) {
        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
      }
      else {
        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);
      }
    }
    
    System.out.println("#Location instances: " + LocationElement.NUMBER_OF_INSTANCES);
    System.out.println("Time in reach: " + mTimeInReach.getSeconds());
    System.out.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");
    
    System.out.println("#abstraction elements: " + mSymbPredAbsCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    //System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);
    
    return lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));
  }

  private CounterexampleTraceInfo reach(GuardedEdgeAutomatonCPA pAutomatonCPA, CFAFunctionDefinitionNode pEntryNode, ConfigurableProgramAnalysis pPassingCPA) {
    mTimeInReach.proceed();
    mTimesInReach++;
    
    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */
    
    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    lCompoundCPAFactory.push(mCallStackCPA, true);
    
    if (pPassingCPA != null) {
      lCompoundCPAFactory.push(pPassingCPA, true);
    }
    
    lCompoundCPAFactory.push(pAutomatonCPA, true);
    
    lCompoundCPAFactory.push(mSymbPredAbsCPA);
    lCompoundCPAFactory.push(mProductAutomatonCPA);
    
    lCompoundCPAFactory.push(mAssumeCPA);
    
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);
    try {
      lComponentAnalyses.add(lCompoundCPAFactory.createInstance());
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

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
    
    SymbPredAbsRefiner lRefiner;
    try {
      lRefiner = new SymbPredAbsRefiner(lBasicAlgorithm.getCPA());
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

    Statistics lARTStatistics;
    try {
      lARTStatistics = new ARTStatistics(mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);

    AbstractElement lInitialElement = lARTCPA.getInitialElement(pEntryNode);
    Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

    ReachedSet lReachedSet = new LocationMappedReachedSet(ReachedSet.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    mTimeInReach.pause();
    
    return lRefiner.getCounterexampleTraceInfo();
  }
  
  private static ThreeValuedAnswer accepts(Automaton<GuardedEdgeLabel> pAutomaton, CFAEdge[] pCFAPath) {
    Set<Automaton.State> lCurrentStates = new HashSet<Automaton.State>();
    Set<Automaton.State> lNextStates = new HashSet<Automaton.State>();
    
    lCurrentStates.add(pAutomaton.getInitialState());
    
    boolean lHasPredicates = false;
    
    for (CFAEdge lCFAEdge : pCFAPath) {
      for (Automaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (pAutomaton.getFinalStates().contains(lCurrentState)) {
          return ThreeValuedAnswer.ACCEPT;
        }
        
        for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState)) {
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
      
      Set<Automaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }
    
    for (Automaton.State lCurrentState : lCurrentStates) {
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
  
  private void removeCoveredGoals(Deque<Goal> pGoals, FlleShResult.Factory pResultFactory, TestCase pTestCase, Wrapper pWrapper, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA) {
    // a) determine cfa path
    CFAEdge[] lCFAPath;
    try {
      lCFAPath = reconstructPath(pTestCase, mWrapper.getEntry(), pAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    HashSet<Goal> lSubsumedGoals = new HashSet<Goal>();
    
    // check whether remaining goals are subsumed by current counter example
    for (Goal lOpenGoal : pGoals) {
      // is goal subsumed by structural path?
      ThreeValuedAnswer lAcceptanceAnswer = accepts(lOpenGoal.getAutomaton(), lCFAPath);
      
      if (lAcceptanceAnswer == ThreeValuedAnswer.ACCEPT) {
        // test case satisfies goal 
        
        // I) remove goal from task list
        lSubsumedGoals.add(lOpenGoal);
        
        // II) log information
        pResultFactory.addFeasibleTestCase(lOpenGoal.getPattern(), pTestCase);
      }
      else if (lAcceptanceAnswer == ThreeValuedAnswer.UNKNOWN) {
        // we need a more expensive subsumption analysis
        // c) check predicate goals for subsumption
        // TODO implement
        
        throw new RuntimeException();
      }
    }
    
    System.out.println("#COVERED GOALS: " + lSubsumedGoals.size());
    
    // remove all subsumed goals
    pGoals.removeAll(lSubsumedGoals);
  }
  
  private CFAEdge[] reconstructPath(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException {
    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lCompoundCPAFactory.push(pPassingAutomatonCPA, true);  
    }
    lCompoundCPAFactory.push(pCoverAutomatonCPA, true);
    
    // call stack CPA
    lCompoundCPAFactory.push(mCallStackCPA, true);
    
    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lCompoundCPAFactory.push(lInterpreterCPA);
    
    // CFA path CPA
    int lCFAPathCPAIndex = lCompoundCPAFactory.push(mCFAPathCPA);
    
    // product automaton CPA
    int lProductAutomatonCPAIndex = lCompoundCPAFactory.push(mProductAutomatonCPA);
    
    // assume CPA
    lCompoundCPAFactory.push(mAssumeCPA);
    
    
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);
    lComponentAnalyses.add(lCompoundCPAFactory.createInstance());
    
    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();
    
    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new LocationMappedReachedSet(ReachedSet.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    Set<AbstractElement> lEndNodes = lReachedSet.getReached(pEndNode);
    
    if (lEndNodes.size() != 1) {
      System.out.println(pTestCase);
      System.out.println(lEndNodes);
      throw new RuntimeException();
    }
    
    CompositeElement lEndNode = (CompositeElement)lEndNodes.iterator().next();
    
    CompoundElement lDataElement = (CompoundElement)lEndNode.get(1);
    
    if (!lDataElement.getSubelement(lProductAutomatonCPAIndex).equals(ProductAutomatonAcceptingElement.getInstance())) {
      throw new RuntimeException();
    }
    
    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lDataElement.getSubelement(lCFAPathCPAIndex);
    
    return lPathElement.toArray();
  }
  
  public static Configuration createConfiguration(String pSourceFile, String pEntryFunction) {
    File lPropertiesFile = FlleSh.createPropertiesFile(pEntryFunction);
    return createConfiguration(Collections.singletonList(pSourceFile), lPropertiesFile.getAbsolutePath());
  }
  
  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) {
    Map<String, String> lCommandLineOptions = new HashMap<String, String>();

    lCommandLineOptions.put("analysis.programNames", Joiner.on(", ").join(pSourceFiles));
    //lCommandLineOptions.put("output.path", "test/output");

    Configuration lConfiguration = null;
    try {
      lConfiguration = new Configuration(pPropertiesFile, lCommandLineOptions);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return lConfiguration;
  }

  private static File createPropertiesFile(String pEntryFunction) {
    if (pEntryFunction == null) {
      throw new IllegalArgumentException("Parameter pEntryFunction is null!");
    }
    
    File lPropertiesFile = null;

    try {

      lPropertiesFile = File.createTempFile("fllesh.", ".properties");
      lPropertiesFile.deleteOnExit();

      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");

      //lWriter.println("log.consoleLevel = ALL");

      lWriter.println("analysis.traversal = topsort");
      lWriter.println("analysis.entryFunction = " + pEntryFunction);

      // we want to use CEGAR algorithm
      lWriter.println("analysis.useRefinement = true");
      lWriter.println("cegar.refiner = " + org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsRefiner.class.getCanonicalName());

      lWriter.println("cpas.symbpredabs.initAllVars = false");
      //lWriter.println("cpas.symbpredabs.noAutoInitPrefix = __BLAST_NONDET");
      lWriter.println("cpas.symbpredabs.blk.useCache = false");
      //lWriter.println("cpas.symbpredabs.mathsat.lvalsAsUIFs = true");
      // we need theory combination for example for using uninterpreted functions used in conjunction with linear arithmetic (correctly)
      // TODO caution: using dtc changes the results ... WRONG RESULTS !!!
      //lWriter.println("cpas.symbpredabs.mathsat.useDtc = true");
      
      lWriter.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return lPropertiesFile;
  }
  
  private boolean isTransitivelyInfeasible(Automaton<GuardedEdgeLabel> pInfeasibleAutomaton, Automaton<GuardedEdgeLabel> pOtherAutomaton, Collection<Automaton.State> pReachedAutomatonStates) {
    // When all reached states are contained in the similar states than pOtherAutomaton has to be infeasible, too.
    return getSimilarStates(pInfeasibleAutomaton, pOtherAutomaton).containsAll(pReachedAutomatonStates);
  }
  
  private Collection<Automaton.State> getSimilarStates(Automaton<GuardedEdgeLabel> pInfeasibleAutomaton, Automaton<GuardedEdgeLabel> pOtherAutomaton) {
    Pair<Automaton.State, Automaton.State> lInitialPair = new Pair<Automaton.State, Automaton.State>(pInfeasibleAutomaton.getInitialState(), pOtherAutomaton.getInitialState());
    
    LinkedList<Pair<Automaton.State, Automaton.State>> lWorklist = new LinkedList<Pair<Automaton.State, Automaton.State>>();
    lWorklist.add(lInitialPair);
    
    Multimap<Automaton.State, Automaton.State> lCore = HashMultimap.create();
    Multimap<Automaton.State, Automaton.State> lFrontier = HashMultimap.create();
    
    while (!lWorklist.isEmpty()) {
      Pair<Automaton.State, Automaton.State> lCurrentPair = lWorklist.removeFirst();
      
      if (lCore.containsEntry(lCurrentPair.getFirst(), lCurrentPair.getSecond())
          || lFrontier.containsEntry(lCurrentPair.getFirst(), lCurrentPair.getSecond())) {
        continue;
      }
      
      boolean lSimilar = true;
      
      HashSet<Pair<Automaton.State, Automaton.State>> lPotentialWork = new HashSet<Pair<Automaton.State, Automaton.State>>();
      
      for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pInfeasibleAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
        boolean lOneDirectionSimilar = false;
        
        for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge2 : pOtherAutomaton.getOutgoingEdges(lCurrentPair.getSecond())) {
          if (lOutgoingEdge.getLabel().equals(lOutgoingEdge2.getLabel())) {
            lPotentialWork.add(new Pair<Automaton.State, Automaton.State>(lOutgoingEdge.getTarget(), lOutgoingEdge2.getTarget()));
            lOneDirectionSimilar = true;
          }
        }
        
        if (!lOneDirectionSimilar) {
          lSimilar = false;
        }
      }
      
      for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pOtherAutomaton.getOutgoingEdges(lCurrentPair.getSecond())) {
        boolean lOneDirectionSimilar = false;
        
        for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge2 : pInfeasibleAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
          if (lOutgoingEdge.getLabel().equals(lOutgoingEdge2.getLabel())) {
            lPotentialWork.add(new Pair<Automaton.State, Automaton.State>(lOutgoingEdge2.getTarget(), lOutgoingEdge.getTarget()));
            lOneDirectionSimilar = true;
          }
        }
        
        if (!lOneDirectionSimilar) {
          lSimilar = false;
        }
      }
      
      if (lSimilar) {
        lCore.put(lCurrentPair.getFirst(), lCurrentPair.getSecond());
        lWorklist.addAll(lPotentialWork);
      }
      else {
        lFrontier.put(lCurrentPair.getFirst(), lCurrentPair.getSecond());
      }
    }
    
    /*if (lCore.keySet().size() >= 4) {
      System.out.println(pInfeasibleAutomaton.toString());
      System.out.println("---");
      System.out.println(pOtherAutomaton.toString());
      throw new RuntimeException();
    }*/
    
    return lCore.keySet();
    
    /*HashSet<Automaton<GuardedEdgeLabel>.State> lSimilarStates = new HashSet<Automaton<GuardedEdgeLabel>.State>();
    
    if (lCore.size() > 1) {
      System.out.println(lCore);
      System.out.println(lFrontier);
      throw new RuntimeException();
    }
    
    for (Automaton<GuardedEdgeLabel>.State lState : lCore.keySet()) {
      if (!lFrontier.containsKey(lState)) {
        lSimilarStates.add(lState);
      }
    }
    
    return lSimilarStates;*/
  }
  
  private void removeTransitiveInfeasibleGoals(Automaton<GuardedEdgeLabel> pInfeasibleAutomaton, Deque<Goal> pGoals, Collection<Automaton.State> pReachedAutomatonStates) {
    HashSet<Goal> lSubsumedGoals = new HashSet<Goal>();
    
    //System.out.println(pAutomaton.toString());
    //System.out.println("---");
    //System.out.println(pReachedAutomatonStates);
    //System.out.println("---");
    
    /*if (pGoals.size() == 100) {
      System.out.println(pAutomaton.toString());
      System.out.println("---");
      System.out.println(pReachedAutomatonStates);
      System.out.println("---");
    }*/
    
    if (pReachedAutomatonStates.size() <= 3) {
      System.out.println(pInfeasibleAutomaton.toString());
      System.out.println("---");
      System.out.println(pReachedAutomatonStates);
      System.out.println("---");
      throw new RuntimeException();
    }
    
    // check whether remaining goals are subsumed by current counter example
    for (Goal lOpenGoal : pGoals) {
      if (isTransitivelyInfeasible(pInfeasibleAutomaton, lOpenGoal.getAutomaton(), pReachedAutomatonStates)) {
        lSubsumedGoals.add(lOpenGoal);
      }
    }
    
    System.out.println("Removing " + lSubsumedGoals.size() + " many infeasible test goals!");
    
    pGoals.removeAll(lSubsumedGoals);
  }
  
  // TODO reimplement without modifying the old automaton
  /*
  public void removeInfeasibleTransitions(Automaton<GuardedEdgeLabel> pAutomaton) {
    HashSet<Automaton<GuardedEdgeLabel>.Edge> lEdgesToBeRemoved = new HashSet<Automaton<GuardedEdgeLabel>.Edge>();
    
    //for (Automaton<GuardedEdgeLabel>.Edge lEdge : pAutomaton.getEdges()) {
    for (Automaton<GuardedEdgeLabel>.Edge lEdge : pAutomaton.edges()) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();
      
      if (lLabel.getClass().equals(GuardedEdgeLabel.class)) {
        if (lLabel.getEdgeSet().size() == 1) {
          // currently we only simplify singleton edge sets
          CFAEdge lCFAEdge = lLabel.getEdgeSet().iterator().next();
          
          boolean lKeep = false; // falsch
          
          int lRelevantEdgesCounter = 0;
          
          for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lEdge.getTarget())) {
            GuardedEdgeLabel lOutgoingLabel = lOutgoingEdge.getLabel();
            
            if (lOutgoingLabel.getClass().equals(GuardedEdgeLabel.class)) {
              lRelevantEdgesCounter++;
              
              for (CFAEdge lOutgoingCFAEdge : lOutgoingLabel.getEdgeSet()) {
                if (lCFAEdge.getSuccessor().equals(lOutgoingCFAEdge.getPredecessor())) {
                  lKeep = true;
                  break;
                }
              }
            }
            
            if (lKeep) {
              break;
            }
          }
          
          if (!lKeep && lRelevantEdgesCounter > 0) {
            lEdgesToBeRemoved.add(lEdge);
          }
        }
      }
    }
    
    for (Automaton<GuardedEdgeLabel>.Edge lEdge : lEdgesToBeRemoved) {
      pAutomaton.remove(lEdge);
    }
  }*/
  
}
