/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

@Options(prefix = "pcc.backwardtargets")
public class BackwardTargetsReachedSetStrategy extends SequentialReadStrategy implements StatisticsProvider {

  private final @Nullable AlgorithmWithPropertyCheck algorithm;
  private AbstractState[] backwardTargets;

  @Option(secure = true, description = "Enable to store ARG states instead of abstract states wrapped by ARG state")
  private boolean certificateStatesAsARGStates = false;

  public BackwardTargetsReachedSetStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);
    algorithm =
        pCpa == null
            ? null
            : new AlgorithmWithPropertyCheck(
                CPAAlgorithm.create(pCpa, pLogger, pConfig, pShutdownNotifier), pLogger, pCpa);
  }

  @Override
  public void constructInternalProofRepresentation(final UnmodifiableReachedSet pReached)
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
        exploreTimes.put(top.getFirst(), Pair.of(exploreTimes.get(top.getFirst()).getFirst(), time++));
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

    return statesToStore.toArray(new AbstractState[statesToStore.size()]);
  }

  private ARGState replaceByCoveringState(final ARGState pState) {
    ARGState result = pState;
    while (result.isCovered()) {
      result = result.getCoveringState();
    }
    return result;
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
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
  protected Object getProofToWrite(final UnmodifiableReachedSet pReached) throws InvalidConfigurationException {
    constructInternalProofRepresentation(pReached);
    return backwardTargets;
  }

  @Override
  protected void prepareForChecking(final Object pReadObject) throws InvalidConfigurationException {
    backwardTargets = (AbstractState[]) pReadObject;
    this.stats.proofSize = backwardTargets.length;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    super.collectStatistics(statsCollection);
    if (algorithm != null) {
      algorithm.collectStatistics(statsCollection);
    }
  }

}
