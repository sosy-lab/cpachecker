package fllesh.fql.fllesh.reachability;

import static org.junit.Assert.*;

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
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.concrete.ConcreteAnalysisCPA;
import cpa.location.LocationCPA;
import cpa.mustmay.MustMayAnalysisCPA;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;
import fllesh.fql.backend.pathmonitor.Automaton;
import fllesh.fql.backend.targetgraph.Node;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.fllesh.FeasibilityCheck;
import fllesh.fql.fllesh.cpa.AddSelfLoop;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.fllesh.util.Cilly;
import fllesh.fql.frontend.ast.filter.Identity;

public class SingletonQueryTest {
  
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }    

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
            
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = "test/programs/simple/functionCall.cil.c";
    
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
    
    
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
    
    LocationCPA lLocationCPA = new LocationCPA();
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);
    
    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();
    
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
