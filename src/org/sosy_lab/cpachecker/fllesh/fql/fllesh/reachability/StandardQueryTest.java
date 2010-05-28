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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisTopElement;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisElement;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.FeasibilityCheck;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.AddSelfLoop;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.FilterMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;


public class StandardQueryTest {

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

    String lSourceFileName = "test/programs/simple/functionCall.c";

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

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);


    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);


    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));


    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());


    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();

    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);

    LocationCPA lLocationCPA = new LocationCPA();

    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();

    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);

    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();

    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);

    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);

    Automaton lFirstAutomaton = Automaton.create(new FilterMonitor(Identity.getInstance()), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new FilterMonitor(Identity.getInstance()), lTargetGraph);

    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);

    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());


  }

  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {

    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

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

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);


    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);


    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));


    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());


    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();

    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);

    LocationCPA lLocationCPA = new LocationCPA();

    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();

    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);

    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();

    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);

    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);

    Automaton lFirstAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);

    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);

    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction.getExitNode(), lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());

    System.out.println(lQuery.hasNext());
  }

  @Test
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {

    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

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

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);


    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);


    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));


    Node lProgramEntry = new Node(lMainFunction);
    //Node lProgramExit = new Node(lMainFunction.getExitNode());


    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();

    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);

    LocationCPA lLocationCPA = new LocationCPA();

    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();

    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);

    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();

    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lProgramEntry);
    //CompositeElement lFinalDataSpaceElement = FeasibilityCheck.createNextElement(lProgramExit);

    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);

    Automaton lFirstAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);

    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);

    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction, lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());

    System.out.println(lQuery.hasNext());
  }

  @Test
  public void test_04() throws IOException, InvalidConfigurationException, CPAException {

    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

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

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);


    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);


    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));


    CPAFactory lSymbPredAbsFactory = SymbPredAbsCPA.factory();

    //TODO: modify configuration such that SymbPredAbsCPA is properly configured
    lSymbPredAbsFactory.setConfiguration(lConfiguration);
    lSymbPredAbsFactory.setLogger(lLogManager);

    ConfigurableProgramAnalysis lMayCPA = lSymbPredAbsFactory.createInstance();

    /*CPAFactory lMayARTFactory = ARTCPA.factory();

    lMayARTFactory.setChild(lMayCPA);
    lMayARTFactory.setConfiguration(lConfiguration);
    lMayARTFactory.setLogger(lLogManager);*/

    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();

    MustMayAnalysisCPA lMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);

    LocationCPA lLocationCPA = new LocationCPA();

    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();

    lCPAs.add(lLocationCPA);
    lCPAs.add(lMustMayAnalysisCPA);

    ConfigurableProgramAnalysis lCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();

    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();

    // Caution: take care of abstraction location
    AbstractElement lMayTopElement = lMayCPA.getInitialElement(lMainFunction);
    //AbstractElement lMayTopElement = lMayCPA.getAbstractDomain().getTopElement();

    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lMayTopElement);

    CompositeElement lInitialDataSpaceElement = FeasibilityCheck.createInitialElement(lMainFunction, lInitialMustMayAnalysisElement);

    CompositePrecision lDataSpacePrecision = (CompositePrecision)lCompositeCPA.getInitialPrecision(lMainFunction);

    Automaton lFirstAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);
    Automaton lSecondAutomaton = Automaton.create(new LowerBound(new FilterMonitor(Identity.getInstance()), 0), lTargetGraph);

    StandardQuery.Factory lQueryFactory = new StandardQuery.Factory(lLogManager, lMustCPA, lMayCPA);

    StandardQuery lQuery = lQueryFactory.create(lFirstAutomaton, lSecondAutomaton, lInitialDataSpaceElement, lDataSpacePrecision, lFirstAutomaton.getInitialStates(), lSecondAutomaton.getInitialStates(), lMainFunction, lFirstAutomaton.getFinalStates(), lSecondAutomaton.getFinalStates());

    System.out.println("Automaton1: " + lFirstAutomaton.toString());
    System.out.println("Automaton2: " + lSecondAutomaton.toString());

    System.out.println("Source: " + lQuery.getSource().toString());
    System.out.println("Target: " + lQuery.getTarget().toString());

    System.out.println(lQuery.hasNext());
  }

}
