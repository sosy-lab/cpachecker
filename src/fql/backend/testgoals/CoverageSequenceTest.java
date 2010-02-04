package fql.backend.testgoals;

import java.io.IOException;

import org.junit.Test;

import cmdline.CPAMain;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import cpa.common.MainCPAStatistics;
import exceptions.CPAException;
import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.TargetGraph;
import fql.fllesh.CPAchecker;
import fql.frontend.ast.coverage.Edges;
import fql.frontend.ast.coverage.Sequence;
import fql.frontend.ast.coverage.States;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.pathmonitor.LowerBound;
import fql.frontend.ast.pathmonitor.PathMonitor;
import fql.frontend.ast.query.Query;


public class CoverageSequenceTest {
  
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
  public void test_02() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
  public void test_03() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
