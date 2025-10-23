// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

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
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRunNTimes;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

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
        TestDataTools.configurationForTest()
            .setOption("cegar.refiner", "cpa.predicate.PredicateDelegatingRefiner")
            .setOption("analysis.reachedSet.withTracking", "true")
            .build();
    cfa =
        TestDataTools.toSingleFunctionCFA(
            new CFACreator(config, logger, shutdownNotifier), " int x;", " x = 0;", " return x;");
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
   * * This test checks that the termination signal in PredicateDelegatingRefiner is correctly *
   * switched when StopRefiner is called and persists.
   */
  @Test
  public void checkTerminationSignalReceivedInDelegatingRefiner()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    ImmutableList<HeuristicDelegatingRefinerRecord> refinerRecords =
        ImmutableList.of(
            new HeuristicDelegatingRefinerRecord(
                new DelegatingRefinerHeuristicRunNTimes(1), new DummyRefiner()),
            new HeuristicDelegatingRefinerRecord(
                (pReached, pDeltas) -> true, new PredicateStopRefiner()),
            new HeuristicDelegatingRefinerRecord(
                new DelegatingRefinerHeuristicRunNTimes(1), new DummyRefiner()));
    PredicateDelegatingRefiner delegatingRefiner =
        new PredicateDelegatingRefiner(logger, refinerRecords);

    // Refinement with first DummyRefiner - DummyRefiner is called once
    boolean dummyRefinerResult = delegatingRefiner.performRefinement(reachedSet);
    DummyRefiner firstDummyRefiner = (DummyRefiner) refinerRecords.getFirst().pRefiner();
    assertThat(firstDummyRefiner.callsToRefiner).isEqualTo(1);
    assertThat(dummyRefinerResult).isTrue();
    assertThat(delegatingRefiner.shouldTerminateRefinement()).isFalse();

    // Refinement with StopRefiner - termination signal is switched
    boolean stopRefinerResult = delegatingRefiner.performRefinement(reachedSet);
    assertThat(stopRefinerResult).isTrue();
    assertThat(delegatingRefiner.shouldTerminateRefinement()).isTrue();

    // Refinement with second DummyRefiner - termination signal persists - DummyRefiner is never
    // called
    boolean secondDummyRefinerResult = delegatingRefiner.performRefinement(reachedSet);
    DummyRefiner secondDummyRefiner = (DummyRefiner) refinerRecords.getLast().pRefiner();
    assertThat(secondDummyRefinerResult).isTrue();
    assertThat(delegatingRefiner.shouldTerminateRefinement()).isTrue();
    assertThat(secondDummyRefiner.callsToRefiner).isEqualTo(0);
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
                new DelegatingRefinerHeuristicRunNTimes(1), new DummyRefiner()),
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
    assertThat(delegatingRefiner.shouldTerminateRefinement()).isFalse();
    assertThat(countCallsToAlgorithmInCEGAR.runCount).isEqualTo(1);

    // StopRefiner runs - countCallsToAlgorithmInCEGAR Algorithms remains at 1 because algorithm is
    // not called again
    boolean stopRefinerResult = delegatingRefiner.performRefinement(reachedSet);
    cegarAlgorithm.run(reachedSet);
    assertThat(stopRefinerResult).isTrue();
    assertThat(delegatingRefiner.shouldTerminateRefinement()).isTrue();
  }

  /**
   * This tests checks that the StopRefiner as the only Refiner in DelegatingRefiner signals
   * termination immediately.
   */
  @Test
  public void checkOnlyStopRefinerInDelegatingRefiner()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    ImmutableList<HeuristicDelegatingRefinerRecord> stopOnly =
        ImmutableList.of(
            new HeuristicDelegatingRefinerRecord(
                (pReached, pDeltas) -> true, new PredicateStopRefiner()));

    PredicateDelegatingRefiner stopOnlyDelegatingRefiner =
        new PredicateDelegatingRefiner(logger, stopOnly);

    countCallsToAlgorithmInCEGAR = new DummyAlgorithm(stopOnlyDelegatingRefiner);

    CEGARAlgorithm stopCegarAlgorithm =
        new CEGARAlgorithmFactory(
                countCallsToAlgorithmInCEGAR, argCpa, logger, config, shutdownNotifier)
            .newInstance();

    // When StopRefiner is called as first refiner, the algorithm's run() method is never executed
    boolean stopRefinerResult = stopOnlyDelegatingRefiner.performRefinement(reachedSet);
    stopCegarAlgorithm.run(reachedSet);
    assertThat(stopRefinerResult).isTrue();
    assertThat(stopOnlyDelegatingRefiner.shouldTerminateRefinement()).isTrue();
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
      if (!refiner.shouldTerminateRefinement()) {
        runCount++;
      }
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
  }

  // A dummy refiner to serve as Refiner instance in the StopRefinerTests that tracks the number of
  // times it was called.
  private static class DummyRefiner implements Refiner {
    int callsToRefiner = 0;

    @Override
    public boolean performRefinement(ReachedSet pReached) {
      callsToRefiner++;
      return true;
    }
  }
}
