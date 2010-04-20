package fllesh.fql.backend.pathmonitor;

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
import fllesh.fql.frontend.ast.filter.Filter;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.pathmonitor.Alternative;
import fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fllesh.fql.frontend.ast.predicate.CIdentifier;
import fllesh.fql.frontend.ast.predicate.NaturalNumber;
import fllesh.fql.frontend.ast.predicate.Predicate;
import fllesh.fql.frontend.ast.predicate.Predicates;

public class AutomatonTest {
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
    
    //System.out.println(lTargetGraph);
    
    Filter lFilter = Identity.getInstance();
    
    Automaton lAutomaton = Automaton.create(lFilter, lTargetGraph);
    
    System.out.println(lAutomaton);
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
    
    //System.out.println(lTargetGraph);
    
    /*Filter lFilter = Identity.getInstance();
    
    Automaton lAutomaton = Automaton.create(lFilter, lTargetGraph);*/
    
    Alternative lAlternative = new Alternative(Identity.getInstance(), Identity.getInstance());
    
    Automaton lAutomaton = Automaton.create(lAlternative, lTargetGraph);
    
    System.out.println(lAutomaton);
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
    
    Predicates lPreconditions = new Predicates();
    Predicates lPostconditions = new Predicates();
    
    //new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100))
    
    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, Identity.getInstance(), lPostconditions);
    
    Automaton lAutomaton = Automaton.create(lConditionalMonitor, lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
  @Test
  public void test_04() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Predicates lPreconditions = new Predicates();
    Predicates lPostconditions = new Predicates();
    
    lPreconditions.add(new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, Identity.getInstance(), lPostconditions);
    
    Automaton lAutomaton = Automaton.create(lConditionalMonitor, lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
  @Test
  public void test_05() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Predicates lPreconditions = new Predicates();
    Predicates lPostconditions = new Predicates();
    
    lPostconditions.add(new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, Identity.getInstance(), lPostconditions);
    
    Automaton lAutomaton = Automaton.create(lConditionalMonitor, lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
  @Test
  public void test_06() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Predicates lPreconditions = new Predicates();
    Predicates lPostconditions = new Predicates();
    
    lPreconditions.add(new Predicate(new CIdentifier("y"), Predicate.Comparison.LESS, new CIdentifier("z")));
    lPostconditions.add(new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, Identity.getInstance(), lPostconditions);
    
    Automaton lAutomaton = Automaton.create(lConditionalMonitor, lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
  @Test
  public void test_07() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Predicates lPreconditions = new Predicates();
    Predicates lPostconditions = new Predicates();
    
    lPreconditions.add(new Predicate(new CIdentifier("y"), Predicate.Comparison.LESS, new CIdentifier("z")));
    lPostconditions.add(new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, Identity.getInstance(), lPostconditions);
    
    Automaton lAutomaton = Automaton.create(new Concatenation(lConditionalMonitor, lConditionalMonitor), lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
}
