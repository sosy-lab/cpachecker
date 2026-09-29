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
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

@RunWith(Parameterized.class)
public class DssBlockAnalysisPrecisionTest {

  @Parameterized.Parameters(name = "{0}")
  public static ImmutableList<String> modes() {
    return ImmutableList.of("ALWAYS_REPLACE");
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
  }

  private Harness createHarness() throws Exception {
    return createHarness(true);
  }

  private Harness createHarness(boolean pOptimized) throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFunctionBody("int x = 0; int y = x + 1; return y;");
    BlockNode root = new SingleBlockDecomposition().decompose(cfa).getRoot();
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
            .setOption("distributedSummaries.sharePrecision", Boolean.toString(pOptimized))
            .setOption("distributedSummaries.combineStates", Boolean.toString(pOptimized))
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
    BitvectorFormulaManagerView bv =
        cpa.getSolver().getFormulaManager().getBitvectorFormulaManager();
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
  public void summaryCombinationRetainsPrecisionWithoutSemanticComparisons() throws Exception {
    try (Harness h = createHarness()) {
      AbstractState first = h.analysis().makeStartState(false);
      AbstractState second = h.analysis().makeStartState(false);
      long comparisons = h.stats().getCoverageCounter().getValue();
      ImmutableList<StateAndPrecision> combined =
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
  public void analysisUsesOnlyTheSuppliedPrecision() throws Exception {
    try (Harness h = createHarness()) {
      DssBlockAnalysisResult first =
          h.analysis()
              .runInitialBlockAnalysis(h.analysis().makeStartState(false), h.updatedPrecision());
      assertThat(h.analysis().summariesOf(first)).isNotEmpty();
      for (StateAndPrecision summary : h.analysis().summariesOf(first)) {
        assertThat(
                Precisions.extractPrecisionByType(summary.precision(), PredicatePrecision.class)
                    .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
            .contains(h.predicate());
      }
      DssBlockAnalysisResult second =
          h.analysis()
              .runBlockAnalysis(
                  h.analysis().makeStartState(false),
                  h.analysis().makeStartPrecision(),
                  ImmutableList.of());
      assertThat(h.analysis().summariesOf(second)).isNotEmpty();
      for (StateAndPrecision summary : h.analysis().summariesOf(second)) {
        assertThat(
                Precisions.extractPrecisionByType(summary.precision(), PredicatePrecision.class)
                    .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
            .doesNotContain(h.predicate());
      }
    }
  }

  @Test
  public void plainAnalysisDoesNotCombineStatesOrExchangePrecision() throws Exception {
    try (Harness h = createHarness(false)) {
      AbstractState first = h.analysis().makeStartState(false);
      AbstractState second = h.analysis().makeStartState(false);
      assertThat(h.analysis().combineStates(ImmutableList.of(first, second)))
          .containsExactly(first, second)
          .inOrder();
      StateAndPrecision updated = new StateAndPrecision(first, h.updatedPrecision());
      ImmutableMap<String, String> content = h.analysis().serialize(ImmutableList.of(updated));
      assertThat(content.keySet().stream().anyMatch(key -> key.contains("PredicatePrecision")))
          .isFalse();
      DssPostConditionMessage message =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor", AlgorithmStatus.SOUND_AND_PRECISE, content);
      ImmutableList<StateAndPrecision> restored = h.analysis().deserialize(message);
      assertThat(restored).hasSize(1);
      assertThat(
              Precisions.extractPrecisionByType(
                      restored.getFirst().precision(), PredicatePrecision.class)
                  .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .doesNotContain(h.predicate());
      assertThat(
              Precisions.extractPrecisionByType(
                      h.analysis().combinePrecisions(ImmutableList.of(updated)),
                      PredicatePrecision.class)
                  .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .doesNotContain(h.predicate());
      assertThat(h.analysis().precisionsCoveredBy(ImmutableList.of(updated), restored)).isTrue();
    }
  }

  @Test
  public void statePrecisionSurvivesMessageRoundTrip() throws Exception {
    try (Harness h = createHarness()) {
      ImmutableMap<String, String> content =
          h.analysis()
              .serialize(
                  ImmutableList.of(
                      new StateAndPrecision(
                          h.analysis().makeStartState(false), h.updatedPrecision())));
      DssPostConditionMessage message =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor", AlgorithmStatus.SOUND_AND_PRECISE, content);
      ImmutableList<StateAndPrecision> restored =
          h.analysis().deserialize(DssMessage.fromJson(message.asJson()));
      assertThat(restored).hasSize(1);
      assertThat(
              Precisions.extractPrecisionByType(
                      restored.getFirst().precision(), PredicatePrecision.class)
                  .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
          .contains(h.predicate());
    }
  }

  @Test
  public void newPrecisionOnAnUnchangedPreconditionTriggersOnce() throws Exception {
    try (Harness h = createHarness()) {
      DssPostConditionMessage original =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor",
                  AlgorithmStatus.SOUND_AND_PRECISE,
                  h.analysis()
                      .serialize(
                          ImmutableList.of(
                              new StateAndPrecision(
                                  h.analysis().makeStartState(false),
                                  h.analysis().makeStartPrecision()))));
      DssPostConditionMessage updated =
          h.messages()
              .createDssPostConditionMessage(
                  "predecessor",
                  AlgorithmStatus.SOUND_AND_PRECISE,
                  h.analysis()
                      .serialize(
                          ImmutableList.of(
                              new StateAndPrecision(
                                  h.analysis().makeStartState(false), h.updatedPrecision()))));
      assertThat(h.analysis().storePrecondition(original).shouldProceed()).isTrue();
      assertThat(h.analysis().storePrecondition(updated).shouldProceed()).isTrue();
      assertThat(h.analysis().storePrecondition(updated).shouldProceed()).isFalse();
      assertThat(h.analysis().storePrecondition(original).shouldProceed()).isFalse();
      Collection<DssMessage> messages = h.analysis().analyze(false);
      assertThat(messages).isNotEmpty();
      assertThat(
              messages.stream()
                  .flatMap(
                      m -> {
                        try {
                          return h.analysis().deserialize(m).stream();
                        } catch (Exception e) {
                          throw new AssertionError(e);
                        }
                      })
                  .anyMatch(
                      sap ->
                          Precisions.extractPrecisionByType(
                                  sap.precision(), PredicatePrecision.class)
                              .getPredicates(h.analysis().getBlock().getFinalLocation(), 1)
                              .contains(h.predicate())))
          .isTrue();
    }
  }

  @Test
  public void newPrecisionOnAnUnchangedViolationTriggersOnce() throws Exception {
    try (Harness h = createHarness()) {
      DssViolationConditionMessage original =
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
      DssViolationConditionMessage updated =
          h.messages()
              .createViolationConditionMessage(
                  "successor",
                  AlgorithmStatus.SOUND_AND_PRECISE,
                  h.analysis()
                      .serialize(
                          ImmutableList.of(
                              new StateAndPrecision(
                                  h.analysis().makeStartState(false), h.updatedPrecision()))));
      assertThat(h.analysis().storeViolationCondition(original).shouldProceed()).isTrue();
      assertThat(h.analysis().storeViolationCondition(updated).shouldProceed()).isTrue();
      assertThat(h.analysis().storeViolationCondition(updated).shouldProceed()).isFalse();
      assertThat(h.analysis().storeViolationCondition(original).shouldProceed()).isFalse();
    }
  }

  @Test
  public void violationPathsKeepTheirPrecisionWhenAnotherExplorationRuns() throws Exception {
    try (Harness h = createHarness()) {
      DssBlockAnalysisResult result =
          h.analysis()
              .runInitialBlockAnalysis(h.analysis().makeStartState(false), h.updatedPrecision());
      Set<ArgPathAndCondition> paths =
          h.analysis().pathsFromOrigin(result.getFinalLocationStates());
      assertThat(paths).isNotEmpty();
      h.analysis()
          .runInitialBlockAnalysis(
              h.analysis().makeStartState(false), h.analysis().makeStartPrecision());
      for (ArgPathAndCondition path : paths) {
        assertThat(
                Precisions.extractPrecisionByType(path.precision(), PredicatePrecision.class)
                    .getPredicates(h.analysis().getBlock().getFinalLocation(), 1))
            .contains(h.predicate());
      }
    }
  }
}
