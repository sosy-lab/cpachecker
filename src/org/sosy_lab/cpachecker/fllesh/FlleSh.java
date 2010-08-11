package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitAnalysisElement;
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
import org.sosy_lab.cpachecker.fllesh.util.Automaton;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatValue;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;

import com.google.common.base.Joiner;

public class FlleSh {

  public static FlleShResult run(String pSourceFileName, String pFQLSpecification, String pEntryFunction, boolean pApplySubsumptionCheck) {

    File lPropertiesFile = Main.createPropertiesFile(pEntryFunction);
    Configuration lConfiguration = Main.createConfiguration(pSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager;
    ModifiedCPAchecker lCPAchecker;
    try {
      lLogManager = new LogManager(lConfiguration);
      lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    System.out.println("FQL query: " + lFQLSpecification);
    System.out.println("File: " + pSourceFileName);
    
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
    
    System.out.println("TEST GOALS:");
    
    LinkedList<Goal> lGoals = new LinkedList<Goal>();
    
    for (ElementaryCoveragePattern lGoalPattern : lTask) {
      Goal lGoal = new Goal(lGoalPattern, lWrapper);
      lGoals.add(lGoal);
    }
    
    System.out.println(lGoals.size());
    
    int lIndex = 0;
    
    while (!lGoals.isEmpty()) {
      Goal lGoal = lGoals.pollFirst();
      
      int lCurrentGoalNumber = ++lIndex;
      
      System.out.println("Goal #" + lCurrentGoalNumber);
      System.out.println(lPrettyPrinter.printPretty(lGoal.getPattern()));
      
      CPAFactory lLocationCPAFactory = LocationCPA.factory();
      ConfigurableProgramAnalysis lLocationCPA;
      try {
        lLocationCPA = lLocationCPAFactory.createInstance();
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException(e);
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }

      CPAFactory lSymbPredAbsCPAFactory = SymbPredAbsCPA.factory();
      lSymbPredAbsCPAFactory.setConfiguration(lConfiguration);
      lSymbPredAbsCPAFactory.setLogger(lLogManager);
      ConfigurableProgramAnalysis lSymbPredAbsCPA;
      try {
        lSymbPredAbsCPA = lSymbPredAbsCPAFactory.createInstance();
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException(e);
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }
      
      CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
      
      lCompoundCPAFactory.push(lSymbPredAbsCPA);
      lCompoundCPAFactory.push(ProductAutomatonCPA.getInstance());
      
      if (lTask.hasPassingClause()) {
        lCompoundCPAFactory.push(lPassingCPA, true);
      }
      
      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoal.getAutomaton());
      lCompoundCPAFactory.push(lAutomatonCPA, true);
      
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
        lARTCPA = getARTCPA(lComponentAnalyses, lConfiguration, lLogManager);
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException(e);
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }

      CPAAlgorithm lBasicAlgorithm = new CPAAlgorithm(lARTCPA, lLogManager);
      
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
        lAlgorithm = new CEGARAlgorithm(lBasicAlgorithm, lRefiner, lConfiguration, lLogManager);
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException(e);
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }

      Statistics lARTStatistics;
      try {
        lARTStatistics = new ARTStatistics(lConfiguration, lLogManager);
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException(e);
      }
      Set<Statistics> lStatistics = new HashSet<Statistics>();
      lStatistics.add(lARTStatistics);
      lAlgorithm.collectStatistics(lStatistics);

      AbstractElement lInitialElement = lARTCPA.getInitialElement(lWrapper.getEntry());
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(lWrapper.getEntry());

      ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.TOPSORT, true);
      lReachedElements.add(lInitialElement, lInitialPrecision);

      try {
        lAlgorithm.run(lReachedElements, true);
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }

      //System.out.println(lReachedElements);
      
      CounterexampleTraceInfo lCounterexampleTraceInfo = lRefiner.getCounterexampleTraceInfo();
      
      // TODO remove in future ... here for debugging purposes
      boolean lIsFeasible;
      try {
        lIsFeasible = Main.determineGoalFeasibility(lReachedElements, lARTStatistics);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      
      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        if (lIsFeasible) {
          throw new RuntimeException("Inconsitent result!");
        }
        
        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());
        System.out.println("Goal #" + lCurrentGoalNumber + " is infeasible!");
      }
      else {
        if (!lIsFeasible) {
          throw new RuntimeException("Inconsitent result!");
        }
        
        Model lCounterexample = lCounterexampleTraceInfo.getCounterexample();
        
        System.out.println(lCounterexample);
        
        lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lCounterexample);
        System.out.println("Goal #" + lCurrentGoalNumber + " is feasible!");
        
        
        if (pApplySubsumptionCheck) {
          /** goal subsumption check */
          
          // a) determine cfa path
          CFAEdge[] lCFAPath;
          if (lTask.hasPassingClause()) {
            try {
              lCFAPath = reconstructPath(lCounterexample, lWrapper.getEntry(), lAutomatonCPA, lPassingCPA, lConfiguration, lLogManager, lWrapper.getOmegaEdge().getSuccessor());
            } catch (InvalidConfigurationException e) {
              throw new RuntimeException(e);
            } catch (CPAException e) {
              throw new RuntimeException(e);
            }
          }
          else {
            try {
              lCFAPath = reconstructPath(lCounterexample, lWrapper.getEntry(), lAutomatonCPA, lConfiguration, lLogManager, lWrapper.getOmegaEdge().getSuccessor());
            } catch (InvalidConfigurationException e) {
              throw new RuntimeException(e);
            } catch (CPAException e) {
              throw new RuntimeException(e);
            }
          }
          
          HashSet<Goal> lSubsumedGoals = new HashSet<Goal>();
          
          // check whether remaining goals are subsumed by current counter example
          for (Goal lOpenGoal : lGoals) {
            // is goal subsumed by structural path?
            ThreeValuedAnswer lAcceptanceAnswer = accepts(lOpenGoal.getAutomaton(), lCFAPath);
            
            if (lAcceptanceAnswer == ThreeValuedAnswer.ACCEPT) {
              // test case satisfies goal 
              
              // I) remove goal from task list
              lSubsumedGoals.add(lOpenGoal);
              
              // II) log information
              lResultFactory.addFeasibleTestCase(lOpenGoal.getPattern(), lCounterexample);
              
              System.out.println("SUBSUMED");
            }
            else if (lAcceptanceAnswer == ThreeValuedAnswer.UNKNOWN) {
              // we need a more expensive subsumption analysis
              // c) check predicate goals for subsumption
              // TODO implement
              
              throw new RuntimeException();
            }
          }
          
          // remove all subsumed goals
          lGoals.removeAll(lSubsumedGoals);
        }
      }
    }
    
    return lResultFactory.create();
  }
  

  private static GuardedEdgeAutomatonCPA getAutomatonCPA(ElementaryCoveragePattern pPattern, Wrapper pWrapper) {
    Automaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(pPattern, pWrapper.getAlphaEdge(), pWrapper.getOmegaEdge());
    GuardedEdgeAutomatonCPA lCPA = new GuardedEdgeAutomatonCPA(lAutomaton);
    
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
  
  private static boolean determineGoalFeasibility(ReachedElements pReachedElements, Statistics pStatistics) throws IOException {
    boolean lErrorReached = false;
    
    for (AbstractElement lReachedElement : pReachedElements) {
      if ((lReachedElement instanceof Targetable) && ((Targetable)lReachedElement).isTarget()) {
        lErrorReached = true;
      }

      //System.out.println(reachedElement);
    }

    PrintWriter lStatisticsWriter = new PrintWriter(System.out);

    if (lErrorReached) {
      pStatistics.printStatistics(lStatisticsWriter, Result.UNSAFE, pReachedElements);
      
      return true;
    }
    else {
      pStatistics.printStatistics(lStatisticsWriter, Result.SAFE, pReachedElements);
      
      return false;
    }
  }
  
  private static Configuration createConfiguration(String pSourceFile, String pPropertiesFile) {
    return createConfiguration(Collections.singletonList(pSourceFile), pPropertiesFile);
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
      lWriter.println("cpas.symbpredabs.mathsat.lvalsAsUIFs = true");
      // we need theory combination for example for using uninterpreted functions used in conjunction with linear arithmetic (correctly)
      lWriter.println("cpas.symbpredabs.mathsat.useDtc = true");
      
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
  
  private static CFAEdge[] reconstructPath(Model pCounterexample, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, Configuration pConfiguration, LogManager pLogManager, CFANode pEndNode) throws InvalidConfigurationException, CPAException {
    
    System.out.println(pCounterexample);
    
    // location CPA
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();

    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    // explicit cpa
    CPAFactory lExplicitCPAFactory = ExplicitAnalysisCPA.factory();
    lExplicitCPAFactory.setConfiguration(pConfiguration);
    lExplicitCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lExplicitCPA = lExplicitCPAFactory.createInstance();
    lCompoundCPAFactory.push(lExplicitCPA);
    
    lCompoundCPAFactory.push(CFAPathCPA.getInstance());
    
    // automaton cpas
    lCompoundCPAFactory.push(ProductAutomatonCPA.getInstance());
    lCompoundCPAFactory.push(pCoverAutomatonCPA, true);
    
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
    
    Set<AbstractElement> lEndNodes = getFinalStates(pCounterexample, lAlgorithm, pEntry, pEndNode);
    
    if (lEndNodes.size() != 1) {
      System.out.println(lEndNodes);
      throw new RuntimeException();
      // TODO add proper handling
      //return new CFAEdge[0];
    }
    
    CompositeElement lEndNode = (CompositeElement)lEndNodes.iterator().next();
    
    CompoundElement lDataElement = (CompoundElement)lEndNode.get(1);
    
    if (!lDataElement.getSubelement(2).equals(ProductAutomatonAcceptingElement.getInstance())) {
      throw new RuntimeException();
    }
    
    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lDataElement.getSubelement(1);
    
    return lPathElement.toArray();
  }
  
  private static CFAEdge[] reconstructPath(Model pCounterexample, CFAFunctionDefinitionNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, Configuration pConfiguration, LogManager pLogManager, CFANode pEndNode) throws InvalidConfigurationException, CPAException {
    // location CPA
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();

    CompoundCPA.Factory lCompoundCPAFactory = new CompoundCPA.Factory();
    
    // explicit cpa
    CPAFactory lExplicitCPAFactory = ExplicitAnalysisCPA.factory();
    lExplicitCPAFactory.setConfiguration(pConfiguration);
    lExplicitCPAFactory.setLogger(pLogManager);
    ConfigurableProgramAnalysis lExplicitCPA = lExplicitCPAFactory.createInstance();
    lCompoundCPAFactory.push(lExplicitCPA);
    
    lCompoundCPAFactory.push(CFAPathCPA.getInstance());
    
    // automaton cpas
    lCompoundCPAFactory.push(ProductAutomatonCPA.getInstance());
    lCompoundCPAFactory.push(pPassingAutomatonCPA, true);
    lCompoundCPAFactory.push(pCoverAutomatonCPA, true);
    
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

    Set<AbstractElement> lEndNodes = getFinalStates(pCounterexample, lAlgorithm, pEntry, pEndNode);
    
    if (lEndNodes.size() != 1) {
      throw new RuntimeException();
    }
    
    CompositeElement lEndNode = (CompositeElement)lEndNodes.iterator().next();
    
    CompoundElement lDataElement = (CompoundElement)lEndNode.get(1);
    
    if (!lDataElement.getSubelement(2).equals(ProductAutomatonAcceptingElement.getInstance())) {
      throw new RuntimeException();
    }
    
    CFAPathStandardElement lPathElement = (CFAPathStandardElement)lDataElement.getSubelement(1);
    
    return lPathElement.toArray();
  }
  
  private static Set<AbstractElement> getFinalStates(Model pCounterexample, CPAAlgorithm pAlgorithm, CFAFunctionDefinitionNode pEntry, CFANode pEndNode) {
    CompositeElement lInitialElement = (CompositeElement)pAlgorithm.getCPA().getInitialElement(pEntry);
    
    AbstractElement lInitialLocation = lInitialElement.get(0);
    
    CompoundElement lInitialCompoundElement = (CompoundElement)lInitialElement.get(1);
    
    List<AbstractElement> lElements = new LinkedList<AbstractElement>();
    lElements.add(getInitialElement(pCounterexample));
    for (int lIndex = 1; lIndex < lInitialCompoundElement.size(); lIndex++) {
      lElements.add(lInitialCompoundElement.getSubelement(lIndex));
    }
    
    CompoundElement lNewInitialCompoundElement = new CompoundElement(lElements);
    
    List<AbstractElement> lElements2 = new LinkedList<AbstractElement>();
    lElements2.add(lInitialLocation);
    lElements2.add(lNewInitialCompoundElement);
    
    CompositeElement lNewInitialCompositeElement = new CompositeElement(lElements2, lInitialElement.getCallStack());
    
    Precision lInitialPrecision = pAlgorithm.getCPA().getInitialPrecision(pEntry);

    ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.TOPSORT, true);
    lReachedElements.add(lNewInitialCompositeElement, lInitialPrecision);

    try {
      pAlgorithm.run(lReachedElements, true);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
    
    System.out.println(lReachedElements.getReached());
    
    return lReachedElements.getReached(pEndNode);
  }
  
  private static ExplicitAnalysisElement getInitialElement(Model pCounterexample) {
    ExplicitAnalysisElement lElement = new ExplicitAnalysisElement();
    
    MathsatModel lCounterexample = (MathsatModel)pCounterexample;
    
    Set<MathsatAssignable> lAssignables = lCounterexample.getAssignables();
    
    for (MathsatAssignable lAssignable : lAssignables) {
      
      // TODO hier liegt wohl ein Problem vor!!! Muss behoben werden!!!
      
      //if (lAssignable.getName().endsWith("@1")) {
      if (lAssignable.getName().endsWith("@2")) {
        String lVariableName = lAssignable.getName().substring(0, lAssignable.getName().length() - 2);
        
        MathsatValue lValue = lCounterexample.getValue(lAssignable);
        
        double lDoubleValue = Double.parseDouble(lValue.toString());
        
        lElement.assignConstant(lVariableName, (long)lDoubleValue, Integer.MAX_VALUE);
      }
    }
    
    System.out.println("Initial element: " + lElement.toString());
    
    return lElement;
  }
  
}
