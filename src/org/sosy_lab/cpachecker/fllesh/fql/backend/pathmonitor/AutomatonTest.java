/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.FilterMonitor;
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

    Automaton lAutomaton = Automaton.create(new FilterMonitor(lFilter), lTargetGraph);

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

    Alternative lAlternative = new Alternative(new FilterMonitor(Identity.getInstance()), new FilterMonitor(Identity.getInstance()));

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

    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, new FilterMonitor(Identity.getInstance()), lPostconditions);

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

    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, new FilterMonitor(Identity.getInstance()), lPostconditions);

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

    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, new FilterMonitor(Identity.getInstance()), lPostconditions);

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

    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, new FilterMonitor(Identity.getInstance()), lPostconditions);

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

    ConditionalMonitor lConditionalMonitor = new ConditionalMonitor(lPreconditions, new FilterMonitor(Identity.getInstance()), lPostconditions);

    Automaton lAutomaton = Automaton.create(new Concatenation(lConditionalMonitor, lConditionalMonitor), lTargetGraph);

    System.out.println(lAutomaton);
  }

}
