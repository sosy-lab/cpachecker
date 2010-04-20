package fllesh.fql.backend.query;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import common.configuration.Configuration;

import cmdline.CPAMain;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cpa.common.LogManager;
import exceptions.CPAException;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.frontend.ast.coverage.Edges;
import fllesh.fql.frontend.ast.coverage.Sequence;
import fllesh.fql.frontend.ast.coverage.States;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import fllesh.fql.frontend.ast.pathmonitor.PathMonitor;
import fllesh.fql.frontend.ast.query.Query;


public class QueryEvaluationTest {

  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    Query lQuery = new Query(lStatesCoverage, new LowerBound(Identity.getInstance(), 0));
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
  }
  
  @Test
  public void test_02() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    PathMonitor lTrueMonitor = new LowerBound(Identity.getInstance(), 0);
    
    Sequence lSequence = new Sequence(lTrueMonitor, lStatesCoverage, lTrueMonitor);
    
    Query lQuery = new Query(lSequence, lTrueMonitor);
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
  }
  
  @Test
  public void test_03() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    PathMonitor lTrueMonitor = new LowerBound(Identity.getInstance(), 0);
    
    Sequence lSequence = new Sequence(lTrueMonitor, lStatesCoverage, lTrueMonitor);
    
    lSequence.extend(lTrueMonitor, new Edges(Identity.getInstance()));
    
    Query lQuery = new Query(lSequence, lTrueMonitor);
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
  }
  
}
