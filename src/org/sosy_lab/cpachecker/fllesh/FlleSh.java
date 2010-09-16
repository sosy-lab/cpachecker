package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
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
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsCPA;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.cfapath.CFAPathStandardElement;
import org.sosy_lab.cpachecker.fllesh.cpa.composite.CompoundCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.composite.CompoundElement;
import org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.fllesh.cpa.productautomaton.ProductAutomatonAcceptingElement;
import org.sosy_lab.cpachecker.fllesh.cpa.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPrettyPrinter;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;
import org.sosy_lab.cpachecker.fllesh.util.profiling.MemoryInfo;
import org.sosy_lab.cpachecker.fllesh.util.profiling.TimeAccumulator;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;

import com.google.common.base.Joiner;

public class FlleSh {
  
  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final ModifiedCPAchecker mCPAchecker;
  private final Wrapper mWrapper;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  
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
  }
  
  public FlleShResult run(String pFQLSpecification) {
    return run(pFQLSpecification, true);
  }
  
  public FlleShResult run(String pFQLSpecification, boolean pApplySubsumptionCheck) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    Task lTask = Task.create(lFQLSpecification, mCoverageSpecificationTranslator);
    
    System.out.println("Number of test goals: " + lTask.getNumberOfTestGoals());
    
    FlleShResult.Factory lResultFactory = FlleShResult.factory(lTask);
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (lTask.hasPassingClause()) {
      lPassingCPA = getAutomatonCPA(lTask.getPassingClause(), mWrapper);
    }
    
    Deque<Goal> lGoals = lTask.toGoals(mWrapper);
    
    int lIndex = 0;
    
    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;
    
    TimeAccumulator lTimeAccu = new TimeAccumulator(2);
    
    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();
    
    while (!lGoals.isEmpty()) {
      lTimeAccu.proceed();
      
      Goal lGoal = lGoals.poll();
      
      int lCurrentGoalNumber = ++lIndex;
      System.out.println("Goal #" + lCurrentGoalNumber);
      
      System.out.println("Memory used: " + MemoryInfo.getUsedMemory());
      
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), StringBasedTestCase.INPUT_FUNCTION_NAME, mWrapper.getReplacedEdges());
      
      lTimeReach.proceed();
      
      CounterexampleTraceInfo lCounterexampleTraceInfo = reach(lAutomatonCPA, mWrapper.getEntry(), lPassingCPA, mConfiguration, mLogManager);
      
      lTimeReach.pause();
      
      boolean lIsFeasible;
      
      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        lIsFeasible = false;
        
        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());
        System.out.println("Goal #" + lCurrentGoalNumber + " is infeasible!");
      }
      else {
        lTimeCover.proceed();
        
        lIsFeasible = true;
        
        Model lCounterexample = lCounterexampleTraceInfo.getCounterexample();
        StringBasedTestCase lTestCase = StringBasedTestCase.fromCounterexample((MathsatModel)lCounterexample, mLogManager);
        
        if (lTestCase.isPrecise()) {
          lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);
          System.out.println("Goal #" + lCurrentGoalNumber + " is feasible!");
          
          if (pApplySubsumptionCheck) {
            removeCoveredGoals(lGoals, lResultFactory, lTestCase, mWrapper, lAutomatonCPA, lPassingCPA, mConfiguration, mLogManager);
          }
        }
        else {
          System.out.println(lTestCase.getInputFunction());
          
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
      
      System.out.println("Memory used (end): " + MemoryInfo.getUsedMemory());
    }
    
    return lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));
  }
  
  public static FlleShResult run(String pSourceFileName, String pFQLSpecification, String pEntryFunction, boolean pApplySubsumptionCheck) {

    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    System.out.println("FQL query: " + lFQLSpecification);
    System.out.println("File: " + pSourceFileName);
    
    
    Configuration lConfiguration = FlleSh.createConfiguration(pSourceFileName, pEntryFunction);

    LogManager lLogManager;
    ModifiedCPAchecker lCPAchecker;
    try {
      lLogManager = new LogManager(lConfiguration);
      // TODO check whether we need ModifiedCPAchecker anymore
      lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    Task lTask = Task.create(lFQLSpecification, lMainFunction);
    
    FlleShResult.Factory lResultFactory = FlleShResult.factory(lTask);
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);
    
    try {
      lWrapper.toDot("test/output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();
    
    GuardedEdgeAutomatonCPA lPassingCPA = null;
    
    if (lTask.hasPassingClause()) {
      System.out.println("PASSING:");
      System.out.println(lPrettyPrinter.printPretty(lTask.getPassingClause()));
      
      lPassingCPA = getAutomatonCPA(lTask.getPassingClause(), lWrapper);
    }
    
    Deque<Goal> lGoals = lTask.toGoals(lWrapper);
    System.out.println("#TEST GOALS: " + lGoals.size());
    
    int lIndex = 0;
    
    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;
    
    TimeAccumulator lTimeAccu = new TimeAccumulator(2);
    
    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();
    
    while (!lGoals.isEmpty()) {
      lTimeAccu.proceed();
      
      Goal lGoal = lGoals.poll();
      
      int lCurrentGoalNumber = ++lIndex;
      System.out.println("Goal #" + lCurrentGoalNumber);
      //System.out.println(lPrettyPrinter.printPretty(lGoal.getPattern()));
      
      System.out.println("Memory used: " + MemoryInfo.getUsedMemory());
      
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton(), StringBasedTestCase.INPUT_FUNCTION_NAME, lWrapper.getReplacedEdges());
      
      lTimeReach.proceed();
      
      CounterexampleTraceInfo lCounterexampleTraceInfo = reach(lAutomatonCPA, lWrapper.getEntry(), lPassingCPA, lConfiguration, lLogManager);
      
      lTimeReach.pause();
      
      boolean lIsFeasible;
      
      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        lIsFeasible = false;
        
        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());
        System.out.println("Goal #" + lCurrentGoalNumber + " is infeasible!");
      }
      else {
        lTimeCover.proceed();
        
        lIsFeasible = true;
        
        Model lCounterexample = lCounterexampleTraceInfo.getCounterexample();
        StringBasedTestCase lTestCase = StringBasedTestCase.fromCounterexample((MathsatModel)lCounterexample, lLogManager);
        //System.out.println(lCounterexample);
        
        if (lTestCase.isPrecise()) {
          lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);
          System.out.println("Goal #" + lCurrentGoalNumber + " is feasible!");
          
          if (pApplySubsumptionCheck) {
            removeCoveredGoals(lGoals, lResultFactory, lTestCase, lWrapper, lAutomatonCPA, lPassingCPA, lConfiguration, lLogManager);
          }
        }
        else {
          System.out.println(lTestCase.getInputFunction());
          
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
      
      System.out.println("Memory used (end): " + MemoryInfo.getUsedMemory());
    }
    
    return lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));
  }
  
  private static CounterexampleTraceInfo reach(GuardedEdgeAutomatonCPA pAutomatonCPA, CFAFunctionDefinitionNode pEntryNode, ConfigurableProgramAnalysis pPassingCPA, Configuration pConfiguration, LogManager pLogManager) {
    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */
    
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA;
    try {
      lLocationCPA = lLocationCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    ConfigurableProgramAnalysis lCallStackCPA;
    try {
      lCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    CPAFactory lSymbPredAbsCPAFactory = SymbPredAbsCPA.factory();
    lSymbPredAbsCPAFactory.setConfiguration(pConfiguration);
    lSymbPredAbsCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lSymbPredAbsCPA;
    try {
      lSymbPredAbsCPA = lSymbPredAbsCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    lCompoundCPAFactory.push(lCallStackCPA, true);
    
    if (pPassingCPA != null) {
      lCompoundCPAFactory.push(pPassingCPA, true);
    }
    
    lCompoundCPAFactory.push(pAutomatonCPA, true);
    
    lCompoundCPAFactory.push(lSymbPredAbsCPA);
    lCompoundCPAFactory.push(ProductAutomatonCPA.getInstance());
    
    AssumeCPA lAssumeCPA = AssumeCPA.getCBMCAssume();
    lCompoundCPAFactory.push(lAssumeCPA);
    
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);
    try {
      lComponentAnalyses.add(lCompoundCPAFactory.createInstance());
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    ConfigurableProgramAnalysis lARTCPA;
    try {
      lARTCPA = getARTCPA(lComponentAnalyses, pConfiguration, pLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    CPAAlgorithm lBasicAlgorithm = new CPAAlgorithm(lARTCPA, pLogManager);
    
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
      lAlgorithm = new CEGARAlgorithm(lBasicAlgorithm, lRefiner, pConfiguration, pLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    Statistics lARTStatistics;
    try {
      lARTStatistics = new ARTStatistics(pConfiguration, pLogManager);
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
    
    return lRefiner.getCounterexampleTraceInfo();
  }

  private static GuardedEdgeAutomatonCPA getAutomatonCPA(ElementaryCoveragePattern pPattern, Wrapper pWrapper) {
    Automaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(pPattern, pWrapper.getAlphaEdge(), pWrapper.getOmegaEdge());
    GuardedEdgeAutomatonCPA lCPA = new GuardedEdgeAutomatonCPA(lAutomaton, StringBasedTestCase.INPUT_FUNCTION_NAME, pWrapper.getReplacedEdges());
    
    return lCPA;
  }
  
  private static ConfigurableProgramAnalysis getARTCPA(List<ConfigurableProgramAnalysis> pComponentCPAs, Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException, CPAException {
    // create composite CPA
    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(pComponentCPAs);
    lCPAFactory.setConfiguration(pConfiguration);
    lCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();
    
    // create ART CPA
    CPAFactory lARTCPAFactory = ARTCPA.factory();
    lARTCPAFactory.setChild(lCPA);
    lARTCPAFactory.setConfiguration(pConfiguration);
    lARTCPAFactory.setLogger(pLogManager);
    
    return lARTCPAFactory.createInstance();
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
      
      lWriter.println("cpas.explicit.threshold = " + Integer.MAX_VALUE);
      
      lWriter.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return lPropertiesFile;
  }
  
  private static ThreeValuedAnswer accepts(Automaton<GuardedEdgeLabel> pAutomaton, CFAEdge[] pCFAPath) {
    Set<Automaton<GuardedEdgeLabel>.State> lCurrentStates = new HashSet<Automaton<GuardedEdgeLabel>.State>();
    Set<Automaton<GuardedEdgeLabel>.State> lNextStates = new HashSet<Automaton<GuardedEdgeLabel>.State>();
    
    lCurrentStates.add(pAutomaton.getInitialState());
    
    boolean lHasPredicates = false;
    
    for (CFAEdge lCFAEdge : pCFAPath) {
      for (Automaton<GuardedEdgeLabel>.State lCurrentState : lCurrentStates) {
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
      
      Set<Automaton<GuardedEdgeLabel>.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }
    
    for (Automaton<GuardedEdgeLabel>.State lCurrentState : lCurrentStates) {
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
  
  private static void removeCoveredGoals(Deque<Goal> pGoals, FlleShResult.Factory pResultFactory, StringBasedTestCase pTestCase, Wrapper pWrapper, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Configuration pConfiguration, LogManager pLogManager) {
    // a) determine cfa path
    CFAEdge[] lCFAPath = getTakenCFAPath(pTestCase, pWrapper, pAutomatonCPA, pPassingCPA, pConfiguration, pLogManager);
    
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
  
  private static CFAEdge[] getTakenCFAPath(StringBasedTestCase pTestCase, Wrapper pWrapper, GuardedEdgeAutomatonCPA pAutomatonCPA, GuardedEdgeAutomatonCPA pPassingCPA, Configuration pConfiguration, LogManager pLogManager) {
    CFAFunctionDefinitionNode lInputFunction = pTestCase.getInputFunctionEntry();
    
    CFAFunctionDefinitionNode lNondetInputFunction = pWrapper.replace(lInputFunction);
    
    // a) determine cfa path
    CFAEdge[] lCFAPath;
    try {
      lCFAPath = reconstructPath(pWrapper.getEntry(), pAutomatonCPA, pPassingCPA, pConfiguration, pLogManager, pWrapper.getOmegaEdge().getSuccessor());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    LinkedList<CFAEdge> lModifiedPath = new LinkedList<CFAEdge>();
    
    // replace cfa edges related to input function by original cfa edges
    
    for (CFAEdge lCFAEdge : lCFAPath) {
      CFANode lPredecessor = lCFAEdge.getPredecessor();
      CFANode lSuccessor = lCFAEdge.getSuccessor();
      
      if (!lSuccessor.getFunctionName().equals(StringBasedTestCase.INPUT_FUNCTION_NAME)) {
        if (lPredecessor.getFunctionName().equals(StringBasedTestCase.INPUT_FUNCTION_NAME)) {
          if (!lCFAEdge.getEdgeType().equals(CFAEdgeType.ReturnEdge)) {
            throw new RuntimeException();
          }
          
          CallToReturnEdge lSummaryEdge = lSuccessor.getEnteringSummaryEdge();
          
          if (lSummaryEdge == null) {
            throw new RuntimeException();
          }
          
          CFAEdge lReplacedEdge = pWrapper.getReplacedEdges().get(lSummaryEdge);
          
          if (lReplacedEdge == null) {
            throw new RuntimeException();
          }
          
          lModifiedPath.add(lReplacedEdge);
        }
        else {
          lModifiedPath.add(lCFAEdge);
        }
      } 
    }
    
    pWrapper.replace(lNondetInputFunction);
    
    return lModifiedPath.toArray(new CFAEdge[lModifiedPath.size()]);
  }
  
  private static CFAEdge[] reconstructPath(CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, Configuration pConfiguration, LogManager pLogManager, CFANode pEndNode) throws InvalidConfigurationException, CPAException {
    // location CPA
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();

    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    
    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lCompoundCPAFactory.push(pPassingAutomatonCPA, true);  
    }
    lCompoundCPAFactory.push(pCoverAutomatonCPA, true);
    

    // call stack CPA
    CPAFactory lCallStackFactory = CallstackCPA.factory();
    ConfigurableProgramAnalysis lCallStackCPA = lCallStackFactory.createInstance();
    lCompoundCPAFactory.push(lCallStackCPA, true);
    
    
    // explicit CPA
    CPAFactory lExplicitCPAFactory = ExplicitAnalysisCPA.factory();
    lExplicitCPAFactory.setConfiguration(pConfiguration);
    lExplicitCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lExplicitCPA = lExplicitCPAFactory.createInstance();
    lCompoundCPAFactory.push(lExplicitCPA);
    
    
    // CFA path CPA
    int lCFAPathCPAIndex = lCompoundCPAFactory.push(CFAPathCPA.getInstance());
    
    
    // product automaton CPA
    int lProductAutomatonCPAIndex = lCompoundCPAFactory.push(ProductAutomatonCPA.getInstance());
    
    
    // assume CPA
    AssumeCPA lAssumeCPA = AssumeCPA.getCBMCAssume();
    lCompoundCPAFactory.push(lAssumeCPA);
    
    
    
    
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);
    lComponentAnalyses.add(lCompoundCPAFactory.createInstance());
    
    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(pConfiguration);
    lCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();
    
    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, pLogManager);

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
  
}
