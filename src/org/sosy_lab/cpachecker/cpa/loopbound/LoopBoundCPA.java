// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.AbstractStates.projectToType;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
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
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.DeltaTrackingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.loopbound")
public class LoopBoundCPA extends AbstractCPA
    implements ReachedSetAdjustingCPA, StatisticsProvider, Statistics, LoopIterationBounding {

  @Option(secure = true, description = "enable stack-based tracking of loops")
  private boolean trackStack = false;

  @Option(
      secure = true,
      description =
          "Use a stop operator that will identify loop states who's depth is congruent regarding"
              + " the modulus of this number. Values smaller or equal to zero will deactivate this"
              + " feature.")
  private int cyclicStopModulus = -1;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopBoundCPA.class);
  }

  private final LoopStructure loopStructure;

  private final LoopBoundPrecisionAdjustment precisionAdjustment;

  private final StatTimer adjustReachedSetTimer = new StatTimer("Time for adjusting reached set");
  private int adjustReachedSetCalls = 0;
  private long adjustReachedSetCandidates = 0;

  LoopBoundCPA(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException, CPAException {
    super("sep", "sep", new LoopBoundTransferRelation(pConfig, pCFA));
    pConfig.inject(this);
    loopStructure = pCFA.getLoopStructure().orElseThrow();
    precisionAdjustment = new LoopBoundPrecisionAdjustment(pConfig, pLogger);
  }

  @Override
  public StopOperator getStopOperator() {
    if (cyclicStopModulus <= 0) {
      return super.getStopOperator();
    } else {
      return new ModularStopOperator(cyclicStopModulus);
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    LoopBoundState initialState = new LoopBoundState();
    for (Loop loop : loopStructure.getLoopsForLoopHead(pNode)) {
      initialState = initialState.visitLoopHead(loop);
    }
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new LoopBoundPrecision(
        trackStack,
        precisionAdjustment.getMaxLoopIterations(),
        precisionAdjustment.getLoopIterationsBeforeAbstraction());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public boolean adjustPrecision() {
    return precisionAdjustment.nextState();
  }

  @Override
  public void adjustReachedSet(final ReachedSet pReachedSet) {
    adjustReachedSetTimer.start();
    SequencedSet<AbstractState> toRemove = new LinkedHashSet<>();
    try {
      // When the reached set tracks its own increments, only states added since the last
      // adjustment can have become removal candidates, so the full scan can be avoided.
      Iterable<AbstractState> candidates =
          pReachedSet instanceof DeltaTrackingReachedSet delta
          ? delta.drainAddedSinceMark()
          : pReachedSet;

      adjustReachedSetCalls++;
      for (AbstractState s : candidates) {
        adjustReachedSetCandidates++;
        LoopBoundState loopBoundState = extractStateByType(s, LoopBoundState.class);
        if (loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance()) {
          toRemove.add(s);
        }
      }
    } finally {
      adjustReachedSetTimer.stop();
    }

    // Never delete the first state
    if (toRemove.contains(pReachedSet.getFirstState())) {
      pReachedSet.clear();
      return;
    }

    SequencedSet<AbstractState> waitlist = new LinkedHashSet<>();
    for (ARGState s : projectToType(toRemove, ARGState.class)) {
      waitlist.addAll(s.getParents());
    }

    // Add the new waitlist
    waitlist.forEach(pReachedSet::reAddToWaitlist);

    pReachedSet.removeAll(toRemove);
    for (ARGState s : projectToType(toRemove, ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put("Bound k", precisionAdjustment.getMaxLoopIterations());
    int maximumLoopIterationReached = 0;
    for (LoopBoundState state : projectToType(pReached, LoopBoundState.class)) {
      maximumLoopIterationReached =
          Math.max(maximumLoopIterationReached, state.getDeepestIteration());
    }
    writer.put("Maximum loop iteration reached", maximumLoopIterationReached);
    writer.spacer();
    writer.put("Calls to adjustReachedSet", adjustReachedSetCalls);
    writer.put("States scanned in adjustReachedSet", adjustReachedSetCandidates);
    writer.put(adjustReachedSetTimer);
    writer.spacer();
  }

  @Override
  public String getName() {
    return "Bounds CPA";
  }

  @Override
  public void setMaxLoopIterations(int pMaxLoopIterations) {
    precisionAdjustment.setMaxLoopIterations(pMaxLoopIterations);
  }

  @Override
  public int getMaxLoopIterations() {
    return precisionAdjustment.getMaxLoopIterations();
  }

  public void incrementLoopIterationsBeforeAbstraction() {
    precisionAdjustment.incrementLoopIterationsBeforeAbstraction();
  }
}
