package org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CPAchecker;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Alternative;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.CIdentifier;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.NaturalNumber;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;

public class AutomatonTest {
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
    
    //System.out.println(lTargetGraph);
    
    Filter lFilter = Identity.getInstance();
    
    Automaton lAutomaton = Automaton.create(lFilter, lTargetGraph);
    
    System.out.println(lAutomaton);
  }
  
  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
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
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
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
  public void test_04() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
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
  public void test_05() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
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
  public void test_06() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);

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
  public void test_07() throws IOException, InvalidConfigurationException, CPAException {
    Configuration lConfiguration = new Configuration(mPropertiesFile, mProperties);
    
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
