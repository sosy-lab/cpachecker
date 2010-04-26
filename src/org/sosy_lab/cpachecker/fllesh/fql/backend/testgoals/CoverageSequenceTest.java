package org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CPAchecker;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query.Query;


public class CoverageSequenceTest {
  
  private final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  private final ImmutableMap<String, String> mProperties =
        ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    Query lQuery = new Query(lStatesCoverage, new LowerBound(Identity.getInstance(), 0));
    
    CoverageSequence lCoverageSequence = CoverageSequence.create(lQuery.getCoverage(), lTargetGraph);
    
    Automaton lPassingAutomaton = Automaton.create(lQuery.getPassingMonitor(), lTargetGraph);
    
    System.out.println(lCoverageSequence);
    System.out.println("PASSING AUTOMATON: ");
    System.out.println(lPassingAutomaton);
  }
  
  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    PathMonitor lTrueMonitor = new LowerBound(Identity.getInstance(), 0);
    
    Sequence lSequence = new Sequence(lTrueMonitor, lStatesCoverage, lTrueMonitor);
    
    Query lQuery = new Query(lSequence, lTrueMonitor);
    
    CoverageSequence lCoverageSequence = CoverageSequence.create(lQuery.getCoverage(), lTargetGraph);
    
    Automaton lPassingAutomaton = Automaton.create(lQuery.getPassingMonitor(), lTargetGraph);
    
    System.out.println(lCoverageSequence);
    System.out.print("PASSING AUTOMATON: ");
    System.out.println(lPassingAutomaton);
  }
  
  @Test
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    PathMonitor lTrueMonitor = new LowerBound(Identity.getInstance(), 0);
    
    Sequence lSequence = new Sequence(lTrueMonitor, lStatesCoverage, lTrueMonitor);
    
    lSequence.extend(lTrueMonitor, new Edges(Identity.getInstance()));
    
    Query lQuery = new Query(lSequence, lTrueMonitor);
    
    CoverageSequence lCoverageSequence = CoverageSequence.create(lQuery.getCoverage(), lTargetGraph);
    
    Automaton lPassingAutomaton = Automaton.create(lQuery.getPassingMonitor(), lTargetGraph);
    
    System.out.println(lCoverageSequence);
    System.out.print("PASSING AUTOMATON: ");
    System.out.println(lPassingAutomaton);
  }
  
}
