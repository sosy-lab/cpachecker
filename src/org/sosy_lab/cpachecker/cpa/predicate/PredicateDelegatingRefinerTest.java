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
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicReachedSetRatio;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRedundantPredicates;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicStaticRefinement;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerRefinerType;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class PredicateDelegatingRefinerTest {
  private Configuration config;
  private LogManager logger;
  private ShutdownNotifier shutdownNotifier;
  private BlockOperator blk;
  private Specification spec;
  private CFA cfa;
  private AggregatedReachedSets reachedSet;
  private Refiner staticRefiner;
  private Refiner defaultRefiner;

  /**
   * Create shared components for the DelegatingRefiner configuration tests that do not change
   * between different tests.
   */
  @Before
  public void setupShared() {
    logger = LogManager.createTestLogManager();
    shutdownNotifier = ShutdownNotifier.createDummy();
    blk = new BlockOperator();
    spec = Specification.alwaysSatisfied();
    reachedSet = AggregatedReachedSets.empty();
    staticRefiner = new DummyRefiner();
    defaultRefiner = new DummyRefiner();
  }

  // Creates a PredicateCPARefinerFactory with the command-line options for different refinement
  // configurations
  private PredicateCPARefinerFactory setUpRefinerFactory(Configuration pConfig) throws Exception {
    config = pConfig;
    cfa =
        TestDataTools.toSingleFunctionCFA(
            new CFACreator(config, logger, shutdownNotifier),
            "  int x;",
            "  x = 0;",
            "  return x;");
    PredicateCPA predicateCPA =
        new PredicateCPA(config, logger, blk, cfa, shutdownNotifier, spec, reachedSet);
    ARGCPA argCpa =
        (ARGCPA)
            ARGCPA
                .factory()
                .set(predicateCPA, ConfigurableProgramAnalysis.class)
                .set(config, Configuration.class)
                .set(logger, LogManager.class)
                .set(Specification.alwaysSatisfied(), Specification.class)
                .set(cfa, CFA.class)
                .createInstance();

    return new PredicateCPARefinerFactory(argCpa);
  }

  // Creates a default map of available refiners for the DelegatingRefiner
  private ImmutableMap<DelegatingRefinerRefinerType, Refiner> setUpRefinerMap(
      PredicateCPARefinerFactory pRefinerFactory) {
    return pRefinerFactory.buildRefinerMap(defaultRefiner, staticRefiner);
  }

  /**
   * This test checks if DelegatingRefiner parses the command-line input for a custom reached
   * set/refinement number ratio for the DelegatingRefinerHeuristicRunNTimes correctly.
   */
  @Test
  public void setUpDefaultRefinementIndividualRuns() throws Exception {
    Configuration pDefaultIndividualRunsConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REACHED_SET_RATIO:DEFAULT")
            .setOption(
                "cpa.predicate.delegatingRefinerHeuristics.ReachedSetRatio.abstractionLocationRefinementRatio",
                "5.0")
            .build();

    PredicateCPARefinerFactory pDefaultIndividualRunsRefinerFactory =
        setUpRefinerFactory(pDefaultIndividualRunsConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pDefaultIndividualRunsRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pDefaultIndividualRunsRefinerFactory));

    assertThat(
            ((DelegatingRefinerHeuristicReachedSetRatio) pRefinerRecords.getFirst().pHeuristic())
                .getAbstractionLocationRefinementRatio())
        .isEqualTo(5.0);
  }

  /**
   * This test checks if DelegatingRefiner parses the command-line input for a custom redundancy
   * threshold for the predicate redundancy heuristic correctly.
   */
  @Test
  public void setUpRedundantHeuristicCustomThreshold() throws Exception {
    Configuration pRedundantCustomThresholdConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REDUNDANT_PREDICATES:DEFAULT")
            .setOption("cpa.predicate.refinement.acceptableRedundancyThreshold", "0.1")
            .build();
    PredicateCPARefinerFactory pRedundantCustomThresholdRefinerFactory =
        setUpRefinerFactory(pRedundantCustomThresholdConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pRedundantCustomThresholdRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pRedundantCustomThresholdRefinerFactory));

    assertThat(
            ((DelegatingRefinerHeuristicRedundantPredicates)
                    pRefinerRecords.getFirst().pHeuristic())
                .getRedundancyThreshold())
        .isEqualTo(0.1);
  }

  /**
   * This test checks if DelegatingRefiner parses the command-line input multiple heuristic-refiner
   * pairs.
   */
  @Test
  public void setUpMultipleRefinerHeuristicPairs() throws Exception {
    Configuration pMultipleConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:STATIC,STATIC:STATIC")
            .build();
    PredicateCPARefinerFactory pMultipleRefinerFactory = setUpRefinerFactory(pMultipleConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pMultipleRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pMultipleRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicRedundantPredicates.class);
    assertThat(pRefinerRecords.getLast().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicStaticRefinement.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords.getLast().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords).hasSize(2);
  }

  /** This test checks if DelegatingRefiner parses command-line input case-insensitively. */
  @Test
  public void checkCaseInsensitivity() throws Exception {
    Configuration plowerCaseConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "static:STATIC,STATIC:default")
            .build();
    PredicateCPARefinerFactory pLowerCaseRefinerFactory = setUpRefinerFactory(plowerCaseConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pLowerCaseRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pLowerCaseRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicStaticRefinement.class);
    assertThat(pRefinerRecords.getLast().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicStaticRefinement.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords.getLast().pRefiner()).isSameInstanceAs(defaultRefiner);
  }

  /**
   * This test checks if DelegatingRefiner ignores whitespaces around the colon in command-line
   * input.
   */
  @Test
  public void ignoreWhiteSpaceColon() throws Exception {
    Configuration pIgnoreWhiteSpaceColonConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATIC : STATIC")
            .build();
    PredicateCPARefinerFactory pIgnoreWhiteSpaceColonConfigRefinerFactory =
        setUpRefinerFactory(pIgnoreWhiteSpaceColonConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pIgnoreWhiteSpaceColonConfigRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pIgnoreWhiteSpaceColonConfigRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicStaticRefinement.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords).hasSize(1);
  }

  /**
   * This test checks if DelegatingRefiner ignores whitespaces between the heuristic-refiner pairs
   * in command-line input.
   */
  @Test
  public void ignoreWhiteSpaceComma() throws Exception {
    Configuration pIgnoreWhiteSpaceCommaConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs",
                "STATIC:STATIC, REACHED_SET_RATIO:DEFAULT ,REDUNDANT_PREDICATES:DEFAULT")
            .build();
    PredicateCPARefinerFactory pIgnoreWhiteSpaceCommaConfigRefinerFactory =
        setUpRefinerFactory(pIgnoreWhiteSpaceCommaConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pIgnoreWhiteSpaceCommaConfigRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pIgnoreWhiteSpaceCommaConfigRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicStaticRefinement.class);
    assertThat(pRefinerRecords.get(1).pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicReachedSetRatio.class);
    assertThat(pRefinerRecords.get(2).pHeuristic())
        .isInstanceOf(DelegatingRefinerHeuristicRedundantPredicates.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords.get(1).pRefiner()).isSameInstanceAs(defaultRefiner);
    assertThat(pRefinerRecords.get(2).pRefiner()).isSameInstanceAs(defaultRefiner);
    assertThat(pRefinerRecords).hasSize(3);
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g. a
   * different separator from a comma, in the command-line input.
   */
  @Test
  public void checkOtherSeparators() throws Exception {
    Configuration pOtherSeparatorsConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs",
                "STATIC:STATIC;REACHED_SET_RATIO:DEFAULT")
            .build();
    PredicateCPARefinerFactory pIgnoreOtherSeparatorsRefinerFactory =
        setUpRefinerFactory(pOtherSeparatorsConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pIgnoreOtherSeparatorsRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pIgnoreOtherSeparatorsRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * missing a colon, in the command-line input.
   */
  @Test
  public void checkMissingColon() throws Exception {
    Configuration pMissingColonConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATICSTATIC")
            .build();
    PredicateCPARefinerFactory pMissingColonRefinerFactory =
        setUpRefinerFactory(pMissingColonConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pMissingColonRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pMissingColonRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * having only one argument, in the command-line input.
   */
  @Test
  public void checkOnlyComponentInPair() throws Exception {
    Configuration pOnlyOneComponentConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATIC")
            .build();
    PredicateCPARefinerFactory pOnlyOneComponentRefinerFactory =
        setUpRefinerFactory(pOnlyOneComponentConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pOnlyOneComponentRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pOnlyOneComponentRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * having three arguments, in the command-line input.
   */
  @Test
  public void checkThreeComponentsInPair() throws Exception {
    Configuration pThreeComponentsConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATIC:STATIC:STATIC")
            .build();
    PredicateCPARefinerFactory pThreeComponentsRefinerFactory =
        setUpRefinerFactory(pThreeComponentsConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pThreeComponentsRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pThreeComponentsRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception an unknown heuristic in the
   * command-line input.
   */
  @Test
  public void checkUnknownHeuristic() throws Exception {
    Configuration pUnknownHeuristicConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "FOO:STATIC")
            .build();
    PredicateCPARefinerFactory pUnknownHeuristicRefinerFactory =
        setUpRefinerFactory(pUnknownHeuristicConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pUnknownHeuristicRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pUnknownHeuristicRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception an unknown refiner in the
   * command-line input.
   */
  @Test
  public void checkUnknownRefiner() throws Exception {
    Configuration pUnknownRefinerConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATIC:FOO")
            .build();
    PredicateCPARefinerFactory pUnkmownRefinerRefinerFactory =
        setUpRefinerFactory(pUnknownRefinerConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pUnkmownRefinerRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pUnkmownRefinerRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a negative number of the reached
   * set/refinement number ratio for the run n-times heuristic in the command-line input.
   */
  @Test
  public void checkNegativeFixedRuns() throws Exception {
    Configuration pNegativeFixedRunsConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REACHED_SET_RATIO:DEFAULT")
            .setOption(
                "cpa.predicate.delegatingRefinerHeuristics.ReachedSetRatio.abstractionLocationRefinementRatio",
                "-10")
            .build();
    PredicateCPARefinerFactory pNegativeFixedRunsFactory =
        setUpRefinerFactory(pNegativeFixedRunsConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pNegativeFixedRunsFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pNegativeFixedRunsFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a negative number for
   * redundancyThreshold for the REDUNDANT_PREDICATES heuristic.
   */
  @Test
  public void checkNegativeRedundancyThreshold() throws Exception {
    Configuration pNegativeRedundancyConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REDUNDANT_PREDICATES:DEFAULT")
            .setOption("cpa.predicate.refinement.acceptableRedundancyThreshold", "-0.1")
            .build();
    PredicateCPARefinerFactory pNegativeRedundancyRefinerFactory =
        setUpRefinerFactory(pNegativeRedundancyConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pNegativeRedundancyRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pNegativeRedundancyRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a number larger than 1.0 (i.e.
   * 100%) for redundancyThreshold for the redundant predicates heuristic.
   */
  @Test
  public void checkTooLargeRedundancyThreshold() throws Exception {
    Configuration pTooLargeRedundancyConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REDUNDANT_PREDICATES:DEFAULT")
            .setOption("cpa.predicate.refinement.acceptableRedundancyThreshold", "2.0")
            .build();
    PredicateCPARefinerFactory pTooLargeRedundancyRefinerFactory =
        setUpRefinerFactory(pTooLargeRedundancyConfig);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pTooLargeRedundancyRefinerFactory.createDelegatingRefinerConfig(
                setUpRefinerMap(pTooLargeRedundancyRefinerFactory)));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for non-double command-line input for
   * redundancyThreshold for the redundant predicates heuristic.
   */
  @Test
  public void checkStringRedundancyThreshold() throws Exception {
    Configuration pStringRedundancyThresholdConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REDUNDANT_PREDICATES:DEFAULT")
            .setOption("cpa.predicate.refinement.acceptableRedundancyThreshold", "xyz")
            .build();

    assertThrows(
        InvalidConfigurationException.class,
        () -> setUpRefinerFactory(pStringRedundancyThresholdConfig));
  }

  // A dummy refiner to serve as Refiner instances the DelegatingRefiner adds to its map of
  // available refiners.
  private static class DummyRefiner implements Refiner {

    @Override
    public boolean performRefinement(ReachedSet pReached)
        throws CPAException, InterruptedException {
      return false;
    }

    @Override
    public boolean shouldTerminateRefinement() {
      return true;
    }
  }
}
