package fql.fllesh.reachability;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;

import cfa.DOTBuilder;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cmdline.CPAMain.InvalidCmdlineArgumentException;

import common.configuration.Configuration;
import compositeCPA.CompositeCPA;
import compositeCPA.CompositeElement;
import compositeCPA.CompositePrecision;

import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.LogManager;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.concrete.ConcreteAnalysisCPA;
import cpa.location.LocationCPA;
import cpa.mustmay.MustMayAnalysisCPA;
import exceptions.CPAException;
import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Node;
import fql.backend.targetgraph.TargetGraph;
import fql.fllesh.FeasibilityCheck;
import fql.fllesh.cpa.AddSelfLoop;
import fql.fllesh.util.CPAchecker;
import fql.fllesh.util.Cilly;
import fql.frontend.ast.filter.Identity;


public class StandardQueryTest {
  
  private static final String mConfig = "-config";
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws IOException, InvalidCmdlineArgumentException, CPAException {
    
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/tests/single/functionCall.cil.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    lArguments[2] = lSourceFileName;
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);

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
    Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    
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
    
    StandardQuery lQuery = StandardQuery.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    
  }
  
  @Test
  public void test_02() throws IOException, InvalidCmdlineArgumentException, CPAException {
    
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/tests/single/functionCall.cil.c";
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    lArguments[2] = lSourceFileName;
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);

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
    Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    
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
    
    StandardQuery lQuery = StandardQuery.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());
    
    System.out.println(lQuery.hasNext());
  }
  
}
