/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.threadmodular.refinement")
public class ThreadModularCPARefiner implements Refiner, StatisticsProvider {

  @Option(
    secure = true,
    description = "Instead of updating precision and arg we say that the refinement was not successful"
        + " after N times of refining. A real error state is not necessary to be found. Use 0 for"
        + " unlimited refinements (default).")
  @IntegerOption(min = 0)
  private int stopAfterNRefinements = 0;

  private final ARGBasedRefiner delegate;
  // statistics
  private final StatTimer totalTime = new StatTimer("Time for threadmodular refinement");
  private final StatTimer delegatingTime = new StatTimer("Time for delegate refiner");
  private final StatTimer modifingPathTime = new StatTimer("Time for modifing paths");

  private final LogManager logger;
  private final GlobalRefinementStrategy strategy;
  private final ARGCPA argCPA;

  public ThreadModularCPARefiner(
      LogManager pLogger,
      GlobalRefinementStrategy pStrategy,
      @NonNull ARGCPA pArgcpa,
      Configuration pConfig,
      ARGBasedRefiner pDelegate)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    logger = pLogger;
    strategy = pStrategy;
    argCPA = pArgcpa;
    delegate = pDelegate;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    totalTime.start();
    try {

      int iterationCounter = 0;
      List<AbstractState> targets =
          FluentIterable.from(pReached).filter(AbstractStates.IS_TARGET_STATE).toList();
      assert !targets.isEmpty();
      Collection<CFANode> previousNodes = Collections.emptySet();

      ARGReachedSet argReachedSet = new ARGReachedSet(pReached, argCPA);
      strategy.initializeGlobalRefinement();

      // Is it necessary to have more than one?
      ARGPath refinedPath = ARGUtils.getOnePathTo((ARGState) targets.get(0));
      assert refinedPath != null;

      while (refinedPath != null) {
        iterationCounter++;
        delegatingTime.start();
        CounterexampleInfo counterexample =
            delegate.performRefinementForPath(argReachedSet, refinedPath);
        delegatingTime.stop();

        // TODO fix handling of counterexamples
        // + 1 for update count as the current interval is not finished
        if (!counterexample.isSpurious()) {
          if (iterationCounter == 1) {
            // real ARG path
            strategy.resetGlobalRefinement();
            return false;
          } else {
            // need to rebuild abstraction with new predicates to obtain the potentially true path
            break;
          }
        } else if (stopAfterNRefinements == iterationCounter) {
          break;
        }

        List<CFANode> newBlock = new ArrayList<>();
        for (CFANode n : strategy.getAffectedNodes()) {
          if (!previousNodes.contains(n)) {
            newBlock.add(n);
          }
        }
        previousNodes = strategy.getAffectedNodes();

        modifingPathTime.start();
        refinedPath = modifyThePathWithEffects(refinedPath, newBlock);
        modifingPathTime.stop();
      }

      strategy.updatePrecisionAndARG();
      return true;

    } finally {
      totalTime.stop();
    }
  }

  private ARGPath
      modifyThePathWithEffects(ARGPath pRefinedPath, Collection<CFANode> pAffectedNodes) {
    List<ARGState> newStates = new ArrayList<>(pRefinedPath.asStatesList());
    List<CFANode> affectedNodes =
        from(newStates).filter(s -> s.getAppliedFrom() != null)
            .transform(s -> AbstractStates.extractLocation(s))
            .toList();
    Set<CFANode> nodesToAffect =
        from(pAffectedNodes).filter(n -> !affectedNodes.contains(n)).toSet();

    if (nodesToAffect.isEmpty()) {
      return null;
    }

    boolean changed = false;
    for (CFANode node : nodesToAffect) {
      changed |= insertEffectFor(node, newStates);
    }

    if (changed) {
      return new ARGPath(newStates);
    } else {
      return null;
    }
  }

  private boolean insertEffectFor(CFANode pNode, List<ARGState> pNewStates) {
    int i = 0;
    int initialSize = pNewStates.size();
    while (i < pNewStates.size()) {
      ARGState state = pNewStates.get(i);
      if (!state.isProjection() && pNode.equals(AbstractStates.extractLocation(state))) {
        if (!state.getAppliedTo().isEmpty()) {
          List<ARGState> toAdd = new ArrayList<>();
          Set<CFAEdge> effects = new HashSet<>();
          for (ARGState appliedState : state.getAppliedTo()) {
            // We add the state if the successor is covered by initial state, because it means the
            // abstractions are equal
            Collection<ARGState> children = appliedState.getChildren();
            for (ARGState child : children) {
              // Number of children does not matter ( assume edges)
              if (child.isCovered()) {
                List<CFAEdge> newEffects = state.getEdgesToChild(appliedState);
                if (!effects.containsAll(newEffects)) {
                  toAdd.add(appliedState);
                  effects.addAll(newEffects);
                }
                break;
              }
            }
          }
          if (!toAdd.isEmpty()) {
            // Some children are related to the same edges, we may skip them
            // Need to add the "covered" child, which is similar to the initial state
            for (ARGState add : toAdd) {
              i++;
              pNewStates.add(i, add);
            }
            i++;
            pNewStates.add(i, state);
          }
        }
      }
      i++;
    }
    return initialSize != pNewStates.size();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
    if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(pStatsCollection);
    }
  }

  private class Stats implements Statistics {

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);
      w0.put(totalTime).put(delegatingTime).put(modifingPathTime);
    }

    @Override
    public String getName() {
      return "ThreadModular Refiner";
    }
  }
}
