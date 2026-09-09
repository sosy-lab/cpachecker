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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicInterpolationRate;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicReachedSetRatio;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicRedundantPredicates;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicRefinerRecord;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicResultNegation;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics.ProgressBasedRefinementSelectionRefinerType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class ProgressBasedRefinementSelectionTest {
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
   * Create shared components for the ProgressBasedRefinementSelection configuration tests that do
   * not change between different tests.
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

    cfa = TestCfaUtils.makeCfaFromFunctionBody("  int x; x = 0;return x;");

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

  // Creates a default map of available refiners for the ProgressBasedRefinementSelection
  private ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> setUpRefinerMap(
      PredicateCPARefinerFactory pRefinerFactory) throws InvalidConfigurationException {
    return pRefinerFactory.buildRefinerMap(defaultRefiner, staticRefiner);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection parses the command-line input for a custom
   * reached set/refinement number ratio for the
   * ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes correctly.
   */
  @Test
  public void setUpDefaultRefinementIndividualRuns() throws Exception {
    Configuration pDefaultIndividualRunsConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REACHED_SET_RATIO:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.ReachedSetRatio.abstractionLocationRefinementRatio",
                "5.0")
            .build();

    PredicateCPARefinerFactory pDefaultIndividualRunsRefinerFactory =
        setUpRefinerFactory(pDefaultIndividualRunsConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pDefaultIndividualRunsRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pDefaultIndividualRunsRefinerFactory));

    assertThat(
            ((ProgressBasedRefinementSelectionHeuristicReachedSetRatio)
                    pRefinerRecords.getFirst().pHeuristic())
                .getAbstractionLocationRefinementRatio())
        .isEqualTo(5.0);
  }

  /**
   * This test checks if progressBasedRefinementSelection correctly instantiates a negated heuristic
   * and if that heuristics correctly negates the result of another heuristic.
   */
  @Test
  public void setUpNegatedHeuristic() throws Exception {
    Configuration pNegatedConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "NEGATED(RUNREFINERNTIMES):DEFAULT,NEGATED(INTERPOLATION_RATE):DEFAULT")
            .build();

    PredicateCPARefinerFactory pNegatedRunsRefinerFactory = setUpRefinerFactory(pNegatedConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pNegatedRunsRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pNegatedRunsRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicResultNegation.class);
    assertThat(pRefinerRecords.getLast().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicResultNegation.class);

    ProgressBasedRefinementSelectionHeuristicResultNegation firstHeuristic =
        (ProgressBasedRefinementSelectionHeuristicResultNegation)
            pRefinerRecords.getFirst().pHeuristic();
    assertThat(firstHeuristic.getDelegateHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);

    ProgressBasedRefinementSelectionHeuristicResultNegation secondHeuristic =
        (ProgressBasedRefinementSelectionHeuristicResultNegation)
            pRefinerRecords.getLast().pHeuristic();
    assertThat(secondHeuristic.getDelegateHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicInterpolationRate.class);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection parses the command-line input for a custom
   * redundancy threshold for the predicate redundancy heuristic correctly.
   */
  @Test
  public void setUpRedundantHeuristicCustomThreshold() throws Exception {
    Configuration pRedundantCustomThresholdConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.RedundantPredicates.redundancyThreshold",
                "0.1")
            .build();
    PredicateCPARefinerFactory pRedundantCustomThresholdRefinerFactory =
        setUpRefinerFactory(pRedundantCustomThresholdConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pRedundantCustomThresholdRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pRedundantCustomThresholdRefinerFactory));

    assertThat(
            ((ProgressBasedRefinementSelectionHeuristicRedundantPredicates)
                    pRefinerRecords.getFirst().pHeuristic())
                .getRedundancyThreshold())
        .isEqualTo(0.1);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection parses the command-line input multiple
   * heuristic-refiner pairs.
   */
  @Test
  public void setUpMultipleRefinerHeuristicPairs() throws Exception {
    Configuration pMultipleConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:STATIC,RUNREFINERNTIMES:STATIC")
            .build();
    PredicateCPARefinerFactory pMultipleRefinerFactory = setUpRefinerFactory(pMultipleConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pMultipleRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pMultipleRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRedundantPredicates.class);
    assertThat(pRefinerRecords.getLast().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords.getLast().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords).hasSize(2);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection parses command-line input
   * case-insensitively.
   */
  @Test
  public void checkCaseInsensitivity() throws Exception {
    Configuration plowerCaseConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "runrefinerntimes:STATIC,RUNREFINERNTIMES:default")
            .build();
    PredicateCPARefinerFactory pLowerCaseRefinerFactory = setUpRefinerFactory(plowerCaseConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pLowerCaseRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pLowerCaseRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);
    assertThat(pRefinerRecords.getLast().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords.getLast().pRefiner()).isSameInstanceAs(defaultRefiner);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection ignores whitespaces around the colon in
   * command-line input.
   */
  @Test
  public void ignoreWhiteSpaceColon() throws Exception {
    Configuration pIgnoreWhiteSpaceColonConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES : STATIC")
            .build();
    PredicateCPARefinerFactory pIgnoreWhiteSpaceColonConfigRefinerFactory =
        setUpRefinerFactory(pIgnoreWhiteSpaceColonConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pIgnoreWhiteSpaceColonConfigRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pIgnoreWhiteSpaceColonConfigRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);
    assertThat(pRefinerRecords.getFirst().pRefiner()).isSameInstanceAs(staticRefiner);
    assertThat(pRefinerRecords).hasSize(1);
  }

  /**
   * This test checks if ProgressBasedRefinementSelection ignores whitespaces between the
   * heuristic-refiner pairs in command-line input.
   */
  @Test
  public void ignoreWhiteSpaceComma() throws Exception {
    Configuration pIgnoreWhiteSpaceCommaConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES:STATIC, REACHED_SET_RATIO:DEFAULT ,REDUNDANT_PREDICATES:DEFAULT")
            .build();
    PredicateCPARefinerFactory pIgnoreWhiteSpaceCommaConfigRefinerFactory =
        setUpRefinerFactory(pIgnoreWhiteSpaceCommaConfig);

    ImmutableList<ProgressBasedRefinementSelectionHeuristicRefinerRecord> pRefinerRecords =
        pIgnoreWhiteSpaceCommaConfigRefinerFactory.createProgressBasedRefinementSelectionConfig(
            setUpRefinerMap(pIgnoreWhiteSpaceCommaConfigRefinerFactory));

    assertThat(pRefinerRecords.getFirst().pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRunRefinerNTimes.class);
    assertThat(pRefinerRecords.get(1).pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicReachedSetRatio.class);
    assertThat(pRefinerRecords.get(2).pHeuristic())
        .isInstanceOf(ProgressBasedRefinementSelectionHeuristicRedundantPredicates.class);
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
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES:STATIC;REACHED_SET_RATIO:DEFAULT")
            .build();
    PredicateCPARefinerFactory pIgnoreOtherSeparatorsRefinerFactory =
        setUpRefinerFactory(pOtherSeparatorsConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pIgnoreOtherSeparatorsRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pIgnoreOtherSeparatorsRefinerFactory.createProgressBasedRefinementSelectionConfig(
                refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * missing a colon, in the command-line input.
   */
  @Test
  public void checkMissingColon() throws Exception {
    Configuration pMissingColonConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMESSTATIC")
            .build();
    PredicateCPARefinerFactory pMissingColonRefinerFactory =
        setUpRefinerFactory(pMissingColonConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pMissingColonRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () -> pMissingColonRefinerFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * having only one argument, in the command-line input.
   */
  @Test
  public void checkOnlyComponentInPair() throws Exception {
    Configuration pOnlyOneComponentConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES")
            .build();
    PredicateCPARefinerFactory pOnlyOneComponentRefinerFactory =
        setUpRefinerFactory(pOnlyOneComponentConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pOnlyOneComponentRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pOnlyOneComponentRefinerFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for an invalid pair format, e.g.
   * having three arguments, in the command-line input.
   */
  @Test
  public void checkThreeComponentsInPair() throws Exception {
    Configuration pThreeComponentsConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES:RUNREFINERNTIMES:STATIC")
            .build();
    PredicateCPARefinerFactory pThreeComponentsRefinerFactory =
        setUpRefinerFactory(pThreeComponentsConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pThreeComponentsRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pThreeComponentsRefinerFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception an unknown heuristic in the
   * command-line input.
   */
  @Test
  public void checkUnknownHeuristic() throws Exception {
    Configuration pUnknownHeuristicConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "FOO:RUNREFINERNTIMES")
            .build();
    PredicateCPARefinerFactory pUnknownHeuristicRefinerFactory =
        setUpRefinerFactory(pUnknownHeuristicConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pUnknownHeuristicRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pUnknownHeuristicRefinerFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception an unknown refiner in the
   * command-line input.
   */
  @Test
  public void checkUnknownRefiner() throws Exception {
    Configuration pUnknownRefinerConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "RUNREFINERNTIMES:FOO")
            .build();
    PredicateCPARefinerFactory pUnknownRefinerRefinerFactory =
        setUpRefinerFactory(pUnknownRefinerConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pUnknownRefinerRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () -> pUnknownRefinerRefinerFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a negative number of the reached
   * set/refinement number ratio for the run n-times heuristic in the command-line input.
   */
  @Test
  public void checkNegativeFixedRuns() throws Exception {
    Configuration pNegativeFixedRunsConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REACHED_SET_RATIO:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.ReachedSetRatio.abstractionLocationRefinementRatio",
                "-10")
            .build();
    PredicateCPARefinerFactory pNegativeFixedRunsFactory =
        setUpRefinerFactory(pNegativeFixedRunsConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pNegativeFixedRunsFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () -> pNegativeFixedRunsFactory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a negative number for
   * redundancyThreshold for the REDUNDANT_PREDICATES heuristic.
   */
  @Test
  public void checkNegativeRedundancyThreshold() throws Exception {
    Configuration pNegativeRedundancyConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.RedundantPredicates.redundancyThreshold",
                "-0.1")
            .build();
    PredicateCPARefinerFactory pNegativeRedundancyRefinerFactory =
        setUpRefinerFactory(pNegativeRedundancyConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pNegativeRedundancyRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pNegativeRedundancyRefinerFactory.createProgressBasedRefinementSelectionConfig(
                refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for a number larger than 1.0 (i.e.
   * 100%) for redundancyThreshold for the redundant predicates heuristic.
   */
  @Test
  public void checkTooLargeRedundancyThreshold() throws Exception {
    Configuration pTooLargeRedundancyConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.RedundantPredicates.redundancyThreshold",
                "2.0")
            .build();
    PredicateCPARefinerFactory pTooLargeRedundancyRefinerFactory =
        setUpRefinerFactory(pTooLargeRedundancyConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(pTooLargeRedundancyRefinerFactory);

    assertThrows(
        InvalidConfigurationException.class,
        () ->
            pTooLargeRedundancyRefinerFactory.createProgressBasedRefinementSelectionConfig(
                refiners));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception for non-double command-line input for
   * redundancyThreshold for the redundant predicates heuristic.
   */
  @Test
  public void checkStringRedundancyThreshold() throws Exception {
    Configuration pStringRedundancyThresholdConfig =
        TestUtils.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.progressBasedRefinementSelectionHeuristics.heuristicRefinerPairs",
                "REDUNDANT_PREDICATES:DEFAULT")
            .setOption(
                "cpa.predicate.progressBasedRefinementSelectionHeuristics.RedundantPredicates.redundancyThreshold",
                "xyz")
            .build();

    PredicateCPARefinerFactory factory = setUpRefinerFactory(pStringRedundancyThresholdConfig);

    ImmutableMap<ProgressBasedRefinementSelectionRefinerType, Refiner> refiners =
        setUpRefinerMap(factory);

    assertThrows(
        InvalidConfigurationException.class,
        () -> factory.createProgressBasedRefinementSelectionConfig(refiners));
  }

  // A dummy refiner to serve as Refiner instances the progressBasedRefinementSelection adds to its
  // map of
  // available refiners.
  private static class DummyRefiner implements Refiner {

    @Override
    public boolean performRefinement(ReachedSet pReached)
        throws CPAException, InterruptedException {
      return false;
    }
  }
}
