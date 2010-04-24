package fllesh.fql.backend.query;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import common.configuration.Configuration;

import cpa.common.LogManager;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;
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

  private final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  private final ImmutableMap<String, String> mProperties =
        ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    States lStatesCoverage = new States(Identity.getInstance());
    
    Query lQuery = new Query(lStatesCoverage, new LowerBound(Identity.getInstance(), 0));
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
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
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
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
    
    System.out.println(QueryEvaluation.evaluate(lQuery, lTargetGraph));
  }
  
}
