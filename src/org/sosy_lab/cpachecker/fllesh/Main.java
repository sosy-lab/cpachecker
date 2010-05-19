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
package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTStatistics;
import org.sosy_lab.cpachecker.cpa.automatonanalysis.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.cpa.edgevisit.EdgeVisitCPA;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.ObserverAutomatonTranslator;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Pattern;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CPAchecker;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.FunctionCall;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fllesh.fql2.parser.FQLParser;

import com.google.common.base.Joiner;

public class Main {
  
  private static final String GOAL_AUTOMATON = "GoalAutomaton";
  private static final String PASSING_AUTOMATON = "PassingAutomaton";
  private static final String PRODUCT_AUTOMATON = "ProductAutomaton";

  private static Configuration createConfiguration(String pSourceFile, String pPropertiesFile) {
    return createConfiguration(Collections.singletonList(pSourceFile), pPropertiesFile);
  }

  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) {
    Map<String, String> lCommandLineOptions = new HashMap<String, String>();

    lCommandLineOptions.put("analysis.programNames", Joiner.on(", ").join(pSourceFiles));
    //lCommandLineOptions.put("output.path", "test/output");

    Configuration lConfiguration = null;
    try {
      lConfiguration = new Configuration(pPropertiesFile, lCommandLineOptions);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return lConfiguration;
  }

  private static File createPropertiesFile() {
    File lPropertiesFile = null;

    try {

      lPropertiesFile = File.createTempFile("fllesh.", ".properties");
      lPropertiesFile.deleteOnExit();

      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");

      lWriter.println("log.consoleLevel = ALL");

      lWriter.println("analysis.traversal = topsort");

      // we want to use CEGAR algorithm
      lWriter.println("analysis.useRefinement = true");
      lWriter.println("cegar.refiner = " + org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsRefiner.class.getCanonicalName());

      lWriter.close();

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return lPropertiesFile;
  }

  private static File createPropertiesFile(File pObserverAutomatonFile, String pDotFile) {
    File lPropertiesFile = Main.createPropertiesFile();

    // append configuration for observer automaton
    PrintWriter lWriter;
    try {

      lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile, true));

      lWriter.println("automatonAnalysis.inputFile = " + pObserverAutomatonFile.getAbsolutePath());
      lWriter.println("automatonAnalysis.dotExportFile = " + pDotFile);
      lWriter.close();

      return lPropertiesFile;

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }
  
  private static String getProductAutomaton() {
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    lWriter.println("AUTOMATON " + PRODUCT_AUTOMATON);
    lWriter.println("INITIAL STATE Init;");
    lWriter.println();
    lWriter.println("STATE Init:");
    //lWriter.println("  CHECK(AutomatonAnalysis_" + GOAL_AUTOMATON + "(\"state == Accept\")) && CHECK(AutomatonAnalysis_" + PASSING_AUTOMATON + "(\"state == Accept\")) -> ERROR;"); 
    lWriter.println("  CHECK(AutomatonAnalysis_" + GOAL_AUTOMATON + "(\"state == Accept\")) -> ERROR;");
    lWriter.println("  TRUE -> GOTO Init;");
    
    return lResult.toString();
  }

  /**
   * @param pArguments
   * @throws Exception
   */
  public static void main(String[] pArguments) throws Exception {
    assert(pArguments != null);
    assert(pArguments.length > 1);

    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();

    String lSourceFileName = pArguments[1];

    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);
      lCillyProcessedFile.deleteOnExit();

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    File lPropertiesFile = Main.createPropertiesFile();
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);

    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);


    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    
    String lFQLSpecificationString = pArguments[0];
    

    FQLSpecification lFQLSpecification = Main.parse(lFQLSpecificationString);
    
    System.out.println("FQL query: " + lFQLSpecification);
    System.out.println("File: " + lSourceFileName);
    
    
    org.sosy_lab.cpachecker.fllesh.fql2.translators.reducedecp.CoverageSpecificationToReducedECPTranslator lCoverageSpecificationTranslator = new org.sosy_lab.cpachecker.fllesh.fql2.translators.reducedecp.CoverageSpecificationToReducedECPTranslator(lMainFunction);
    //Pattern lPassingClause = lCoverageSpecificationTranslator.getPathPatternTranslator().translate(lFQLSpecification.getPathPattern());
    /*Set<Pattern> lTestGoals = lCoverageSpecificationTranslator.translate(lFQLSpecification.getCoverageSpecification());
    Pattern lPassingClause = lCoverageSpecificationTranslator.getPathPatternTranslator().translate(lFQLSpecification.getPathPattern());
    
    int lTestGoalIndex = 0;
    
    for (Pattern lTestGoal : lTestGoals) {
      System.out.print("Test Goal #" + (++lTestGoalIndex) + ": ");
      System.out.println(lTestGoal);
    }
    
    System.out.print("Passing clause: ");
    
    System.out.println(lPassingClause);*/
    
    
    
    FunctionCall lFunctionCallFilter = new FunctionCall("f");
    //SetMinus lSetMinus = new SetMinus(Identity.getInstance(), lFunctionCallFilter);
    Filter lSetMinus = Identity.getInstance();
    //PathPattern lPrefixPattern = new Repetition(new Edges(Identity.getInstance()));
    PathPattern lPrefixPattern = new Repetition(new Edges(lSetMinus));
    Quotation lQuotation = new Quotation(lPrefixPattern);
    CoverageSpecification lTarget = new Edges(lFunctionCallFilter);

    CoverageSpecification lIdRepetition = new Quotation(new Repetition(new Edges(Identity.getInstance())));

    CoverageSpecification lSpecification = new org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation(lQuotation, new org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation(lTarget, lIdRepetition));

    Set<Pattern> lTestGoals = lCoverageSpecificationTranslator.translate(lSpecification);

    System.out.println(lTestGoals);

    Pattern lTestGoal = null;

    for (Pattern lGoal : lTestGoals) {
      lTestGoal = lGoal;
      break;
    }

    System.out.println(lTestGoal);

    

    
    
    // 1) enumerate all test goals, i.e., evaluate them before adding the wrapper function
    
    // 2) check the feasibility of every test goal
    
    
    
    // TODO: for every test goal (i.e., pattern) create an automaton and check reachabilities


    /** Generating a wrapper start up method.
     * 
     *  The wrapper method is necessary for a correct semantics of the
     *  generated automata, especially, the correct semantics of the
     *  Kleene star operation.
     **/
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lCoverageSpecificationTranslator.getAnnotations(), lLogManager);

    String lAlphaId = lCoverageSpecificationTranslator.getAnnotations().getId(lWrapper.getAlphaEdge());
    String lOmegaId = lCoverageSpecificationTranslator.getAnnotations().getId(lWrapper.getOmegaEdge());


    /** create test goal automaton */
    File lTestGoalAutomatonFile = File.createTempFile("fllesh.goal.", ".oa");
    //lTestGoalAutomatonFile.deleteOnExit();
    
    PrintStream lTestGoalAutomaton = new PrintStream(new FileOutputStream(lTestGoalAutomatonFile));
    lTestGoalAutomaton.println(ObserverAutomatonTranslator.translate(lTestGoal, GOAL_AUTOMATON, lAlphaId, lOmegaId));
    lTestGoalAutomaton.close();
    
    File lTestGoalPropertiesFile = Main.createPropertiesFile(lTestGoalAutomatonFile, "test/output/test_goal_automaton.dot");
    Configuration lTestGoalConfiguration = Main.createConfiguration(lSourceFileName, lTestGoalPropertiesFile.getAbsolutePath());

    CPAFactory lTestGoalAutomatonFactory = ControlAutomatonCPA.factory();
    lTestGoalAutomatonFactory.setConfiguration(lTestGoalConfiguration);
    lTestGoalAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lTestGoalCPA = lTestGoalAutomatonFactory.createInstance();
    
    
    
    
    
    /** create passing automaton */
    /*File lPassingAutomatonFile = File.createTempFile("fllesh.passing.", ".oa");
    //lPassingAutomatonFile.deleteOnExit();

    PrintStream lPassingAutomaton = new PrintStream(new FileOutputStream(lPassingAutomatonFile));
    lPassingAutomaton.println(ObserverAutomatonTranslator.translate(lPassingClause, PASSING_AUTOMATON, lAlphaId, lOmegaId));
    lPassingAutomaton.close();
    
    File lPassingPropertiesFile = Main.createPropertiesFile(lPassingAutomatonFile, "test/output/passing_automaton.dot");
    Configuration lPassingConfiguration = Main.createConfiguration(lSourceFileName, lPassingPropertiesFile.getAbsolutePath());
    
    CPAFactory lPassingAutomatonFactory = ControlAutomatonCPA.factory();
    lPassingAutomatonFactory.setConfiguration(lPassingConfiguration);
    lPassingAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lPassingCPA = lPassingAutomatonFactory.createInstance();*/
    
    
    
    /** create product automaton */
    File lProductAutomatonFile = File.createTempFile("fllesh.product.", ".oa");
    //lProductAutomatonFile.deleteOnExit();
    
    PrintStream lProductAutomatonStream = new PrintStream(new FileOutputStream(lProductAutomatonFile));
    lProductAutomatonStream.println(Main.getProductAutomaton());
    lProductAutomatonStream.close();
    
    File lProductAutomatonPropertiesFile = Main.createPropertiesFile(lProductAutomatonFile, "test/output/product_automaton.dot");
    Configuration lProductAutomatonConfiguration = Main.createConfiguration(lSourceFileName, lProductAutomatonPropertiesFile.getAbsolutePath());

    CPAFactory lProductAutomatonFactory = ControlAutomatonCPA.factory();
    lProductAutomatonFactory.setConfiguration(lProductAutomatonConfiguration);
    lProductAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lProductObserverCPA = lProductAutomatonFactory.createInstance();

    
    
    
    EdgeVisitCPA.Factory lFactory = new EdgeVisitCPA.Factory(lCoverageSpecificationTranslator.getAnnotations());
    //lFactory.setConfiguration(lExtendedConfiguration);
    lFactory.setConfiguration(lConfiguration);
    lFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lEdgeVisitCPA = lFactory.createInstance();

    
    
    
    
    
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();

    CPAFactory lSymbPredAbsCPAFactory = SymbPredAbsCPA.factory();
    lSymbPredAbsCPAFactory.setConfiguration(lConfiguration);
    lSymbPredAbsCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lSymbPredAbsCPA = lSymbPredAbsCPAFactory.createInstance();

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);
    lComponentAnalyses.add(lEdgeVisitCPA);
    lComponentAnalyses.add(lSymbPredAbsCPA);
    lComponentAnalyses.add(lTestGoalCPA);
    //lComponentAnalyses.add(lPassingCPA);
    lComponentAnalyses.add(lProductObserverCPA);

    // create composite CPA
    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    // create ART CPA
    CPAFactory lARTCPAFactory = ARTCPA.factory();
    lARTCPAFactory.setChild(lCPA);
    lARTCPAFactory.setConfiguration(lConfiguration);
    lARTCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lARTCPA = lARTCPAFactory.createInstance();



    CEGARAlgorithm lAlgorithm = new CEGARAlgorithm(new CPAAlgorithm(lARTCPA, lLogManager), lConfiguration, lLogManager);

    Statistics lARTStatistics = new ARTStatistics(lConfiguration, lLogManager);
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);

    AbstractElement initialElement = lARTCPA.getInitialElement(lWrapper.getEntry());
    Precision initialPrecision = lARTCPA.getInitialPrecision(lWrapper.getEntry());

    ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.TOPSORT, true);
    lReachedElements.add(initialElement, initialPrecision);

    try {
      lAlgorithm.run(lReachedElements, true);
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (AbstractElement reachedElement : lReachedElements) {
      // TODO determine whether ERROR element was reached

      System.out.println(reachedElement);
    }

    PrintWriter lStatisticsWriter = new PrintWriter(System.out);

    lARTStatistics.printStatistics(lStatisticsWriter, Result.SAFE, lReachedElements);
  }
  
  private static FQLSpecification parse(String pFQLSpecificationString) throws Exception {
    FQLParser lParser = new FQLParser(pFQLSpecificationString);

    Object pParseResult;

    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      System.out.println(pFQLSpecificationString);

      throw e;
    }

    assert(pParseResult instanceof FQLSpecification);

    return (FQLSpecification)pParseResult;
  }

}

