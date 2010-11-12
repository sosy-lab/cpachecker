package org.sosy_lab.cpachecker.fshell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFACreator;
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
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonAcceptingElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.cpa.symbpredabsCPA.NonabstractionElement;
import org.sosy_lab.cpachecker.fshell.cpa.symbpredabsCPA.SymbPredAbsCPA;
import org.sosy_lab.cpachecker.fshell.cpa.symbpredabsCPA.SymbPredAbsRefiner;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time 
 * measurements when invoking FlleSh several times in a unit test.
 * 
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals. 
 * 
 */

public class FlleSh {
  
  private final Configuration mConfiguration;
  private final LogManager mLogManager;
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
  private final Map<TestCase, CFAEdge[]> mGeneratedTestCases;
  
  private final Map<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, Collection<NondeterministicFiniteAutomaton.State>> mInfeasibleGoals;
  
  public static Map<String, CFAFunctionDefinitionNode> getCFAMap(String pSourceFileName, Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    // parse code file
    IASTTranslationUnit lAst = null;
    try {
      lAst = CParser.parseFile(pSourceFileName, CParser.Dialect.GNUC);
    } catch (Exception e) {
      e.printStackTrace();
      
      throw new RuntimeException(e);
    }

    CFACreator lCFACreator = new CFACreator(pConfiguration, pLogManager);
    lCFACreator.createCFA(lAst);

    return lCFACreator.getFunctions();
  }
  
  public FlleSh(String pSourceFileName, String pEntryFunction) {
    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;
    
    try {
      mConfiguration = FlleSh.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new LogManager(mConfiguration);
      
      lCFAMap = FlleSh.getCFAMap(pSourceFileName, mConfiguration, mLogManager);
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
      mLocationCPA = LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
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
    
    // we can collect test cases accross several run invocations and use them for coverage analysis
    // TODO output test cases from an earlier run
    mGeneratedTestCases = new HashMap<TestCase, CFAEdge[]>();
    
    mInfeasibleGoals = new HashMap<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, Collection<NondeterministicFiniteAutomaton.State>>();
  }
  
  public FlleShResult run(String pFQLSpecification) {
    return run(pFQLSpecification, true, false, false, false, true);
  }
  
  public FlleShResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic) {
    if (pGenerateTestGoalAutomataInAdvance) {
      return run2(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation);
    }
    else {
      return run_incremental(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckCorrectnessOfCoverageCheck, pPedantic);
    }
  }
  
  public void throwAwayTestCases() {
    mGeneratedTestCases.clear();
  }
  
  public void loadTestCases(File pFile) {
    // TODO load test cases from file
    // TODO add loaded test cases into mGeneratedTestCases
  }
  
  public void saveTestCases(File pFile) {
    // TODO write test cases in mGeneratedTestCases into pFile
  }
  
  public void checkCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    checkCoverage(lFQLSpecification, pTestSuite, pPedantic);
  }
  
  public void checkCoverage(FQLSpecification pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic) {
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (pFQLSpecification.hasPassingClause()) {
      ElementaryCoveragePattern lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(pFQLSpecification.getPathPattern());
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      lPassingCPA = new GuardedEdgeAutomatonCPA(lAutomaton, null);
    }
    
    IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);
    
    int lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLSpecification.getCoverageSpecification());
    
    System.out.println("Number of test goals: " + lNumberOfTestGoals);
    
    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(pFQLSpecification.getCoverageSpecification());
    
    int lIndex = 0;
    
    int lNumberOfCoveredTestGoals = 0;
    
    Map<TestCase, CFAEdge[]> mPathCache = new HashMap<TestCase, CFAEdge[]>();
    
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
    GuardedEdgeAutomatonCPA lIdStarCPA = new GuardedEdgeAutomatonCPA(lAutomaton, null);
    
    while (lGoalIterator.hasNext()) {
      lIndex++;
      
      ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();
      
      System.out.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");
      
      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), new HashSet<NondeterministicFiniteAutomaton.State>());
      
      boolean lIsCovered = false;
      
      for (TestCase lTestCase : pTestSuite) {
        if (!lTestCase.isPrecise()) {
          throw new RuntimeException();
        }
        
        CFAEdge[] lCFAPath = mPathCache.get(lTestCase);
        
        boolean lIsPrecise = true;
        
        if (lCFAPath == null) {
          try {
            lCFAPath = reconstructPath(lTestCase, mWrapper.getEntry(), lIdStarCPA, null, mWrapper.getOmegaEdge().getSuccessor());
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
            mPathCache.put(lTestCase, lCFAPath);
          }
        }
        
        ThreeValuedAnswer lCoverageAnswer = accepts(lGoal.getAutomaton(), lCFAPath); 
        
        if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
          lIsCovered = true;
          
          break;
        }
        else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
          try {
            if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
              lIsCovered = true;
              
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
      
      if (lIsCovered) {
        System.out.println("COVERED");
        
        lNumberOfCoveredTestGoals++;
      }
    }
    
    System.out.println("Coverage: " + ((double)lNumberOfCoveredTestGoals)/((double)lIndex));
  }
  
  public FlleShResult run_incremental(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered, boolean pPedantic) {
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
    
    FlleShResult.Factory lResultFactory = FlleShResult.factory();
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (lPassingClause != null) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      lPassingCPA = new GuardedEdgeAutomatonCPA(lAutomaton, null);
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
    
    int lRemovedByInfeasibilityPropagation = 0;
    
    while (lGoalIterator.hasNext()) {
      lIndex++;
      
      ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();
      
      System.out.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");
      
      lTimeAccu.proceed();
      
      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
      
      boolean lIsCovered = false;
      
      if (pApplySubsumptionCheck) {
        for (Map.Entry<TestCase, CFAEdge[]> lGeneratedTestCase : mGeneratedTestCases.entrySet()) {
          TestCase lTestCase = lGeneratedTestCase.getKey();
          
          if (!lTestCase.isPrecise()) {
            throw new RuntimeException();
          }
          
          ThreeValuedAnswer lCoverageAnswer = accepts(lGoal.getAutomaton(), lGeneratedTestCase.getValue()); 
          
          if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
            lIsCovered = true;
            
            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);
            
            break;
          }
          else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
            GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), new HashSet<NondeterministicFiniteAutomaton.State>());
            
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
        
        /*if (!lIsCovered) {
          // apply infeasibility check
          if (lPassingCPA == null) {
            // as a first step consider only cases when no passing clause is given
            
            //Iterator<Collection<Automaton.State>> lStatesIterator = mInfeasibleGoals_States.iterator(); 
            
            for (Map.Entry<Automaton<GuardedEdgeLabel>, Collection<Automaton.State>> lEntry : mInfeasibleGoals.entrySet()) {
            //for (Pair<Automaton<GuardedEdgeLabel>, Collection<Automaton.State>> lEntry : mInfeasibleGoals) {
            //for (Automaton<GuardedEdgeLabel> lInfeasibleAutomaton : mInfeasibleGoals_Automata) {
              //Collection<Automaton.State> lStates = mInfeasibleGoals_States.iterator().ne
              //Collection<Automaton.State> lStates = lStatesIterator.next();
              if (isTransitivelyInfeasible(lEntry.getKey(), lGoal.getAutomaton(), lEntry.getValue())) {
                //throw new RuntimeException();
                
                lResultFactory.addInfeasibleTestCase(lGoal.getPattern());
                
                // TODO with this change the consistency check should not work anymore 
                lIsCovered = true;
                
                lRemovedByInfeasibilityPropagation++;
                
                break;
              }
            }
          }
          else {
            // TODO implement general case
            
          }
        }*/
      }
      
      if (lIsCovered) {
        System.out.println("Goal #" + lIndex + " is covered by an existing test case!");
        
        if (!pCheckReachWhenCovered) {
          lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
          
          continue;
        }
      }
      
      // TODO remove
      HashSet<NondeterministicFiniteAutomaton.State> mReachedAutomatonStates = new HashSet<NondeterministicFiniteAutomaton.State>();
      
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), mReachedAutomatonStates);
      
      lTimeReach.proceed();
      
      CounterexampleTraceInfo lCounterexampleTraceInfo = reach(lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);
      
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
    
    System.out.println("Time in reach: " + mTimeInReach.getSeconds());
    System.out.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");
    
    System.out.println("#abstraction elements: " + mSymbPredAbsCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);
    
    FlleShResult lResult = lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot)); 
    
    /*if (lResult.getNumberOfTestGoals() != lNumberOfTestGoals) {
      throw new RuntimeException();
    }*/
    
    System.out.println("Generated Test Cases:");
    
    for (TestCase lTestCase : lResultFactory.getTestCases()) {
      System.out.println(lTestCase);
    }

    System.out.println("Removed by infeasibility propagation: " + lRemovedByInfeasibilityPropagation);

    System.out.println("Size of infeasibility cache: " + mInfeasibleGoals.size());
    
    return lResult;
  }
  
  public FlleShResult run2(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation) {
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
    
    FlleShResult.Factory lResultFactory = FlleShResult.factory();
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (lTask.hasPassingClause()) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lTask.getPassingClause(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
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
      
      HashSet<NondeterministicFiniteAutomaton.State> mReachedAutomatonStates = new HashSet<NondeterministicFiniteAutomaton.State>();
      
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
            try {
              removeCoveredGoals(lGoals, lResultFactory, lTestCase, mWrapper, lAutomatonCPA, lPassingCPA);
            } catch (ImpreciseExecutionException e) {
              // TODO implement proper handling 
              throw new RuntimeException(e);
            }
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
    
    System.out.println("Time in reach: " + mTimeInReach.getSeconds());
    System.out.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");
    
    System.out.println("#abstraction elements: " + mSymbPredAbsCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);
    
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
    
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);
    
    lComponentAnalyses.add(mCallStackCPA);
    
    if (pPassingCPA != null) {
      lComponentAnalyses.add(pPassingCPA);
    }
    
    lComponentAnalyses.add(pAutomatonCPA);
    
    lComponentAnalyses.add(mSymbPredAbsCPA);
    lComponentAnalyses.add(mProductAutomatonCPA);
    
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
      lARTStatistics = new ARTStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);

    AbstractElement lInitialElement = lARTCPA.getInitialElement(pEntryNode);
    Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);
    
    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    mTimeInReach.pause();
    
    return lRefiner.getCounterexampleTraceInfo();
  }
  
  private static ThreeValuedAnswer accepts(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, CFAEdge[] pCFAPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<NondeterministicFiniteAutomaton.State>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<NondeterministicFiniteAutomaton.State>();
    
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
  
  private void removeCoveredGoals(Deque<Goal> pGoals, FlleShResult.Factory pResultFactory, TestCase pTestCase, Wrapper pWrapper, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA) throws ImpreciseExecutionException {
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

  private boolean checkCoverage(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);
    
    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lComponentAnalyses.add(pPassingAutomatonCPA);
    }
    
    lComponentAnalyses.add(pCoverAutomatonCPA);
    
    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);
    
    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);
    
    // product automaton CPA
    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mProductAutomatonCPA);
    
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
    }
    
    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();
    
    if (lEndNode == null) {
      return false;
    }
    else {
      if (((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
        // location of last element is at end node

        return lEndNode.get(lProductAutomatonCPAIndex).equals(ProductAutomatonAcceptingElement.getInstance());
      }
      
      return false;
    }
  }
  
  private CFAEdge[] reconstructPath(TestCase pTestCase, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);
    
    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lComponentAnalyses.add(pPassingAutomatonCPA);
    }
    
    lComponentAnalyses.add(pCoverAutomatonCPA);
    
    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);
    
    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);
    
    // CFA path CPA
    int lCFAPathCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mCFAPathCPA);
    
    // product automaton CPA
    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mProductAutomatonCPA);
    
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
    }
    
    // TODO sanity check by assertion
    CompositeElement lEndNode = (CompositeElement)lReachedSet.getLastElement();
    
    if (lEndNode == null) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }
    
    if (!((LocationElement)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }
    
    if (!lEndNode.get(lProductAutomatonCPAIndex).equals(ProductAutomatonAcceptingElement.getInstance())) {
      throw new RuntimeException();
    }
    
    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lEndNode.get(lCFAPathCPAIndex);
    
    return lPathElement.toArray();
  }
  
  public static Configuration createConfiguration(String pSourceFile, String pEntryFunction) throws InvalidConfigurationException {
    File lPropertiesFile = FlleSh.createPropertiesFile(pEntryFunction);
    return createConfiguration(Collections.singletonList(pSourceFile), lPropertiesFile.getAbsolutePath());
  }
  
  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) throws InvalidConfigurationException {
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
  
  private boolean isTransitivelyInfeasible(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pInfeasibleAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pOtherAutomaton, Collection<NondeterministicFiniteAutomaton.State> pReachedAutomatonStates) {
    // When all reached states are contained in the similar states than pOtherAutomaton has to be infeasible, too.
    return getSimilarStates(pInfeasibleAutomaton, pOtherAutomaton).containsAll(pReachedAutomatonStates);
  }
  
  private Collection<NondeterministicFiniteAutomaton.State> getSimilarStates(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pInfeasibleAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pOtherAutomaton) {
    Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lInitialPair = new Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>(pInfeasibleAutomaton.getInitialState(), pOtherAutomaton.getInitialState());
    
    LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lWorklist = new LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();
    lWorklist.add(lInitialPair);
    
    Multimap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lCore = HashMultimap.create();
    Multimap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lFrontier = HashMultimap.create();
    
    //HashSet<Pair<Automaton.State, Automaton.State>> lPotentialWork = new HashSet<Pair<Automaton.State, Automaton.State>>();
    
    while (!lWorklist.isEmpty()) {
      Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lCurrentPair = lWorklist.removeFirst();
      
      if (lCore.containsEntry(lCurrentPair.getFirst(), lCurrentPair.getSecond())
          || lFrontier.containsEntry(lCurrentPair.getFirst(), lCurrentPair.getSecond())) {
        continue;
      }
      
      boolean lSimilar = true;
      
      //lPotentialWork.clear();
      HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lPotentialWork = new HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();
      
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pInfeasibleAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
        boolean lOneDirectionSimilar = false;
        
        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge2 : pOtherAutomaton.getOutgoingEdges(lCurrentPair.getSecond())) {
          if (lOutgoingEdge.getLabel().equals(lOutgoingEdge2.getLabel())) {
            lPotentialWork.add(new Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>(lOutgoingEdge.getTarget(), lOutgoingEdge2.getTarget()));
            lOneDirectionSimilar = true;
          }
        }
        
        if (!lOneDirectionSimilar) {
          lSimilar = false;
        }
      }
      
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pOtherAutomaton.getOutgoingEdges(lCurrentPair.getSecond())) {
        boolean lOneDirectionSimilar = false;
        
        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge2 : pInfeasibleAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
          if (lOutgoingEdge.getLabel().equals(lOutgoingEdge2.getLabel())) {
            lPotentialWork.add(new Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>(lOutgoingEdge2.getTarget(), lOutgoingEdge.getTarget()));
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
  
  private void removeTransitiveInfeasibleGoals(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pInfeasibleAutomaton, Deque<Goal> pGoals, Collection<NondeterministicFiniteAutomaton.State> pReachedAutomatonStates) {
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
