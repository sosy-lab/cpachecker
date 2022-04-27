// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "pcc.backwardtargets")
public class BackwardTargetsReachedSetStrategy extends SequentialReadStrategy
    implements StatisticsProvider {

  private final @Nullable AlgorithmWithPropertyCheck algorithm;
  private AbstractState[] backwardTargets;

  @Option(
      secure = true,
      description = "Enable to store ARG states instead of abstract states wrapped by ARG state")
  private boolean certificateStatesAsARGStates = false;

  public BackwardTargetsReachedSetStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    pConfig.inject(this);
    algorithm =
        pCpa == null
            ? null
            : new AlgorithmWithPropertyCheck(
                CPAAlgorithm.create(pCpa, pLogger, pConfig, pShutdownNotifier), pLogger, pCpa);
  }

  @Override
  public void constructInternalProofRepresentation(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    try {
      backwardTargets = detectBackwardTargets((ARGState) pReached.getFirstState(), pReached.size());
    } catch (ClassCastException e) {
      throw new InvalidConfigurationException("Require ARG states, as top level abstract states.");
    }
  }

  private AbstractState[] detectBackwardTargets(final ARGState rootNode, final int size) {
    // DFS to detect circles (backward targets)
    Collection<AbstractState> statesToStore = new HashSet<>();
    Map<ARGState, Pair<Integer, Integer>> exploreTimes = Maps.newHashMapWithExpectedSize(size);
    Deque<Pair<ARGState, Iterator<ARGState>>> toVisit = new ArrayDeque<>();

    int time = 0;
    exploreTimes.put(rootNode, Pair.of(time++, Integer.MAX_VALUE));
    toVisit.add(Pair.of(rootNode, rootNode.getChildren().iterator()));
    Pair<ARGState, Iterator<ARGState>> top;
    Pair<Integer, Integer> exploreTime;
    ARGState uncoveredChild;

    while (!toVisit.isEmpty()) {
      top = toVisit.peek();

      if (!top.getSecond().hasNext()) {
        exploreTimes.put(
            top.getFirst(), Pair.of(exploreTimes.get(top.getFirst()).getFirst(), time++));
        toVisit.pop();
        continue;
      }

      uncoveredChild = replaceByCoveringState(top.getSecond().next());
      if (exploreTimes.containsKey(uncoveredChild)) {
        exploreTime = exploreTimes.get(uncoveredChild);
        if (exploreTime.getFirst() < time && exploreTime.getSecond() > time) {
          if (certificateStatesAsARGStates) {
            statesToStore.add(uncoveredChild);
          } else {
            statesToStore.add(uncoveredChild.getWrappedState());
          }
        }
      } else {
        toVisit.push(Pair.of(uncoveredChild, uncoveredChild.getChildren().iterator()));
        exploreTimes.put(uncoveredChild, Pair.of(time++, Integer.MAX_VALUE));
      }
    }

    return statesToStore.toArray(new AbstractState[0]);
  }

  private ARGState replaceByCoveringState(final ARGState pState) {
    ARGState result = pState;
    while (result.isCovered()) {
      result = result.getCoveringState();
    }
    return result;
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    // get initial precision
    Precision initPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // add certificate states to reached set
    for (AbstractState backwardTarget : backwardTargets) {
      pReachedSet.add(backwardTarget, initPrec);
    }

    // recompute deleted part with CPAalgorithm and check property with property checking algorithm
    return algorithm.run(pReachedSet).isSound();
  }

  @Override
  protected Object getProofToWrite(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    constructInternalProofRepresentation(pReached, pCpa);
    return backwardTargets;
  }

  @Override
  protected void prepareForChecking(final Object pReadObject) throws InvalidConfigurationException {
    backwardTargets = (AbstractState[]) pReadObject;
    stats.proofSize = backwardTargets.length;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    super.collectStatistics(statsCollection);
    if (algorithm != null) {
      algorithm.collectStatistics(statsCollection);
    }
  }
}
