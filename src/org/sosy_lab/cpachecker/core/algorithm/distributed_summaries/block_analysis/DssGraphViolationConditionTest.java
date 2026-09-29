// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph.Incoming;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DssGraphViolationConditionTest {
  private static List<ARGPath> enumerate(DssARGPathGraph graph) {
    List<ARGPath> result = new ArrayList<>();
    enumerate(graph, new ArrayList<>(ImmutableList.of(graph.getLastState())), result);
    return result;
  }

  private static void enumerate(DssARGPathGraph graph, List<ARGState> path, List<ARGPath> result) {
    if (path.getLast() == graph.getFirstState()) {
      result.add(new ARGPath(path.reversed()));
      return;
    }
    for (Incoming parent : graph.incoming(path.getLast())) {
      List<ARGState> next = new ArrayList<>(path);
      next.add(parent.parent());
      enumerate(graph, next, result);
    }
  }

  private void compare(String body) throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            "extern void __VERIFIER_error(void); int main(int flag) { " + body + " return 0; }");
    BlockNode block = new SingleBlockDecomposition().decompose(cfa).getRoot();
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(DssTestUtils.DSS_FORWARD_CONFIGURATION_FILE)
            .setOption("dss.graphViolationConditions", "true")
            .setOption("dss.cpa.predicate.projectViolationConditions", "true")
            .setOption("dss.cpa.predicate.projectNestedDisjunctions", "true")
            .setOption("dss.cpa.predicate.generalizeViolationConditions", "false")
            .build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownManager shutdown = ShutdownManager.create();
    Specification spec =
        Specification.fromFiles(
            ImmutableList.of(Path.of("config/specification/default.spc")),
            cfa,
            config,
            logger,
            shutdown.getNotifier());
    DssAnalysisOptions options = new DssAnalysisOptions(config);
    DssBlockAnalysis analysis =
        new DssBlockAnalysis(
            logger,
            block,
            cfa,
            spec,
            config,
            options,
            new DssMessageFactory(options),
            shutdown,
            new DssSingleWorkerStatistics("test"));
    try {
      AbstractState root = analysis.makeStartState(false);
      DssBlockAnalysisResult result =
          analysis.runInitialBlockAnalysis(root, analysis.makeStartPrecision());
      assertThat(result.getTargetStates()).isNotEmpty();
      ViolationConditionOperator operator = analysis.getDcpa().getViolationConditionOperator();
      assertThat(operator.supportsGraph()).isTrue();
      PredicateCPA predicateCPA =
          (PredicateCPA)
              CPAs.retrieveCPA(analysis.getDcpa(), DistributedPredicateCPA.class).getCPA();
      FormulaManagerView fmgr = predicateCPA.getSolver().getFormulaManager();
      BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
      int paths = 0;
      for (ARGState target : result.getTargetStates()) {
        // Parameter declarations assign fresh values inside the enclosing block. Compare the
        // two operators after this initialization, where flag actually is an entry variable.
        ARGState entry =
            ARGUtils.getOnePathTo(target).asStatesList().stream()
                .filter(
                    n ->
                        AbstractStates.extractStateByType(n, PredicateAbstractState.class)
                            .getPathFormula()
                            .getSsa()
                            .containsVariable("main::flag"))
                .findFirst()
                .orElseThrow();
        DssARGPathGraph graph = DssARGPathGraph.of(entry, target);
        List<BooleanFormula> expected = new ArrayList<>();
        for (ARGPath path : enumerate(graph)) {
          paths++;
          Optional<AbstractState> condition =
              operator.computeViolationCondition(path, Optional.empty());
          condition.ifPresent(state -> expected.add(normalize(state, predicateCPA)));
        }
        List<BooleanFormula> actual =
            operator.computeConditions(graph, Optional.empty()).stream()
                .map(state -> normalize(state, predicateCPA))
                .toList();
        BooleanFormula oldUnion = bfmgr.or(expected);
        BooleanFormula newUnion = bfmgr.or(actual);
        assertWithMessage("Backward: %s; forward: %s", oldUnion, newUnion)
            .that(predicateCPA.getSolver().isUnsat(bfmgr.xor(oldUnion, newUnion)))
            .isTrue();
      }
      assertThat(paths).isGreaterThan(1);
    } finally {
      CPAs.closeCpaIfPossible(analysis.getDcpa(), logger);
    }
  }

  private static BooleanFormula normalize(AbstractState state, PredicateCPA cpa) {
    PathFormula path =
        AbstractStates.extractStateByType(state, PredicateAbstractState.class).getPathFormula();
    FormulaManagerView fmgr = cpa.getSolver().getFormulaManager();
    BooleanFormula uninstantiated = fmgr.uninstantiate(path.getFormula());
    // This comparison is valid only when projection removed all intermediate SSA variables.
    assertThat(fmgr.instantiate(uninstantiated, path.getSsa())).isEqualTo(path.getFormula());
    return uninstantiated;
  }

  @Test
  public void differentAssignmentCountsAtJoin() throws Exception {
    compare(
        "int x = flag; if (flag > 0) { x = x + 1; x = x + 2; } else { x = x - 2; } if (x == 7) {"
            + " ERROR: return 1; }");
  }

  @Test
  public void consecutiveDiamondsKeepCorrelations() throws Exception {
    compare(
        "int x; if (flag > 0) x = 1; else x = 2; if (flag < 5) x = x + 3; else x = x + 4; if (x =="
            + " 5) { ERROR: return 1; }");
  }
}
