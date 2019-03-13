/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;

@Options(prefix="testcase")
public class TestTargetProvider implements Statistics {

  private static TestTargetProvider instance = null;

  @Option(
      secure = true,
      name = "generate.parallel",
      description = "set to true if run multiple test case generation instances in parallel"
  )
  private boolean runParallel = false;

  @Option(
      secure = true,
      name = "targets.type", // adapt CPAMain.java if adjust name
      description = "Which CFA edges to use as test targets"
  )
  private TestTargetType type = TestTargetType.ASSUME;

  @Option(
      secure = true,
      name = "targets.optimization.strategy",
      description = "Which strategy to use to optimize set of test target edges"
  )
  private TestTargetAdaption optimization = TestTargetAdaption.NONE;

  private CFA cfa = null;
  private ImmutableSet<CFAEdge> initialTestTargets;
  private Set<CFAEdge> uncoveredTargets;
  private boolean printTargets = false;

  public TestTargetProvider(final Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private Set<CFAEdge> extractEdges() {
    return extractEdgesByCriterion(type.getEdgeCriterion(), optimization);
  }

  private Set<CFAEdge> extractEdgesByCriterion(
      final Predicate<CFAEdge> criterion, final TestTargetAdaption pAdaption) {
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).filter(criterion).toSet());
    }
    edges = pAdaption.adaptTestTargets(edges);
    return edges;
  }

  public static int getCurrentNumOfTestTargets() {
    if (instance == null) {
      return 0;
    }
    return instance.initialTestTargets.size();
  }

  public static void initialize(final Configuration pConfig) throws InvalidConfigurationException {
    instance = new TestTargetProvider(pConfig);
  }

  public static Set<CFAEdge> getTestTargets(final CFA pCfa) {
    if (pCfa != instance.cfa) {
      updateTargetsOfInstance(pCfa);
    }
    return instance.uncoveredTargets;
  }

  private static void updateTargetsOfInstance(final CFA pCfa) {
    instance.cfa = pCfa;
    Set<CFAEdge> targets = instance.extractEdges();
    if (instance.runParallel) {
      instance.uncoveredTargets = Collections.synchronizedSet(targets);
    } else {
      instance.uncoveredTargets = targets;
    }
    instance.initialTestTargets = ImmutableSet.copyOf(instance.uncoveredTargets);
  }

  public static String getCoverageInfo() {
    Preconditions.checkNotNull(instance);
    return (instance.initialTestTargets.size() - instance.uncoveredTargets.size())
        + " of "
        + instance.initialTestTargets.size()
        + " covered";
  }

  public static Statistics getTestTargetStatisitics(boolean pPrintTestGoalInfo) {
    Preconditions.checkNotNull(instance);
    instance.printTargets = pPrintTestGoalInfo;
    return instance;
  }


  @Override
  public @Nullable String getName() {
    return "Testtargets";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    int numCovered = initialTestTargets.size() - uncoveredTargets.size();
    double testTargetCoverage =
        initialTestTargets.size() == 0 ? 0 : (double) numCovered / initialTestTargets.size();
    pOut.printf("Test target coverage: %.2f%%%n", testTargetCoverage * 100);
    pOut.println("Number of total test targets: " + initialTestTargets.size());
    pOut.println("Number of covered test targets: " + numCovered);
    pOut.println("Number of uncovered test targets: " + uncoveredTargets.size());

    if (printTargets) {
    pOut.println("Initial test targets: ");
    for (CFAEdge edge : initialTestTargets) {
      pOut.println(edge.toString());
    }

    pOut.println("Test targets that have not been covered: ");
    for (CFAEdge edge : uncoveredTargets) {
      pOut.println(edge.toString());
    }
    }
    pOut.flush(); // TODO check if makes sense
  }
}
