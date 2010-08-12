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
package org.sosy_lab.cpachecker.fllesh.targetgraph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.ConditionEdge;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Function;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionCall;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionCalls;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionEntry;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Label;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Line;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.terms.Constant;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.terms.Variable;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;

public class TargetGraphTest {
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    System.out.println(lTargetGraph);
  }

  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/loop1.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    System.out.println(lTargetGraph);
  }

  @Test
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    System.out.println(lTargetGraph);
  }

  @Test
  public void test_04() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    TargetGraph lFilteredTargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "func");

    System.out.println(lFilteredTargetGraph);
  }

  @Test
  public void test_05() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    TargetGraph lFuncTargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "func");
    TargetGraph lF2TargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "f2");

    TargetGraph lUnionGraph = TargetGraphUtil.union(lFuncTargetGraph, lF2TargetGraph);

    System.out.println(lUnionGraph);
  }

  @Test
  public void test_06() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    TargetGraph lFuncTargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "func");

    TargetGraph lIntersectionGraph = TargetGraphUtil.intersect(lTargetGraph, lFuncTargetGraph);

    System.out.println(lIntersectionGraph);
  }

  @Test
  public void test_07() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    TargetGraph lFuncTargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "f");

    TargetGraph lMinusGraph = TargetGraphUtil.minus(lTargetGraph, lFuncTargetGraph);

    System.out.println(lMinusGraph);
  }

  @Test
  public void test_08() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());

    TargetGraph lFuncTargetGraph = TargetGraphUtil.applyFunctionNameFilter(lTargetGraph, "f");

    Variable lX = new Variable("x");
    Constant l100 = new Constant(100);
    Predicate lPredicate = new Predicate(lX, Predicate.Comparison.LESS, l100);
    
    TargetGraph lPredicatedGraph = TargetGraphUtil.predicate(lFuncTargetGraph, lPredicate);

    System.out.println(lPredicatedGraph);
  }

  @Test
  public void test_09() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(Identity.getInstance());
    
    // identity returns the (physically) same target graph
    assertTrue(lFilteredTargetGraph == lTargetGraph);
  }

  @Test
  public void test_10() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Function lFunctionFilter = new Function("f");
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph1 = lFilterEvaluator.evaluate(lFunctionFilter);

    TargetGraph lFilteredTargetGraph2 = lFilterEvaluator.evaluate(lFunctionFilter);

    // caching should return in the same target graphs
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph2);

    Function lFunctionFilter2 = new Function("f");

    TargetGraph lFilteredTargetGraph3 = lFilterEvaluator.evaluate(lFunctionFilter2);

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph3);

    Function lFunctionFilter3 = new Function("foo");

    TargetGraph lFilteredTargetGraph4 = lFilterEvaluator.evaluate(lFunctionFilter3);

    // a different function name filter should return in a different target graph
    assertFalse(lFilteredTargetGraph3.equals(lFilteredTargetGraph4));
  }

  @Test
  public void test_11() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);
    
    FunctionCall lFunctionCallFilter = new FunctionCall("f");

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFunctionCallFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionCallFilter));
  }

  @Test
  public void test_12() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    FunctionCall lFunctionCallFilter = new FunctionCall("func");

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFunctionCallFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionCallFilter));

    FunctionCall lFunctionCallFilter2 = new FunctionCall("func");

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionCallFilter2));
  }

  @Test
  public void test_13() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lFunctionCallsFilter = FunctionCalls.getInstance();

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFunctionCallsFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionCallsFilter));
  }

  @Test
  public void test_14() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lFunctionEntryFilter = new FunctionEntry("func");

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFunctionEntryFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionEntryFilter));

    Filter lFunctionEntryFilter2 = new FunctionEntry("func");

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFunctionEntryFilter2));
  }

  @Test
  public void test_15() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lLineFilter = new Line(102);
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lLineFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLineFilter));

    Filter lLineFilter2 = new Line(102);

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLineFilter2));
  }
  
  @Test
  public void test_21() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lLabelFilter = new Label("ERROR");
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lLabelFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLabelFilter));

    Filter lLabelFilter2 = new Label("ERROR");

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLabelFilter2));
  }
  
  @Test
  public void test_22() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lLabelFilter = new Label("ERROR");
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lLabelFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLabelFilter));

    Filter lLabelFilter2 = new Label("ERROR");

    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lLabelFilter2));
    
    System.out.println(lFilteredTargetGraph.getBoundedPaths(1));
  }
  
  @Test
  public void test_23() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph.getBoundedPaths(1));
  }
  
  @Test
  public void test_24() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lFilter = ConditionEdge.getInstance();
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFilter));
  }
  
  @Test
  public void test_25() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/fql/conditioncoverage.cil.c", "analysis.entryFunction", "foo");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    CFANode lInitialNode = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lInitialNode);

    Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(lInitialNode);

    Filter lFilter = ConditionEdge.getInstance();
    
    FilterEvaluator lFilterEvaluator = new FilterEvaluator(lTargetGraph, lBasicBlockEntries);

    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFilter);

    System.out.println(lFilteredTargetGraph);

    // check caching
    assertTrue(lFilteredTargetGraph == lFilterEvaluator.evaluate(lFilter));
  }  
}
