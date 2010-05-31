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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.query.QueryEvaluation;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraphUtil;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.CoverageSequence;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoal;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.AddSelfLoop;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query.Query;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.parser.FQLParser;
import org.sosy_lab.cpachecker.fllesh.util.Cilly;
import org.sosy_lab.cpachecker.fllesh.util.ModifiedCPAchecker;

public class Main {

  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

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

    if (!lCilly.isCillyInvariant(pArguments[1])) {
      File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);

      lSourceFileName = lCillyProcessedFile.getAbsolutePath();

      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }

    // set source file name
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", lSourceFileName);

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    ModifiedCPAchecker lCPAchecker = new ModifiedCPAchecker(lConfiguration, lLogManager);

    Query lQuery = parseQuery(pArguments[0]);

    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();

    TargetGraph lTargetGraph = TargetGraphUtil.cfa(lMainFunction);

    Pair<CoverageSequence, Automaton> lQueryEvaluation = QueryEvaluation.evaluate(lQuery, lTargetGraph);

    Automaton lPassingMonitor = lQueryEvaluation.getSecond();

    List<Pair<Automaton, Set<? extends TestGoal>>> lTargetSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();

    CoverageSequence lCoverageSequence = lQueryEvaluation.getFirst();

    for (Pair<Automaton, Set<? extends TestGoal>> lPair : lCoverageSequence) {
      lTargetSequence.add(lPair);
    }


    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);


    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));


    Node lProgramEntry = new Node(lMainFunction);
    Node lProgramExit = new Node(lMainFunction.getExitNode());

    lTargetSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(lCoverageSequence.getFinalMonitor(), Collections.singleton(lProgramExit)));

    FeasibilityCheck lFeasibilityCheck = new FeasibilityCheck(lLogManager);

    Set<FeasibilityWitness> lWitnesses = TestGoalEnumeration.run(lTargetSequence, lPassingMonitor, lProgramEntry, lFeasibilityCheck);

    generateTestCases(lWitnesses);
  }

  private static void generateTestCases(Set<FeasibilityWitness> pWitnesses) {
    // TODO: implement test case generation mechanism
  }

  private static Query parseQuery(String pFQLQuery) throws Exception {
    FQLParser lParser = new FQLParser(pFQLQuery);

    Object pParseResult;

    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      System.out.println(pFQLQuery);

      throw e;
    }

    assert(pParseResult instanceof Query);

    return (Query)pParseResult;
  }

}
