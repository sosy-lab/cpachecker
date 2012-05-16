/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ecp.translators;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.fshell.FShell3;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.PathPatternTranslator;
import org.sosy_lab.cpachecker.util.Cilly;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPrettyPrinter;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;

public class ToGuardedAutomatonTranslatorTest {

  private Cilly lCilly;

  @Before
  public void setup() throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();
    LogManager logger = new LogManager(config);
    lCilly = new Cilly(logger);
  }

  @Test
  public void testMain001() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    CFAFunctionDefinitionNode lMainFunction = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager).get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    System.out.println(ToGuardedAutomatonTranslator.translate(lPassing));
  }

  @Test
  public void testMain002() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\"";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    CFAFunctionDefinitionNode lMainFunction = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager).get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    System.out.println(ToGuardedAutomatonTranslator.translate(lPassing));
  }

  @Test
  public void testMain003() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING { x > 10 }";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    CFAFunctionDefinitionNode lMainFunction = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager).get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    System.out.println(ToGuardedAutomatonTranslator.translate(lPassing));
  }

  @Test
  public void testMain004() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    CFAFunctionDefinitionNode lMainFunction = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager).get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    System.out.println(ToGuardedAutomatonTranslator.translate(lPassing));
  }

  @Test
  public void testMain005() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }.EDGES(@CALL(f))";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    Map<String, CFAFunctionDefinitionNode> lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
    CFAFunctionDefinitionNode lMainFunction = lCFAMap.get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    NondeterministicFiniteAutomaton<GuardedLabel> lInitialAutomaton = ToGuardedAutomatonTranslator.translate(lPassing);

    System.out.println(lInitialAutomaton);

    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    GuardedEdgeLabel lAlphaLabel = new GuardedEdgeLabel(new ECPEdgeSet(lWrapper.getAlphaEdge()));
    GuardedEdgeLabel lOmegaLabel = new GuardedEdgeLabel(new ECPEdgeSet(lWrapper.getOmegaEdge()));

    NondeterministicFiniteAutomaton<GuardedLabel> lLambdaFreeAutomaton = ToGuardedAutomatonTranslator.removeLambdaEdges(lInitialAutomaton, lAlphaLabel, lOmegaLabel);
    System.out.println(AutomatonPrettyPrinter.print(lLambdaFreeAutomaton));

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lNodeSetFreeAutomaton = ToGuardedAutomatonTranslator.removeNodeSetGuards(lLambdaFreeAutomaton);
    System.out.println(AutomatonPrettyPrinter.print(lNodeSetFreeAutomaton));
  }

  @Test
  public void testMain006() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }.NODES(@CALL(f))";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    String lEntryFunction = "main";
    Configuration lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
    LogManager lLogManager = new LogManager(lConfiguration);
    Map<String, CFAFunctionDefinitionNode> lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
    CFAFunctionDefinitionNode lMainFunction = lCFAMap.get(lEntryFunction);

    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lMainFunction);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Collection<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lPatternTranslator.translate(lSpecification.getPathPattern());

    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();

    System.out.println("TEST GOALS:");

    int lIndex = 0;

    for (ElementaryCoveragePattern lGoal : lGoals) {
      System.out.println("Goal #" + (++lIndex));
      System.out.println(lPrettyPrinter.printPretty(lGoal));
    }

    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));

    NondeterministicFiniteAutomaton<GuardedLabel> lInitialAutomaton = ToGuardedAutomatonTranslator.translate(lPassing);

    System.out.println(lInitialAutomaton);
    System.out.println(AutomatonPrettyPrinter.print(lInitialAutomaton));

    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    GuardedEdgeLabel lAlphaLabel = new GuardedEdgeLabel(new ECPEdgeSet(lWrapper.getAlphaEdge()));
    GuardedEdgeLabel lOmegaLabel = new GuardedEdgeLabel(new ECPEdgeSet(lWrapper.getOmegaEdge()));

    NondeterministicFiniteAutomaton<GuardedLabel> lLambdaFreeAutomaton = ToGuardedAutomatonTranslator.removeLambdaEdges(lInitialAutomaton, lAlphaLabel, lOmegaLabel);
    System.out.println(AutomatonPrettyPrinter.print(lLambdaFreeAutomaton));

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lNodeSetFreeAutomaton = ToGuardedAutomatonTranslator.removeNodeSetGuards(lLambdaFreeAutomaton);
    System.out.println(AutomatonPrettyPrinter.print(lNodeSetFreeAutomaton));
  }

}
