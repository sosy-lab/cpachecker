package fql.fllesh.reachability;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;

import cfa.DOTBuilder;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cmdline.CPAMain.InvalidCmdlineArgumentException;

import compositeCPA.CompositeCPA;
import compositeCPA.CompositeElement;
import compositeCPA.CompositePrecision;
import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.concrete.ConcreteAnalysisCPA;
import cpa.location.LocationCPA;
import cpa.mustmay.MustMayAnalysisCPA;

import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Node;
import fql.backend.targetgraph.TargetGraph;
import fql.fllesh.FeasibilityCheck;
import fql.fllesh.cpa.AddSelfLoop;
import fql.fllesh.util.CPAchecker;
import fql.fllesh.util.Cilly;
import fql.frontend.ast.filter.Identity;


public class SingletonQueryTest {
  
  private static final String mConfig = "-config";
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws IOException, InvalidCmdlineArgumentException {
    
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
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().cfaMapIterator(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    Node lProgramEntry = new Node(lMainFunction);
    
    
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA("", "");
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    CompositeCPA lCompositeCPA = CompositeCPA.createNewCompositeCPA(lCPAs, lMainFunction);
    
    CompositeElement lDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);
    
    Automaton lFirstAutomaton = Automaton.create(Identity.getInstance(), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(Identity.getInstance(), lTargetGraph);
        
    Query lQuery = SingletonQuery.create(lDataSpaceElement, lDataSpacePrecision, lFirstAutomaton, lFirstAutomaton.getInitialStates(), lSecondAutomaton, lSecondAutomaton.getInitialStates());
    
    assertTrue(lQuery.hasNext());
    
    Waypoint lNextWaypoint = lQuery.next();
    
    System.out.println(lNextWaypoint);
    
    assertFalse(lQuery.hasNext());
    
  }
  
}
