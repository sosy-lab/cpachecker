// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DssBlockEntryInvariantsTest {
  private PredicateCPA predicate;
  private Configuration config;
  private CFA cfa;
  private final LogManager logger = LogManager.createTestLogManager();

  @Before
  public void setUp() throws Exception {
    cfa = TestCfaUtils.makeCfaFromString("int main() { int x = 0; while (x < 2) x++; return x; }");
    config =
        TestUtils.configurationForTest()
            .loadFromFile(
                Path.of("config/distributed-summary-synthesis/dss-entry-invariants.properties"))
            .setOption("cpa.invariants.analyzeRelevantVariablesOnly", "false")
            .setOption("cpa.invariants.analyzeTargetPathsOnly", "false")
            .build();
    var predicateConfig =
        TestUtils.configurationForTest()
            .setOption("solver.solver", "SMTINTERPOL")
            .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
            .setOption("cpa.predicate.encodeFloatAs", "RATIONAL")
            .setOption("cpa.predicate.handlePointerAliasing", "false")
            .build();
    predicate =
        (PredicateCPA)
            PredicateCPA.factory()
                .setConfiguration(predicateConfig)
                .setLogger(logger)
                .setShutdownNotifier(ShutdownNotifier.createDummy())
                .set(cfa, CFA.class)
                .set(Specification.alwaysSatisfied(), Specification.class)
                .set(AggregatedReachedSets.empty(), AggregatedReachedSets.class)
                .createInstance();
  }

  @After
  public void tearDown() throws Exception {
    predicate.close();
  }

  @Test
  public void completedPassRetainsTheSmallLoopRange() throws Exception {
    var result =
        DssBlockEntryInvariants.compute(
            cfa,
            cfa.nodes(),
            Specification.alwaysSatisfied(),
            config,
            predicate,
            logger,
            ShutdownManager.create(),
            TimeSpan.ofSeconds(10));
    assertThat(result).isNotEmpty();
    var loopHead = cfa.getLoopStructure().orElseThrow().getAllLoopHeads().iterator().next();
    assertThat(result).containsKey(loopHead);
    var fmgr = predicate.getSolver().getFormulaManager();
    var formula = fmgr.parse(result.get(loopHead).formula());
    var integers = fmgr.getIntegerFormulaManager();
    var outside = integers.equal(integers.makeVariable("main::x"), integers.makeNumber(3));
    assertThat(predicate.getSolver().isUnsat(fmgr.getBooleanFormulaManager().and(formula, outside)))
        .isTrue();
    assertThat(predicate.getSolver().isUnsat(formula)).isFalse();
  }

  @Test
  public void timeoutDiscardsTheWholePassWithoutCancellingDss() throws Exception {
    var parent = ShutdownManager.create();
    var result =
        DssBlockEntryInvariants.compute(
            cfa,
            cfa.nodes(),
            Specification.alwaysSatisfied(),
            config,
            predicate,
            logger,
            parent,
            TimeSpan.ofNanos(1));
    assertThat(result).isEmpty();
    assertThat(parent.getNotifier().shouldShutdown()).isFalse();
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  public void parentCancellationIsNotSwallowedAsAnOptionalTimeout() {
    var parent = ShutdownManager.create();
    parent.requestShutdown("cancel analysis");
    assertThrows(
        InterruptedException.class,
        () ->
            DssBlockEntryInvariants.compute(
                cfa,
                cfa.nodes(),
                Specification.alwaysSatisfied(),
                config,
                predicate,
                logger,
                parent,
                TimeSpan.ofSeconds(10)));
  }
}
