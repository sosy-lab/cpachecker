package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.FeasibilityCheck;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.AddSelfLoop;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CPAchecker;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Identity;

public class SingletonQueryTest {
  
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
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
