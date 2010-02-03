package fql.backend.pathmonitor;

import java.io.IOException;

import org.junit.Test;

import cmdline.CPAMain;
import cmdline.CPAchecker;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import cpa.common.MainCPAStatistics;
import exceptions.CPAException;
import fql.backend.targetgraph.TargetGraph;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.pathmonitor.Alternative;
import fql.frontend.ast.pathmonitor.Concatenation;
import fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;

public class AutomatonTest {
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
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
