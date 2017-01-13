/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

/** View on the set of states computed by {@link BAMSubgraphComputer}. */
final class BAMReachedSetView implements BAMUnmodifiableReachedSet {

  private final ARGState rootOfSubgraph;
  private final ARGState lastState;
  private final Function<AbstractState, Precision> precisionGetter;
  private final Collection<AbstractState> subgraph;
  private final BAMDataManager dataManager;
  private final Map<Pair<ReachedSet, Class<? extends Precision>>, Precision> collectedPrecisionCache = new HashMap<>();
  private final Set<AbstractState> tmpCache = new HashSet<>();

  BAMReachedSetView(ARGState pRootOfSubgraph, ARGState pLastState,
      Function<AbstractState, Precision> pPrecisionGetter, BAMDataManager data) {
    rootOfSubgraph = pRootOfSubgraph;
    lastState = pLastState;
    precisionGetter = pPrecisionGetter;
    subgraph = Collections.unmodifiableCollection(pRootOfSubgraph.getSubgraph());
    dataManager = data;
  }


  @Override
  public Collection<AbstractState> asCollection() {
    return subgraph;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return subgraph.iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections2.transform(subgraph, precisionGetter);
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state) {
    throw new UnsupportedOperationException("should not be needed");
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    throw new UnsupportedOperationException("should not be needed");
  }

  @Override
  public AbstractState getFirstState() {
    return rootOfSubgraph;
  }

  @Override
  public AbstractState getLastState() {
    return lastState;
  }

  @Override
  public boolean hasWaitingState() {
    // BAM-reached-set has no waiting states
    return false;
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    // BAM-reached-set has no waiting states
    return Collections.emptySet();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    return precisionGetter.apply(state);
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    subgraph.forEach(state -> pAction.accept(state, precisionGetter.apply(state)));
  }

  @Override
  public boolean contains(AbstractState state) {
    return subgraph.contains(state);
  }

  @Override
  public boolean isEmpty() {
    return subgraph.isEmpty();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException("should not be needed");
  }

  @Override
  public Precision getPrecisionForSubgraph(AbstractState state,
      BiFunction<Precision, Precision, Precision> joiner, Function<Precision, Precision> extractor) {

   //TODO do we need the option? Seems, it may be implemented in the caller
   /*if (!collectPreisionFromAllSubgraph) {
      return getPrecision(state);*/

    //TODO cache helps!
    /*if (stateToCollectedPrecision.containsKey(state)) {
      return stateToCollectedPrecision.get(state);*/

    assert state instanceof BackwardARGState;
    //There may be returning
    Set<ARGState> foundStates = new HashSet<>();
    Queue<BackwardARGState> worklist = new LinkedList<>();
    Set<BackwardARGState> targetStates = new TreeSet<>();
    targetStates.add((BackwardARGState) state);
    BackwardARGState currentBARGstate = (BackwardARGState)state;
    ARGState currentState = currentBARGstate.getARGState();
    foundStates.add(currentState);
    worklist.add((BackwardARGState) state);

    if (currentState.getParents().isEmpty()) {
      //the initial state
      return getPrecision(state);
    }

    while (!worklist.isEmpty()) {
      currentBARGstate = worklist.poll();

      for (ARGState child : currentBARGstate.getChildren()) {
        if (!foundStates.contains(child)) {
          foundStates.add(child);
          worklist.add((BackwardARGState)child);
        }
        currentState = currentBARGstate.getARGState();
        ARGState argChild = ((BackwardARGState)child).getARGState();
        if (!currentState.getChildren().contains(argChild)) {
          //entry or exit node of a block
          if (AbstractStates.extractLocation(currentState) instanceof CFunctionEntryNode) {
            continue;
          }
          if (!from(argChild.getParents()).filter(foundStates::contains).isEmpty()) {
            targetStates.add((BackwardARGState)child);
          }
        }
      }
    }
    Precision resultPrecision = null;

    for (BackwardARGState targetState : targetStates) {
      ARGState argState = (targetState).getARGState();

      ReachedSet currentReachedSet = getReachedSet(targetState);
      if (currentReachedSet != null) {
        //The main reached set can not be extracted.
        Precision targetPrecision = currentReachedSet.getPrecision(argState);
        //TODO Cache
        /*if (stateToCollectedPrecision.containsKey(targetState)) {
          resultPrecision = stateToCollectedPrecision.get(targetState);
        } else {*/
        resultPrecision = collectPrecision(currentReachedSet, argState, targetPrecision, new HashSet<>(),
            joiner, extractor);
        //}
        //stateToCollectedPrecision.put(targetState, resultPrecision);
      }
    }
    if (resultPrecision != null) {
      //stateToCollectedPrecision.put((BackwardARGState) state, replacedPrecision);
      return resultPrecision;
    } else {
      return getPrecision(state);
    }
  }

  private Precision collectPrecision(ReachedSet reached, AbstractState state, Precision pTargetPrecision,
        Collection<ReachedSet> handledSets, BiFunction<Precision, Precision, Precision> joiner,
        Function<Precision, Precision> extractor) {

      Queue<ARGState> worklist = new LinkedList<>();
      Set<ARGState> handledStates = new HashSet<>();
      ARGState currentState;
      Precision currentPrecision, resultPrecision = extractor.apply(pTargetPrecision);
      Class<? extends Precision> resultClass = resultPrecision.getClass();

      worklist.add((ARGState) state);
      handledStates.add((ARGState) state);

      while (!worklist.isEmpty()) {
        currentState = worklist.poll();
        if (currentState.isCovered() || currentState.isDestroyed()) {
          continue;
        }

        currentPrecision = reached.getPrecision(currentState);
        resultPrecision = joiner.apply(resultPrecision, extractor.apply(currentPrecision));
        from(currentState.getChildren())
          .filter(Predicates.not(handledStates::contains))
          .forEach(s -> { handledStates.add(s);
                          worklist.add(s);});

        if (dataManager.hasInitialState(currentState)) {
          ReachedSet other = dataManager.getReachedSetForInitialState(currentState);
          if (!handledSets.contains(other)) {
            Pair<ReachedSet, Class<? extends Precision>> hashKey = Pair.of(other, resultClass);
            if (collectedPrecisionCache.containsKey(hashKey)) {
              resultPrecision = collectedPrecisionCache.get(hashKey);
            } else {
              AbstractState reducedState = other.getFirstState();
              handledSets.add(other);
              currentPrecision = collectPrecision(other, reducedState, pTargetPrecision, handledSets, joiner, extractor);
              resultPrecision = joiner.apply(resultPrecision, extractor.apply(currentPrecision));
              collectedPrecisionCache.put(hashKey, resultPrecision);
            }
          }
        }
      }
      return resultPrecision;
    }

  private ReachedSet getReachedSet(BackwardARGState state) {
    BackwardARGState currentState = state;
    ARGState currentARG;
    ARGState targetState = state.getARGState();
    Queue<ARGState> worklist = new LinkedList<>();
    Set<ARGState> handledStates = new HashSet<>();
    worklist.add(currentState);
    handledStates.add(currentState);

    //TODO is it faster to iterate over all reachedSets?
    while (!worklist.isEmpty()) {
      currentState = (BackwardARGState) worklist.poll();
      currentARG = currentState.getARGState();
      if (dataManager.hasInitialState(currentARG)) {
        ReachedSet rSet = dataManager.getReachedSetForInitialState(currentARG);
        if (rSet.contains(targetState)) {
          return rSet;
        }
      }
      from(currentState.getParents())
        .filter(Predicates.not(handledStates::contains))
        .forEach(s -> { handledStates.add(s);
                        worklist.add(s);});
    }
    return null;
  }
}