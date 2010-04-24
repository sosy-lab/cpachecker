package fllesh.fql.fllesh.reachability;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import cfa.DOTBuilder;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.collect.ImmutableMap;
import common.configuration.Configuration;
import compositeCPA.CompositeCPA;
import compositeCPA.CompositeElement;
import compositeCPA.CompositePrecision;

import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.LogManager;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.concrete.ConcreteAnalysisCPA;
import cpa.concrete.ConcreteAnalysisTopElement;
import cpa.location.LocationCPA;
import cpa.mustmay.MustMayAnalysisCPA;
import cpa.mustmay.MustMayAnalysisElement;
import cpa.symbpredabsCPA.SymbPredAbsCPA;
import exceptions.CPAException;
import fllesh.fql.backend.pathmonitor.Automaton;
import fllesh.fql.backend.targetgraph.Node;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.fllesh.FeasibilityCheck;
import fllesh.fql.fllesh.cpa.AddSelfLoop;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.fllesh.util.Cilly;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.pathmonitor.LowerBound;


public class StandardQueryTest {
  
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, CPAException {
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/programs/simple/functionCall.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", lSourceFileName);
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
        
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA();
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();
    
    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);
    
    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);
    
    Automaton lFirstAutomaton = Automaton.create(Identity.getInstance(), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(Identity.getInstance(), lTargetGraph);
    
    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);
    
    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    
  }
  
  @Test
  public void test_02() throws IOException, CPAException {
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/programs/simple/functionCall.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", lSourceFileName);
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
        
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA();
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();
    
    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);
    
    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);
    
    Automaton lFirstAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    
    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);
    
    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());
    
    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    System.out.println(lQuery.hasNext());
  }
  
  @Test
  public void test_03() throws IOException, CPAException {
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/programs/simple/functionCall.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", lSourceFileName);
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
        
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA();
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();
    
    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);
    
    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);
    
    Automaton lFirstAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    
    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);
    
    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction, lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());
    
    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    System.out.println(lQuery.hasNext());
  }
  
  @Test
  public void test_04() throws IOException, CPAException {
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/programs/simple/functionCall.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", lSourceFileName);
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);    
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
        
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    CPAFactory lSymbPredAbsFactory = SymbPredAbsCPA.factory();
    
    //TODO: modify configuration such that SymbPredAbsCPA is properly configured 
    lSymbPredAbsFactory.setConfiguration(lConfiguration);
    lSymbPredAbsFactory.setLogger(lLogManager);
    
    ConfigurableProgramAnalysis lMayCPA = lSymbPredAbsFactory.createInstance();
    
    /*CPAFactory lMayARTFactory = ARTCPA.factory();
    
    lMayARTFactory.setChild(lMayCPA);
    lMayARTFactory.setConfiguration(lConfiguration);
    lMayARTFactory.setLogger(lLogManager);*/
    
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA();
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();
    
    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();
    
    // Caution: take care of abstraction location
    AbstractElement lMayTopElement = lMayCPA.getInitialElement(lMainFunction);
    //AbstractElement lMayTopElement = lMayCPA.getAbstractDomain().getTopElement();
    
    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lMayTopElement);
    
    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lMainFunction, lInitialMustMayAnalysisElement);
        
    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);
    
    Automaton lFirstAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(Identity.getInstance(), 0), lTargetGraph);
    
    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);
    
    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction, lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());
    
    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    System.out.println(lQuery.hasNext());
  }
  
}
