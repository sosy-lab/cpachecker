// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

@RunWith(Parameterized.class)
public class DssBlockAnalysisPrecisionTest {

  @Parameterized.Parameters(name = "{0}")
  public static ImmutableList<String> modes() {
    return ImmutableList.of("ALWAYS_REPLACE", "PARTIAL_REPLACE");
  }

  private final String mode;

  public DssBlockAnalysisPrecisionTest(String pMode) {
    mode = pMode;
  }

  private record Harness(
      DssBlockAnalysis analysis,
      DssMessageFactory messages,
      DssSingleWorkerStatistics stats,
      AbstractionPredicate predicate,
      Precision updatedPrecision)
      implements AutoCloseable {
    @Override
    public void close() {
      CPAs.closeCpaIfPossible(analysis.getDcpa(), LogManager.createTestLogManager());
    }

    private ImmutableMap<String, String> content(boolean pPrecisionOnly) throws Exception {
      Map<String, String> result =
          new LinkedHashMap<>(
              analysis.serialize(
                  pPrecisionOnly
                      ? ImmutableList.of()
                      : ImmutableList.of(
                          new StateAndPrecision(
                              analysis.makeStartState(false), analysis.makeStartPrecision()))));
      analysis
          .getDcpa()
          .getSerializePrecisionOperator()
          .serializePrecision(updatedPrecision)
          .forEach((key, value) -> result.put(DssMessage.SHARED_PRECISION_KEY + "." + key, value));
      if (pPrecisionOnly) {
        result.put(DssMessage.PRECISION_ONLY_KEY, "true");
      }
      return ImmutableMap.copyOf(result);
    }

    private void storePrecondition() throws Exception {
      DssPostConditionMessage message =
          messages.createDssPostConditionMessage(
              "predecessor",
              AlgorithmStatus.SOUND_AND_PRECISE,
              analysis.serialize(
                  ImmutableList.of(
                      new StateAndPrecision(
                          analysis.makeStartState(false), analysis.makeStartPrecision()))));
      assertThat(analysis.storePrecondition(message).shouldProceed()).isTrue();
    }
  }

  private Harness createHarness() throws Exception {
    return createHarness(false);
  }

  private Harness createHarness(boolean seedBoundaries) throws Exception {
    CFA cfa =
        seedBoundaries
            ? TestCfaUtils.makeCfaFromString("int main(int x) { if (x > 0) return 1; return 0; }")
            : TestCfaUtils.makeCfaFromFunctionBody("int x = 0; int y = x + 1; return y;");
    BlockNode root = new SingleBlockDecomposition().decompose(cfa).getRoot();
    if (seedBoundaries) {
      root =
          new LinearBlockNodeDecomposition(DssTestUtils.createBlockOperator(cfa))
              .decompose(cfa).getNodes().stream()
                  .filter(
                      b ->
                          b.getInitialLocation().getNumLeavingEdges() > 0
                              && b.getInitialLocation().getLeavingEdge(0) instanceof AssumeEdge)
                  .findFirst()
                  .orElseThrow();
    }
    BlockNode block =
        new BlockNode(
            "block",
            root.getInitialLocation(),
            root.getFinalLocation(),
            root.getNodes(),
            root.getEdges(),
            ImmutableSet.of("predecessor"),
            ImmutableSet.of("successor"));
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(DssTestUtils.DSS_FORWARD_CONFIGURATION_FILE)
            .setOption("distributedSummaries.blockAnalysisType", mode)
            .setOption(
                "distributedSummaries.seedBoundaryAssumptions", Boolean.toString(seedBoundaries))
            .setOption("distributedSummaries.resetCallstackState", "true")
            .setOption(
                "cpa.predicate.blk.alwaysAtGivenNodes",
                block.getInitialLocation().getNodeNumber()
                    + ","
                    + block.getFinalLocation().getNodeNumber())
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
    DssMessageFactory messages = new DssMessageFactory(options);
    DssSingleWorkerStatistics stats = new DssSingleWorkerStatistics("block");
    DssBlockAnalysis analysis =
        new DssBlockAnalysis(logger, block, cfa, spec, config, options, messages, shutdown, stats);
    PredicateCPA cpa =
        (PredicateCPA) CPAs.retrieveCPA(analysis.getDcpa(), DistributedPredicateCPA.class).getCPA();
    var bv = cpa.getSolver().getFormulaManager().getBitvectorFormulaManager();
    AbstractionPredicate predicate =
        cpa.getAbstractionManager()
            .makePredicate(bv.equal(bv.makeVariable(32, "main::x"), bv.makeBitvector(32, 0)));
    PredicatePrecision precision =
        new PredicatePrecision(
            ImmutableListMultimap.of(),
            ImmutableListMultimap.of(block.getFinalLocation(), predicate),
            ImmutableListMultimap.of(),
            ImmutableSet.of());
    Precision updated =
        Precisions.replaceByType(
            analysis.makeStartPrecision(), precision, PredicatePrecision.class::isInstance);
    return new Harness(analysis, messages, stats, predicate, updated);
  }

  @Test
  public void boundarySeedsReachInitialPrecisionThroughArgAndCompositeWrappers() throws Exception {
    try (Harness h = createHarness(true)) {
      PredicatePrecision precision =
          Precisions.extractPrecisionByType(
              h.analysis().makeStartPrecision(), PredicatePrecision.class);
      assertThat(precision.getLocalPredicates().get(h.analysis().getBlock().getInitialLocation()))
          .hasSize(1);
      assertThat(precision.getGlobalPredicates()).isEmpty();
    }
  }

  @Test
  public void summaryCombinationRetainsPrecisionWithoutSemanticComparisons() throws Exception {
    try (Harness h = createHarness()) {
      var first = h.analysis().makeStartState(false);
      var second = h.analysis().makeStartState(false);
      long comparisons = h.stats().getCoverageCounter().getValue();
      var combined =
          h.analysis()
              .combineSummaries(
                  ImmutableList.of(
                      new StateAndPrecision(first, h.analysis().makeStartPrecision()),
                      new StateAndPrecision(second, h.updatedPrecision())));
      assertThat(combined).hasSize(1);
      assertThat(h.stats().getCoverageCounter().getValue()).isEqualTo(comparisons);
      PredicatePrecision precision =
          Precisions.extractPrecisionByType(
              combined.getFirst().precision(), PredicatePrecision.class);
      assertThat(precision.getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .contains(h.predicate());
    }
  }

  @Test
  public void unchangedStateWithNewPrecisionTriggersAnalysisOnce() throws Exception {
    try (Harness h = createHarness()) {
      h.storePrecondition();
      DssPostConditionMessage update =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor", AlgorithmStatus.SOUND_AND_PRECISE, h.content(false));
      assertThat(h.analysis().storePrecondition(update).shouldProceed()).isTrue();
      assertThat(h.analysis().storePrecondition(update).shouldProceed()).isFalse();
      h.analysis().analyze(false);
      PredicatePrecision used =
          Precisions.extractPrecisionByType(
              h.analysis().precisionOfLastAnalysis(), PredicatePrecision.class);
      assertThat(used.getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .contains(h.predicate());
    }
  }

  @Test
  public void unchangedViolationConditionWithNewPrecisionTriggersAnalysisOnce() throws Exception {
    try (Harness h = createHarness()) {
      var previous =
          h.messages()
              .createViolationConditionMessage(
                  "successor",
                  AlgorithmStatus.SOUND_AND_PRECISE,
                  h.analysis()
                      .serialize(
                          ImmutableList.of(
                              new StateAndPrecision(
                                  h.analysis().makeStartState(false),
                                  h.analysis().makeStartPrecision()))));
      assertThat(h.analysis().storeViolationCondition(previous).shouldProceed()).isTrue();
      long previousVersion = h.analysis().precisionVersion();
      var update =
          h.messages()
              .createViolationConditionMessage(
                  "successor", AlgorithmStatus.SOUND_AND_PRECISE, h.content(false));
      assertThat(h.analysis().storeViolationCondition(update).shouldProceed()).isTrue();
      assertThat(h.analysis().precisionVersion()).isGreaterThan(previousVersion);
      assertThat(h.analysis().storeViolationCondition(update).shouldProceed()).isFalse();
    }
  }

  @Test
  public void backwardPrecisionRefreshesCachedSourceAndIsRetained() throws Exception {
    try (Harness h = createHarness()) {
      h.storePrecondition();
      h.analysis().analyze(false);
      int previousRuns = h.stats().getBlockAnalysisCounter().getUpdateCount();
      var update =
          h.messages()
              .createViolationConditionMessage(
                  "successor", AlgorithmStatus.SOUND_AND_PRECISE, h.content(true));
      assertThat(h.analysis().storeViolationCondition(update).shouldProceed()).isTrue();
      assertThat(h.analysis().storeViolationCondition(update).shouldProceed()).isFalse();
      var messages = h.analysis().analyze(true);
      assertThat(h.stats().getBlockAnalysisCounter().getUpdateCount()).isGreaterThan(previousRuns);
      assertThat(messages.stream().anyMatch(m -> m.hasPrecisionUpdate() && !m.isPrecisionOnly()))
          .isTrue();
      PredicatePrecision used =
          Precisions.extractPrecisionByType(
              h.analysis().precisionOfLastAnalysis(), PredicatePrecision.class);
      assertThat(used.getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .contains(h.predicate());
      assertThat(h.analysis().storeViolationCondition(update).shouldProceed()).isFalse();
    }
  }

  @Test
  public void precisionOnlyUpdateDoesNotMakeUnreachablePredecessorReachable() throws Exception {
    try (Harness h = createHarness()) {
      h.analysis()
          .storePrecondition(
              h.messages()
                  .createDssUnreachableBlockEndMessage(
                      "predecessor", AlgorithmStatus.SOUND_AND_PRECISE));
      var update =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor", AlgorithmStatus.SOUND_AND_PRECISE, h.content(true));
      assertThat(h.analysis().storePrecondition(update).shouldProceed()).isTrue();
      assertThat(
              h.analysis().analyze(false).stream()
                  .anyMatch(DssMessage::indicatesUnreachableBlockEnd))
          .isTrue();
      assertThat(h.analysis().storePrecondition(update).shouldProceed()).isFalse();
    }
  }
}
