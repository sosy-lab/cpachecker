/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interpreter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.FShell3;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;

public class InterpreterCPATest {

  @Test
  public void test001() throws Exception {
    ConfigurableProgramAnalysis lLocationCPA;
    ConfigurableProgramAnalysis lCallStackCPA;

    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      lLocationCPA = LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      lCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }


    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(lCallStackCPA);

    int[] lInputs = new int[0];

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(lInputs);
    lComponentAnalyses.add(lInterpreterCPA);


    Configuration lConfiguration;
    LogManager lLogManager;

    String lSourceFileName = "test/programs/fql/locks/test_locks_1.c";
    String lEntryFunction = "main";

    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
      lLogManager = new LogManager(lConfiguration);

      lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
      lMainFunction = lCFAMap.get(lEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }


    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    try {
      lWrapper.toDot("output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, lLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(lWrapper.getEntry());
    Precision lInitialPrecision = lCPA.getInitialPrecision(lWrapper.getEntry());

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test002() throws Exception {
    ConfigurableProgramAnalysis lLocationCPA;
    ConfigurableProgramAnalysis lCallStackCPA;

    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      lLocationCPA = LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      lCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }


    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(lCallStackCPA);

    int[] lInputs = { 1, 0, 0 };

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(lInputs);
    lComponentAnalyses.add(lInterpreterCPA);


    Configuration lConfiguration;
    LogManager lLogManager;

    String lSourceFileName = "test/programs/fql/locks/test_locks_1.c";
    String lEntryFunction = "main";

    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
      lLogManager = new LogManager(lConfiguration);

      lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
      lMainFunction = lCFAMap.get(lEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }


    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    try {
      lWrapper.toDot("output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, lLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(lWrapper.getEntry());
    Precision lInitialPrecision = lCPA.getInitialPrecision(lWrapper.getEntry());

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test003() throws Exception {
    ConfigurableProgramAnalysis lLocationCPA;
    ConfigurableProgramAnalysis lCallStackCPA;

    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      lLocationCPA = LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      lCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }


    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(lCallStackCPA);

    int[] lInputs = new int[0];

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(lInputs, true);
    lComponentAnalyses.add(lInterpreterCPA);


    Configuration lConfiguration;
    LogManager lLogManager;

    String lSourceFileName = "test/programs/fql/locks/test_locks_1.c";
    String lEntryFunction = "main";

    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
      lLogManager = new LogManager(lConfiguration);

      lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
      lMainFunction = lCFAMap.get(lEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }


    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    try {
      lWrapper.toDot("output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, lLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(lWrapper.getEntry());
    Precision lInitialPrecision = lCPA.getInitialPrecision(lWrapper.getEntry());

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test004() throws Exception {
    ConfigurableProgramAnalysis lLocationCPA;
    ConfigurableProgramAnalysis lCallStackCPA;

    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      lLocationCPA = LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      lCallStackCPA = lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }


    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(lLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(lCallStackCPA);

    int[] lInputs = new int[0];

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(lInputs, true);
    lComponentAnalyses.add(lInterpreterCPA);


    Configuration lConfiguration;
    LogManager lLogManager;

    String lSourceFileName = "test/programs/fql/ntdrivers-simplified/cdaudio_simpl1_BUG.cil.c";
    String lEntryFunction = "main";

    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      lConfiguration = FShell3.createConfiguration(lSourceFileName, lEntryFunction);
      lLogManager = new LogManager(lConfiguration);

      lCFAMap = FShell3.getCFAMap(lSourceFileName, lConfiguration, lLogManager);
      lMainFunction = lCFAMap.get(lEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }


    Wrapper lWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, lLogManager);

    try {
      lWrapper.toDot("output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, lLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(lWrapper.getEntry());
    Precision lInitialPrecision = lCPA.getInitialPrecision(lWrapper.getEntry());

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
  }

}
