// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetAdaption;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TestTargetProvider implements Statistics {

  private static TestTargetProvider instance = null;

  private final CFA cfa;
  private final TestTargetType type;
  private final ImmutableSet<CFAEdge> initialTestTargets;
  private final Set<CFAEdge> uncoveredTargets;
  private Set<CFAEdge> uncoveredRedundantTargets;
  private int numNonOptimizedTargets = -1;
  private boolean trackCoverageOfRedundantTargets = false;
  private boolean printTargets = false;
  private boolean runParallel;
  private boolean applyOptimizationsNested;
  private List<TestTargetAdaption> optimizationStrategies;
  private Timer optimizationTimer = new Timer();

  private TestTargetProvider(
      final CFA pCfa,
      final boolean pRunParallel,
      final TestTargetType pType,
      final String pTargetFun,
      final List<TestTargetAdaption> pTargetOptimizationStrategies,
      final boolean pApplyOptimizationsNested,
      final boolean pTrackRedundantTargets) {
    cfa = pCfa;
    runParallel = pRunParallel;
    type = pType;
    optimizationStrategies = pTargetOptimizationStrategies;
    trackCoverageOfRedundantTargets = pTrackRedundantTargets;
    applyOptimizationsNested = pApplyOptimizationsNested;

    Predicate<CFAEdge> edgeCriterion =
        switch (type) {
          case FUN_CALL -> type.getEdgeCriterion(pTargetFun);
          default -> type.getEdgeCriterion();
        };
    Set<CFAEdge> targets =
        extractEdgesByCriterion(edgeCriterion, pTargetOptimizationStrategies, pCfa);

    if (runParallel) {
      uncoveredTargets = Collections.synchronizedSet(targets);
    } else {
      uncoveredTargets = targets;
    }
    initialTestTargets = ImmutableSet.copyOf(uncoveredTargets);
  }

  private Set<CFAEdge> extractEdgesByCriterion(
      final Predicate<CFAEdge> criterion,
      final List<TestTargetAdaption> pTargetOptimizationStrategies,
      final CFA pCfa) {
    Set<CFAEdge> edges = Sets.newLinkedHashSet(CFAUtils.allEdges(pCfa).filter(criterion));

    numNonOptimizedTargets = edges.size();
    if (trackCoverageOfRedundantTargets) {
      uncoveredRedundantTargets = new LinkedHashSet<>(edges);
    }

    if (pTargetOptimizationStrategies != null) {
      optimizationTimer.start();

      try {
        Set<CFAEdge> edgesOfCurrentOptimization;

        for (TestTargetAdaption optimizationStrategy : pTargetOptimizationStrategies) {
          if (applyOptimizationsNested) {
            edges = optimizationStrategy.adaptTestTargets(edges, pCfa);
          }
          edgesOfCurrentOptimization = optimizationStrategy.adaptTestTargets(edges, pCfa);
          if (edgesOfCurrentOptimization.size() < edges.size()) {
            edges = edgesOfCurrentOptimization;
          }
        }
      } finally {
        optimizationTimer.stopIfRunning();
      }
    }
    if (trackCoverageOfRedundantTargets) {
      uncoveredRedundantTargets.removeAll(edges);
    } else {
      uncoveredRedundantTargets = ImmutableSet.of();
    }
    return edges;
  }

  public static void processTargetPath(CounterexampleInfo pCexInfo) {
    if (instance != null && instance.trackCoverageOfRedundantTargets) {
      instance.uncoveredRedundantTargets.removeAll(pCexInfo.getTargetPath().getFullPath());
    }
  }

  public static int getTotalNumberOfTestTargets() {
    if (instance == null) {
      return 0;
    }
    return instance.initialTestTargets.size();
  }

  public static int getNumberOfUncoveredTestTargets() {
    if (instance == null) {
      return 0;
    }
    return instance.uncoveredTargets.size();
  }

  public static synchronized Set<CFAEdge> getTestTargets(
      final CFA pCfa,
      final boolean pRunParallel,
      final TestTargetType pType,
      final String pTargetFun,
      final List<TestTargetAdaption> pTargetOptimizationStrategies,
      final boolean pApplyOptimizationsNested,
      final boolean pTrackRedundantTargets,
      final LogManager pLogger) {
    if (instance == null
        || pCfa != instance.cfa
        || instance.type != pType
        || !instance.optimizationStrategies.equals(pTargetOptimizationStrategies)
        || instance.applyOptimizationsNested != pApplyOptimizationsNested) {
      instance =
          new TestTargetProvider(
              pCfa,
              pRunParallel,
              pType,
              pTargetFun,
              pTargetOptimizationStrategies,
              pApplyOptimizationsNested,
              pTrackRedundantTargets);

      if (pTargetOptimizationStrategies != null
          && !pTargetOptimizationStrategies.isEmpty()
          && !(pTargetOptimizationStrategies.size() == 1
              && pTargetOptimizationStrategies.get(0).equals(TestTargetAdaption.NONE))) {
        pLogger.log(
            Level.SEVERE,
            "Consider "
                + instance.initialTestTargets.size()
                + " of "
                + instance.numNonOptimizedTargets
                + " original targets computed in "
                + instance.optimizationTimer.getLengthOfLastInterval()
                + " seconds.");
      }
    }
    Preconditions.checkState(instance.runParallel || !pRunParallel);
    Preconditions.checkState(instance.trackCoverageOfRedundantTargets || !pTrackRedundantTargets);
    return instance.uncoveredTargets;
  }

  public static String getCoverageInfo() {
    Preconditions.checkNotNull(instance);
    String targetCoverageInfo =
        (instance.initialTestTargets.size() - instance.uncoveredTargets.size())
            + " of "
            + instance.initialTestTargets.size()
            + " covered";
    if (instance.trackCoverageOfRedundantTargets) {
      return targetCoverageInfo
          + "\n"
          + (instance.numNonOptimizedTargets
              - instance.uncoveredTargets.size()
              - instance.uncoveredRedundantTargets.size())
          + " of all "
          + instance.numNonOptimizedTargets
          + " covered";
    }
    return targetCoverageInfo;
  }

  public static Statistics getTestTargetStatisitics(boolean pPrintTestGoalInfo) {
    Preconditions.checkNotNull(instance);
    instance.printTargets = pPrintTestGoalInfo;
    return instance;
  }

  public static boolean isTerminatingFunctionCall(final CFAEdge pEdge) {
    if (pEdge instanceof CStatementEdge cStatementEdge
        && cStatementEdge.getStatement() instanceof CFunctionCall cFunctionCall) {
      String funName = "";
      CExpression funExpr = cFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funExpr instanceof CIdExpression cIdExpression) {
        funName = cIdExpression.getName();
      }
      return funName.equals("abort") || funName.equals("exit") || funName.equals("__assert_fail");
    }
    return false;
  }

  @Override
  public @Nullable String getName() {
    return "Testtargets";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    int numCovered = initialTestTargets.size() - uncoveredTargets.size();
    double testTargetCoverage =
        initialTestTargets.isEmpty() ? 0 : (double) numCovered / initialTestTargets.size();
    pOut.printf("Test target coverage: %.2f%%%n", testTargetCoverage * 100);
    if (numNonOptimizedTargets >= 0) {
      pOut.println("Number of total original test targets: " + numNonOptimizedTargets);
      if (trackCoverageOfRedundantTargets) {
        pOut.println(
            "Number of covered original test targets:"
                + (numNonOptimizedTargets
                    - uncoveredTargets.size()
                    - uncoveredRedundantTargets.size()));
      }
    }
    pOut.println("Number of total test targets: " + initialTestTargets.size());
    pOut.println("Number of covered test targets: " + numCovered);
    pOut.println("Number of uncovered test targets: " + uncoveredTargets.size());
    pOut.println("Total time for test goal reduction:     " + optimizationTimer);

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
  }
}
