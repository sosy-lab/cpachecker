// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRunRefinerNTimes;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class PredicateStopRefinerTest {
  DummyAlgorithm countCallsToAlgorithmInCEGAR;
  private LogManager logger;
  private Configuration config;
  private ShutdownNotifier shutdownNotifier;
  private ReachedSet reachedSet;
  private CFA cfa;
  private ARGCPA argCpa;

  /**
   * * Create common set up for all StopRefinerPredicateStopRefiner in PredicateDelegatingRefiner *
   * tests.
   */
  @Before
  public void setup()
      throws InvalidConfigurationException, ParserException, InterruptedException, CPAException {
    logger = LogManager.createTestLogManager();
    shutdownNotifier = ShutdownNotifier.createDummy();
    config =
        TestUtils.configurationForTest()
            .setOption("cegar.refiner", "cpa.predicate.PredicateDelegatingRefiner")
            .setOption("analysis.reachedSet.trackChanges", "true")
            .setOption("cpa.predicate.delegatingRefinerHeuristics.RunRefinerNTimes.numberRuns", "2")
            .build();
    cfa =
        TestCfaUtils.toSingleFunctionCFA(
            new CFACreator(config, logger, shutdownNotifier), "  int x; x = 0;return x;");
    PredicateCPA predicateCPA =
        new PredicateCPA(
            config,
            logger,
            new BlockOperator(),
            cfa,
            shutdownNotifier,
            Specification.alwaysSatisfied(),
            AggregatedReachedSets.empty());
    argCpa =
        (ARGCPA)
            ARGCPA
                .factory()
                .set(predicateCPA, ConfigurableProgramAnalysis.class)
                .set(config, Configuration.class)
                .set(logger, LogManager.class)
                .set(Specification.alwaysSatisfied(), Specification.class)
                .set(cfa, CFA.class)
                .createInstance();
    reachedSet = new ReachedSetFactory(config, logger).create(argCpa);
  }

  /**
   * * This test checks that the StopRefiner's early termination signal is received in CEGAR and no
   * further runs in CEGAR are executed.
   */
  @Test
  public void checkTerminationSignalReachesCEGAR()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    ImmutableList<HeuristicDelegatingRefinerRecord> refinerRecords =
        ImmutableList.of(
            new HeuristicDelegatingRefinerRecord(
                new DelegatingRefinerHeuristicRunRefinerNTimes(config, logger), new DummyRefiner()),
            new HeuristicDelegatingRefinerRecord(
                (pReached, pDeltas) -> true, new PredicateStopRefiner()));
    PredicateDelegatingRefiner delegatingRefiner =
        new PredicateDelegatingRefiner(logger, refinerRecords);
    countCallsToAlgorithmInCEGAR = new DummyAlgorithm(delegatingRefiner);
    CEGARAlgorithm cegarAlgorithm =
        new CEGARAlgorithmFactory(
                countCallsToAlgorithmInCEGAR, argCpa, logger, config, shutdownNotifier)
            .newInstance();

    // Dummy Refiner runs once - countCallsToAlgorithmInCEGAR Algorithms gets called once
    boolean dummyRefinerResult = delegatingRefiner.performRefinement(reachedSet);
    cegarAlgorithm.run(reachedSet);
    assertThat(dummyRefinerResult).isTrue();
    assertThat(countCallsToAlgorithmInCEGAR.runCount).isEqualTo(1);

    // StopRefiner is invoked - it throws RefinementFailedException to signal CEGAR
    // to stop verification. countCallsToAlgorithmInCEGAR remains at 1 because
    // the algorithm is never invoked again.
    assertThrows(
        RefinementFailedException.class, () -> delegatingRefiner.performRefinement(reachedSet));
    assertThat(countCallsToAlgorithmInCEGAR.runCount).isEqualTo(1);
  }

  /**
   * This tests checks that the StopRefiner as the only Refiner in DelegatingRefiner signals
   * termination immediately.
   */
  @Test
  public void checkOnlyStopRefinerInDelegatingRefiner() {
    ImmutableList<HeuristicDelegatingRefinerRecord> stopOnly =
        ImmutableList.of(
            new HeuristicDelegatingRefinerRecord(
                (pReached, pDeltas) -> true, new PredicateStopRefiner()));

    PredicateDelegatingRefiner stopOnlyDelegatingRefiner =
        new PredicateDelegatingRefiner(logger, stopOnly);

    countCallsToAlgorithmInCEGAR = new DummyAlgorithm(stopOnlyDelegatingRefiner);

    // When StopRefiner is called as the first and only refiner, it throws
    // RefinementFailedException immediately, and the algorithm's run() method
    // is never executed.
    assertThrows(
        RefinementFailedException.class,
        () -> stopOnlyDelegatingRefiner.performRefinement(reachedSet));
    assertThat(countCallsToAlgorithmInCEGAR.runCount).isEqualTo(0);
  }

  // A dummy algorithm to add to the CEGAR AlgorithmFactory.
  private static class DummyAlgorithm implements Algorithm {
    int runCount = 0;

    @SuppressWarnings("unused")
    Refiner refiner;

    DummyAlgorithm(Refiner pRefiner) {
      this.refiner = pRefiner;
    }

    @Override
    public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
      if (refiner.performRefinement(reachedSet)) {
        runCount++;
      }
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
  }

  // A dummy refiner to serve as Refiner instance in the StopRefinerTests that tracks the number of
  // times it was called.
  private static class DummyRefiner implements Refiner {

    @Override
    public boolean performRefinement(ReachedSet pReached) {
      return true;
    }
  }
}
