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
package org.sosy_lab.cpachecker.fllesh.ecp.translators.observerautomaton;

import java.io.File;
import java.util.Set;

import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.fllesh.Main;
import org.sosy_lab.cpachecker.fllesh.Wrapper;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPrettyPrinter;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.targetgraph.TargetGraphUtil;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.PathPatternTranslator;
import org.sosy_lab.cpachecker.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;

public class ToControlAutomatonTranslatorTest {

  @Test
  public void testMain001() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }

  @Test
  public void testMain002() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\"";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }
  
  @Test
  public void testMain003() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING { x > 10 }";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }
  
  @Test
  public void testMain004() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }
  
  @Test
  public void testMain005() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }.EDGES(@CALL(f))";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }
  
  @Test
  public void testMain006() throws Exception {
    /** process FQL query */
    String lSpecificationString = "COVER \"EDGES(ID)*\".(EDGES(@CALL(f)) + NODES(@CALL(f))).\"EDGES(ID)*\" PASSING EDGES(ID)*.{ x > 10 }.NODES(@CALL(f))";
    FQLSpecification lSpecification = FQLSpecification.parse(lSpecificationString);
    System.out.println(lSpecification);
    
    /** process source code */
    Cilly lCilly = new Cilly();

    String lSourceFileName = "test/programs/simple/functionCall.c";

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile("main");
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);
    
    /** do translation */
    PathPatternTranslator lPatternTranslator = new PathPatternTranslator(lTargetGraph);
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lPatternTranslator);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lSpecification.getCoverageSpecification());
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
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);

    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    System.out.println(lTranslator.translate(lPassing, "PassingAutomaton"));
  }
  
}
