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
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPrettyPrinter;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.observerautomaton.ToControlAutomatonTranslator;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.AutomaticStreamReader;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CPAchecker;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.CoverageSpecificationTranslator;

import com.google.common.base.Joiner;

public class Main {
  
  private static final String GOAL_AUTOMATON = "GoalAutomaton";
  private static final String PASSING_AUTOMATON = "PassingAutomaton";
  private static final String PRODUCT_AUTOMATON = "ProductAutomaton";

  /**
   * @param pArguments
   * @throws Exception
   */
  public static void main(String[] pArguments) throws Exception {
    assert(pArguments != null);
    assert(pArguments.length > 1);
    
    String lFQLSpecificationString = pArguments[0];
    String lSourceFileName = pArguments[1];
    
    String lEntryFunction = "main";
    
    if (pArguments.length > 2) {
      lEntryFunction = pArguments[2];
    }
    
    // TODO implement nicer mechanism for disabling cilly preprocessing
    if (pArguments.length <= 3) {  
      // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
      Cilly lCilly = new Cilly();
  
      if (!lCilly.isCillyInvariant(lSourceFileName)) {
        File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);
        lCillyProcessedFile.deleteOnExit();
  
        lSourceFileName = lCillyProcessedFile.getAbsolutePath();
  
        System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
      }
    }

    File lPropertiesFile = Main.createPropertiesFile(lEntryFunction);
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    FQLSpecification lFQLSpecification = FQLSpecification.parse(lFQLSpecificationString);
    
    System.out.println("FQL query: " + lFQLSpecification);
    System.out.println("File: " + lSourceFileName);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    /** do translation */
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lTargetGraph);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(lFQLSpecification.getCoverageSpecification());
    ElementaryCoveragePattern lPassing = lSpecificationTranslator.translate(lFQLSpecification.getPathPattern());
    
    
    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCPAchecker.getCFAMap(), lLogManager);
    
    ToControlAutomatonTranslator lTranslator = new ToControlAutomatonTranslator(lWrapper.getAlphaEdge(), lWrapper.getOmegaEdge());
    
    File lControlAutomatonFile = lTranslator.getControlAutomatonFile(lPassing, Main.PASSING_AUTOMATON);

    File lPassingPropertiesFile = Main.createPropertiesFile(lControlAutomatonFile, "test/output/" + Main.PASSING_AUTOMATON + ".dot", lEntryFunction);
    Configuration lPassingConfiguration = Main.createConfiguration(lSourceFileName, lPassingPropertiesFile.getAbsolutePath());

    CPAFactory lPassingAutomatonFactory = ControlAutomatonCPA.factory();
    lPassingAutomatonFactory.setConfiguration(lPassingConfiguration);
    lPassingAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lPassingAutomatonCPA = lPassingAutomatonFactory.createInstance();
    
    
    
    
    ECPPrettyPrinter lPrettyPrinter = new ECPPrettyPrinter();
    
    // passing clause
    System.out.println("PASSING:");
    System.out.println(lPrettyPrinter.printPretty(lPassing));
    
    
    
    
    
    /** create product automaton */
    File lProductAutomatonFile = File.createTempFile("fllesh." + Main.PRODUCT_AUTOMATON + ".", ".ca");
    //lProductAutomatonFile.deleteOnExit();
    
    PrintStream lProductAutomatonStream = new PrintStream(new FileOutputStream(lProductAutomatonFile));
    // TODO enable passing automaton
    lProductAutomatonStream.println(Main.getProductAutomaton(false));
    lProductAutomatonStream.close();
    
    File lProductAutomatonPropertiesFile = Main.createPropertiesFile(lProductAutomatonFile, "test/output/" + Main.PRODUCT_AUTOMATON + ".dot", lEntryFunction);
    Configuration lProductAutomatonConfiguration = Main.createConfiguration(lSourceFileName, lProductAutomatonPropertiesFile.getAbsolutePath());

    CPAFactory lProductAutomatonFactory = ControlAutomatonCPA.factory();
    lProductAutomatonFactory.setConfiguration(lProductAutomatonConfiguration);
    lProductAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lProductObserverCPA = lProductAutomatonFactory.createInstance();

    
    
    
    
    
    
    
    System.out.println("TEST GOALS:");
    
    int lIndex = 0;
    
    for (ElementaryCoveragePattern lGoal : lGoals) {
      int lCurrentGoalNumber = ++lIndex;
      
      System.out.println("Goal #" + lCurrentGoalNumber);
      System.out.println(lPrettyPrinter.printPretty(lGoal));
      
      File lGoalAutomatonFile = lTranslator.getControlAutomatonFile(lGoal, Main.GOAL_AUTOMATON);
      
      File lTestGoalPropertiesFile = Main.createPropertiesFile(lGoalAutomatonFile, "test/output/" + Main.GOAL_AUTOMATON + ".dot", lEntryFunction);
      Configuration lTestGoalConfiguration = Main.createConfiguration(lSourceFileName, lTestGoalPropertiesFile.getAbsolutePath());

      CPAFactory lTestGoalAutomatonFactory = ControlAutomatonCPA.factory();
      lTestGoalAutomatonFactory.setConfiguration(lTestGoalConfiguration);
      lTestGoalAutomatonFactory.setLogger(lLogManager);
      ConfigurableProgramAnalysis lTestGoalCPA = lTestGoalAutomatonFactory.createInstance();
      
      // TODO annotations accumulate over test goals ... reset?
      EdgeVisitCPA.Factory lFactory = new EdgeVisitCPA.Factory(lTranslator.getAnnotations());
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

      boolean lErrorReached = false;
      
      for (AbstractElement reachedElement : lReachedElements) {
        if (reachedElement.isError()) {
          lErrorReached = true;
        }

        System.out.println(reachedElement);
      }

      PrintWriter lStatisticsWriter = new PrintWriter(System.out);

      if (lErrorReached) {
        lARTStatistics.printStatistics(lStatisticsWriter, Result.UNSAFE, lReachedElements);
        
        System.out.println("Goal #" + lCurrentGoalNumber + " is feasible:");
        
        /** determine test input */
        // TODO get data direct from SymbPredAbsCPA
        List<String> lCommand = new LinkedList<String>();
        lCommand.add("/home/holzera/mathsat-4.2.8-linux-x86/bin/mathsat");
        lCommand.add("-solve");
        lCommand.add("-print_model");
        lCommand.add("test/output/cex.msat");
        
        ProcessBuilder lMathsatBuilder = new ProcessBuilder(lCommand);
        Process lMathsat = lMathsatBuilder.start();
        
        AutomaticStreamReader lInputReader = new AutomaticStreamReader(lMathsat.getInputStream());
        Thread lInputReaderThread = new Thread(lInputReader);
        lInputReaderThread.start();
        
        AutomaticStreamReader lErrorReader = new AutomaticStreamReader(lMathsat.getErrorStream());
        Thread lErrorReaderThread = new Thread(lErrorReader);
        lErrorReaderThread.start();

        try {
          lMathsat.waitFor();
          lInputReaderThread.join();
          lErrorReaderThread.join();

          System.out.println(lInputReader.getInput());
          System.out.println(lErrorReader.getInput());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      else {
        lARTStatistics.printStatistics(lStatisticsWriter, Result.SAFE, lReachedElements);
        
        System.out.println("Goal #" + lCurrentGoalNumber + " is infeasible!");
      }
    }    
  }

  public static Configuration createConfiguration(String pSourceFile, String pPropertiesFile) {
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

  public static File createPropertiesFile(String pEntryFunction) {
    if (pEntryFunction == null) {
      throw new IllegalArgumentException("Parameter pEntryFunction is null!");
    }
    
    File lPropertiesFile = null;

    try {

      lPropertiesFile = File.createTempFile("fllesh.", ".properties");
      lPropertiesFile.deleteOnExit();

      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");

      //lWriter.println("log.consoleLevel = ALL");

      lWriter.println("analysis.traversal = topsort");
      lWriter.println("analysis.entryFunction = " + pEntryFunction);

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

  private static File createPropertiesFile(File pObserverAutomatonFile, String pDotFile, String pEntryFunction) {
    if (pObserverAutomatonFile == null) {
      throw new IllegalArgumentException("Parameter pObserverAutomaton is null!");
    }
    
    if (pDotFile == null) {
      throw new IllegalArgumentException("Parameter pDotFile is null!");
    }
    
    File lPropertiesFile = Main.createPropertiesFile(pEntryFunction);

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
  
  private static String getProductAutomaton(boolean pPassingAutomatonEnabled) {
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    lWriter.println("AUTOMATON " + PRODUCT_AUTOMATON);
    lWriter.println("INITIAL STATE Init;");
    lWriter.println();
    lWriter.println("STATE Init:");
    if (pPassingAutomatonEnabled) {
      lWriter.println("  CHECK(AutomatonAnalysis_" + GOAL_AUTOMATON + "(\"state == " + ToControlAutomatonTranslator.getAcceptingStateName() + "\")) && CHECK(AutomatonAnalysis_" + PASSING_AUTOMATON + "(\"state == " + ToControlAutomatonTranslator.getAcceptingStateName() + "\")) -> ERROR;");
    }
    else {
      lWriter.println("  CHECK(AutomatonAnalysis_" + GOAL_AUTOMATON + "(\"state == " + ToControlAutomatonTranslator.getAcceptingStateName() + "\")) -> ERROR;");
    }
    lWriter.println("  TRUE -> GOTO Init;");
    
    return lResult.toString();
  }
  
}

