// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRedundantPredicates;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRunDefaultNTimes;
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
  private ARGBasedRefiner staticRefiner;
  private ARGBasedRefiner defaultRefiner;

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
    return new PredicateCPARefinerFactory(
        new PredicateCPA(pConfig, logger, blk, cfa, shutdownNotifier, spec, reachedSet));
  }

  // Creates a default map of available refiners for the DelegatingRefiner
  private ImmutableMap<DelegatingRefinerRefinerType, ARGBasedRefiner> setUpRefinerMap(
      PredicateCPARefinerFactory pRefinerFactory) {
    return pRefinerFactory.buildRefinerMap(defaultRefiner, staticRefiner);
  }

  /** This test checks the buildRefinerMap method's functionality for the DelegatingRefiner. */
  @Test
  public void setUpAvailableRefinerMap() throws Exception {
    Configuration pConfig = TestDataTools.configurationForTest().build();
    PredicateCPARefinerFactory pPredicateCPARefinerFactory = setUpRefinerFactory(pConfig);
    ImmutableMap<DelegatingRefinerRefinerType, ARGBasedRefiner> pAvailableRefiners =
        setUpRefinerMap(pPredicateCPARefinerFactory);
    assertEquals(2, pAvailableRefiners.size());
    assertSame(defaultRefiner, pAvailableRefiners.get(DelegatingRefinerRefinerType.DEFAULT));
    assertSame(staticRefiner, pAvailableRefiners.get(DelegatingRefinerRefinerType.STATIC));
  }

  /**
   * This test checks if DelegatingRefiner parses the command-line input for the StaticRefinement
   * heuristic-refiner pair correctly.
   */
  @Test
  public void setUpStaticRefiner() throws Exception {
    Configuration pStaticConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "STATIC:STATIC")
            .build();
    PredicateCPARefinerFactory pStaticRefinerFactory = setUpRefinerFactory(pStaticConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pStaticRefinerFactory.createDelegatingRefinerConfig(setUpRefinerMap(pStaticRefinerFactory));

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertEquals(1, pRefinerRecords.size());
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
  }

  /**
   * This test checks if DelegatingRefiner sets up the default refiner with the default value for
   * the number of runs to collect data for other heuristics correctly.
   */
  @Test
  public void setUpDefaultRefinement() throws Exception {
    Configuration pDefaultConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "DEFAULT_N_TIMES:DEFAULT")
            .build();
    PredicateCPARefinerFactory pDefaultRefinerFactory = setUpRefinerFactory(pDefaultConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pDefaultRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pDefaultRefinerFactory));

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicRunDefaultNTimes);
    assertEquals(defaultRefiner, pRefinerRecords.getFirst().pRefiner());
    assertEquals(
        10,
        ((DelegatingRefinerHeuristicRunDefaultNTimes) pRefinerRecords.getFirst().pHeuristic())
            .getFixedRuns());
  }

  /**
   * This test checks if DelegatingRefiner parses the command-line input for a custom number of runs
   * for the run default refiner n-times heuristic correctly.
   */
  @Test
  public void setUpDefaultRefinementIndividualRuns() throws Exception {
    Configuration pDefaultIndividualRunsConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "DEFAULT_N_TIMES:DEFAULT")
            .setOption("cpa.predicate.refinement.pDefaultFixedRuns", "5")
            .build();
    PredicateCPARefinerFactory pDefaultIndividualRunsRefinerFactory =
        setUpRefinerFactory(pDefaultIndividualRunsConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pDefaultIndividualRunsRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pDefaultIndividualRunsRefinerFactory));

    assertEquals(
        5,
        ((DelegatingRefinerHeuristicRunDefaultNTimes) pRefinerRecords.getFirst().pHeuristic())
            .getFixedRuns());
  }

  /**
   * This test checks if DelegatingRefiner sets up the default refiner with the predicate redundancy
   * heuristic and its default threshold value correctly.
   */
  @Test
  public void setUpRedundantHeuristic() throws Exception {
    Configuration pRedundantConfig =
        TestDataTools.configurationForTest()
            .setOption(
                "cpa.predicate.refinement.heuristicRefinerPairs", "REDUNDANT_PREDICATES:DEFAULT")
            .build();
    PredicateCPARefinerFactory pRedundantRefinerFactory = setUpRefinerFactory(pRedundantConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pRedundantRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pRedundantRefinerFactory));

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicRedundantPredicates);
    assertEquals(defaultRefiner, pRefinerRecords.getFirst().pRefiner());
    assert (((DelegatingRefinerHeuristicRedundantPredicates)
                pRefinerRecords.getFirst().pHeuristic())
            .getRedundancyThreshold()
        == 0.8);
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
            .setOption("cpa.predicate.refinement.pAcceptableRedundancyThreshold", "0.1")
            .build();
    PredicateCPARefinerFactory pRedundantCustomThresholdRefinerFactory =
        setUpRefinerFactory(pRedundantCustomThresholdConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pRedundantCustomThresholdRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pRedundantCustomThresholdRefinerFactory));

    assert (((DelegatingRefinerHeuristicRedundantPredicates)
                pRefinerRecords.getFirst().pHeuristic())
            .getRedundancyThreshold()
        == 0.1);
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

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicRedundantPredicates);
    assertTrue(
        pRefinerRecords.getLast().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertEquals(2, pRefinerRecords.size());
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
    assertEquals(staticRefiner, pRefinerRecords.getLast().pRefiner());
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

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertTrue(
        pRefinerRecords.getLast().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
    assertEquals(defaultRefiner, pRefinerRecords.getLast().pRefiner());
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

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertEquals(1, pRefinerRecords.size());
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
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
                "STATIC:STATIC, DEFAULT_N_TIMES:DEFAULT ,REDUNDANT_PREDICATES:DEFAULT")
            .build();
    PredicateCPARefinerFactory pIgnoreWhiteSpaceCommaConfigRefinerFactory =
        setUpRefinerFactory(pIgnoreWhiteSpaceCommaConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pIgnoreWhiteSpaceCommaConfigRefinerFactory.createDelegatingRefinerConfig(
            setUpRefinerMap(pIgnoreWhiteSpaceCommaConfigRefinerFactory));

    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertTrue(
        pRefinerRecords.get(1).pHeuristic() instanceof DelegatingRefinerHeuristicRunDefaultNTimes);
    assertTrue(
        pRefinerRecords.get(2).pHeuristic()
            instanceof DelegatingRefinerHeuristicRedundantPredicates);
    assertEquals(3, pRefinerRecords.size());
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
    assertEquals(defaultRefiner, pRefinerRecords.get(1).pRefiner());
    assertEquals(defaultRefiner, pRefinerRecords.get(2).pRefiner());
  }

  /**
   * This test checks if DelegatingRefiner uses its default configuration for an empty command-line
   * input for predicate-heuristic pairs.
   */
  @Test
  public void checkEmptyHeuristicRefinerInput() throws Exception {
    Configuration pEmptyInputConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", " ")
            .build();

    PredicateCPARefinerFactory pEmptyInputFactory = setUpRefinerFactory(pEmptyInputConfig);

    ImmutableList<HeuristicDelegatingRefinerRecord> pRefinerRecords =
        pEmptyInputFactory.createDelegatingRefinerConfig(setUpRefinerMap(pEmptyInputFactory));

    assertEquals(3, pRefinerRecords.size());
    assertTrue(
        pRefinerRecords.getFirst().pHeuristic()
            instanceof DelegatingRefinerHeuristicStaticRefinement);
    assertTrue(
        pRefinerRecords.get(1).pHeuristic() instanceof DelegatingRefinerHeuristicRunDefaultNTimes);
    assertTrue(
        pRefinerRecords.get(2).pHeuristic()
            instanceof DelegatingRefinerHeuristicRedundantPredicates);
    assertEquals(staticRefiner, pRefinerRecords.getFirst().pRefiner());
    assertEquals(defaultRefiner, pRefinerRecords.get(1).pRefiner());
    assertEquals(defaultRefiner, pRefinerRecords.get(2).pRefiner());
  }

  /**
   * This test checks that the run default refiner n-times heuristic runs exactly the number of
   * times in pDefaultFixedRuns.
   */
  @Test
  public void checkDefaultNTimesRunNumbers() {
    DelegatingRefinerHeuristicRunDefaultNTimes runDefaultNTimes =
        new DelegatingRefinerHeuristicRunDefaultNTimes(3);
    assertTrue(runDefaultNTimes.fulfilled(null, ImmutableList.of()));
    assertTrue(runDefaultNTimes.fulfilled(null, ImmutableList.of()));
    assertTrue(runDefaultNTimes.fulfilled(null, ImmutableList.of()));
    assertFalse(runDefaultNTimes.fulfilled(null, ImmutableList.of()));
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
                "STATIC:STATIC;DEFAULT_N_TIMES:DEFAULT")
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
   * This test checks if DelegatingRefiner throws an exception for a negative number of fixedRuns
   * for the run default refiner n-times heuristic in the command-line input.
   */
  @Test
  public void checkNegativeFixedRuns() throws Exception {
    Configuration pNegativeFixedRunsConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "DEFAULT_N_TIMES:DEFAULT")
            .setOption("cpa.predicate.refinement.pDefaultFixedRuns", "-5")
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
   * This test checks if DelegatingRefiner throws an exception for non integer command-line input
   * for fixedRuns for the run default refiner n-times heuristic.
   */
  @Test
  public void checkDoubleFixedRuns() throws Exception {
    Configuration pDoubleFixedRunsConfig =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.refinement.heuristicRefinerPairs", "DEFAULT_N_TIMES:DEFAULT")
            .setOption("cpa.predicate.refinement.pDefaultFixedRuns", "5.0")
            .build();
    assertThrows(
        InvalidConfigurationException.class, () -> setUpRefinerFactory(pDoubleFixedRunsConfig));
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
            .setOption("cpa.predicate.refinement.pAcceptableRedundancyThreshold", "-0.1")
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
            .setOption("cpa.predicate.refinement.pAcceptableRedundancyThreshold", "2.0")
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
            .setOption("cpa.predicate.refinement.pAcceptableRedundancyThreshold", "xyz")
            .build();

    assertThrows(
        InvalidConfigurationException.class,
        () -> setUpRefinerFactory(pStringRedundancyThresholdConfig));
  }

  /**
   * This test checks if DelegatingRefiner throws an exception when the refinerMap is empty and no
   * default is available.
   */
  @Test
  public void checkEmptyAvailableRefiners() throws Exception {
    Configuration pEmptyConfig = TestDataTools.configurationForTest().build();

    PredicateCPARefinerFactory pEmptyFactory = setUpRefinerFactory(pEmptyConfig);

    ImmutableMap<DelegatingRefinerRefinerType, ARGBasedRefiner> pEmptyMap = ImmutableMap.of();

    assertThrows(
        InvalidConfigurationException.class,
        () -> pEmptyFactory.createDelegatingRefinerConfig(pEmptyMap));
  }

  // A dummy refiner to serve as ARGBasedRefiner instances the DelegatingRefiner adds to its map of
  // available refiners.
  private static class DummyRefiner implements ARGBasedRefiner {

    @Override
    public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
        throws CPAException, InterruptedException {
      return CounterexampleInfo.spurious();
    }
  }
}
