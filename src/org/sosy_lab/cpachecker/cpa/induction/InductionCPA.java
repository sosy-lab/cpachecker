// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionState.Resets;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Decides at which arrival at a loop head the predicate analysis abstracts, and whether an
 * abstraction starts an induction step, following a precision per loop. A refiner can therefore
 * simulate different algorithms (BMC, k-induction, IMC, ISMC, PDR, predicate analysis) by the
 * precision it chooses.
 */
@Options(prefix = "cpa.induction")
public class InductionCPA extends AbstractCPA implements StatisticsProvider, Statistics {

  @Option(
      secure = true,
      description =
          "initial number of arrivals at a head of a loop after which the next arrival abstracts")
  private int k = 0;

  @Option(
      secure = true,
      description =
          "initially start an induction step at every abstraction of a loop in the base case")
  private boolean induction = false;

  @Option(
      secure = true,
      description =
          "initial number of abstractions of a loop head that the predicate precision describes,"
              + " -1 for all. A path ends without proof at the first abstraction that is not"
              + " described.")
  private int describedAbstractions = LoopPrecision.ALL_ABSTRACTIONS;

  @Option(
      secure = true,
      description = "initially forbid that abstractions of the same loop cover each other")
  private boolean unrolling = false;

  @Option(
      secure = true,
      description =
          "let the base case continue one iteration after an induction start, as in the paper of"
              + " the PredK framework. Native k-induction does not continue the base case.")
  private boolean continueBaseCase = true;

  @Option(
      secure = true,
      description =
          "explore nothing if no target location of the specification is reachable, as native"
              + " BMC, k-induction, IMC and ISMC do")
  private boolean skipWithoutTargetLocations = false;

  @Option(
      secure = true,
      description =
          "CPAs (as in CompositeCPA.cpas) that restart from their initial state at every"
              + " abstraction at a loop head")
  private List<String> abstractedCPAs = ImmutableList.of();

  @Option(
      secure = true,
      description =
          "CPAs (as in CompositeCPA.cpas) that also restart from their initial state at every"
              + " abstraction in an induction step, e.g., the callstack for an arbitrary calling"
              + " context")
  private List<String> inductionStepCPAs = ImmutableList.of();

  private final LoopStructure loopStructure;
  private final Resets resets;
  private final InductionTransferRelation transferRelation;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(InductionCPA.class);
  }

  private InductionCPA(
      Configuration pConfig,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super("sep", "sep", new InductionDomain(), null);
    pConfig.inject(this);
    loopStructure = loopStructureOf(pCfa);
    boolean exploresNothing =
        skipWithoutTargetLocations
            && new TargetLocationProviderImpl(pShutdownNotifier, pLogger, pCfa)
                .tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpecification)
                .isEmpty();
    transferRelation =
        new InductionTransferRelation(loopStructure, continueBaseCase, exploresNothing);
    resets = new Resets(toClassNames(abstractedCPAs), toClassNames(inductionStepCPAs));
  }

  @Override
  public InductionTransferRelation getTransferRelation() {
    return transferRelation;
  }

  private static LoopStructure loopStructureOf(CFA pCfa) throws InvalidConfigurationException {
    return pCfa.getLoopStructure()
        .orElseThrow(
            () ->
                new InvalidConfigurationException(
                    "InductionCPA needs the loop structure of the CFA"));
  }

  private static ImmutableSet<String> toClassNames(List<String> pCpas) {
    return pCpas.stream()
        .map(name -> name.startsWith("org.sosy_lab.") ? name : "org.sosy_lab.cpachecker." + name)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return InductionState.initial(resets);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return InductionPrecision.uniform(
        loopStructure.getAllLoops(),
        new LoopPrecision(k, induction, describedAbstractions, unrolling));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter.writingStatisticsTo(pOut)
        .put(transferRelation.abstractions)
        .put(transferRelation.inductionStarts)
        .put(transferRelation.earlyInductionSteps)
        .put(transferRelation.cutOffs)
        .put(transferRelation.suppressedTargets);
  }

  @Override
  public @Nullable String getName() {
    return "InductionCPA";
  }
}
