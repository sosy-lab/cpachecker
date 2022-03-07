// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CoveringStateSetProvider;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements the <i>stop-join</i> operator for {@link CompositeCPA}.
 *
 * <p>When checking whether to stop, i.e. whether the given state is covered by the reached set, the
 * {@link CompositeStopJoinOperator} first uses the component CPAs with <i>stop-sep</i> operators as
 * a filter to get a subset of the reached set, in which any state can cover the given state
 * individually; then the component CPAs with <i>stop-join</i> operators determine if the states in
 * this subset of the reached set jointly cover the given state.
 */
class CompositeStopJoinOperator extends CompositeStopOperator {

  CompositeStopJoinOperator(ImmutableList<StopOperator> stopOperators) {
    super(stopOperators);
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet = getStopSepCoveringStates(element, reached, precision);
    return !getStopJoinCoveringStates(element, reachedSubSet, precision).isEmpty();
  }

  @Override
  public Collection<AbstractState> getCoveringStates(
      AbstractState pElement, Collection<AbstractState> pReachedSet, Precision pPrecision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet =
        getStopSepCoveringStates(pElement, pReachedSet, pPrecision);
    return ImmutableSet.copyOf(getStopJoinCoveringStates(pElement, reachedSubSet, pPrecision));
  }

  private Collection<AbstractState> getStopSepCoveringStates(
      AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet = new LinkedHashSet<>();
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    checkArgument(
        compositeState.getWrappedStates().size() == stopOperators.size(),
        "State with wrong number of component states given");

    for (AbstractState e : reached) {
      if (stopSep(compositeState, (CompositeState) e, compositePrecision)) {
        reachedSubSet.add(e);
      }
    }
    return reachedSubSet;
  }

  private boolean stopSep(
      CompositeState compositeState,
      CompositeState compositeReachedState,
      CompositePrecision compositePrecision)
      throws CPAException, InterruptedException {
    List<AbstractState> compositeElements = compositeState.getWrappedStates();
    List<AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();
    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);
      // TODO: is there a better way to distinguish StopOperator types?
      // if the stop operator does not implement the interface `StopJoinOperator`,
      // it is treated as a stop-sep operator
      if (stopOp instanceof StopJoinOperator) { // skip stop-join operators
        continue;
      }

      AbstractState absElem1 = compositeElements.get(idx);
      AbstractState absElem2 = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)) {
        return false;
      }
    }
    return true;
  }

  /** Checks whether all the stop-join operators implement {@link CoveringStateSetProvider} */
  private boolean retrieveCoveringStatesPossible() {
    for (StopOperator op : stopOperators) {
      if ((op instanceof StopJoinOperator) && !(op instanceof CoveringStateSetProvider)) {
        return false;
      }
    }
    return true;
  }

  private Collection<AbstractState> getStopJoinCoveringStates(
      AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    if (reached.isEmpty()) {
      return ImmutableSet.of();
    }

    boolean retrievalPossible = retrieveCoveringStatesPossible();
    Set<AbstractState> reachedSubSet =
        retrievalPossible ? new LinkedHashSet<>(reached.size()) : new LinkedHashSet<>(reached);
    List<AbstractState> compositeElements = ((CompositeState) element).getWrappedStates();
    List<Precision> compositePrecisions = ((CompositePrecision) precision).getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);
      if (!(stopOp instanceof StopJoinOperator)) {
        continue;
      }

      // Mapping of composite state to component state
      Multimap<AbstractState, AbstractState> stateMap = ArrayListMultimap.create(reached.size(), 1);
      List<AbstractState> absElems = new ArrayList<>();
      for (AbstractState v : reached) {
        AbstractState k = ((CompositeState) v).get(idx);
        absElems.add(k);
        stateMap.put(k, v);
      }

      AbstractState absElem = compositeElements.get(idx);
      Precision prec = compositePrecisions.get(idx);

      // if retrieval of covering set if possible, we can potentially collect a minimal covering set
      // (depends on whether the component CPA implements some minimization heuristics); otherwise,
      // we simply do a normal stop-join check
      if (retrievalPossible) {
        assert stopOp instanceof CoveringStateSetProvider;
        Collection<AbstractState> currentCoveringStates =
            ((CoveringStateSetProvider) stopOp).getCoveringStates(absElem, absElems, prec);
        if (currentCoveringStates.isEmpty()) {
          return ImmutableSet.of();
        }
        // Map the component state back to the composite state and add it to the accumulated subset
        for (AbstractState k : currentCoveringStates) {
          reachedSubSet.addAll(stateMap.get(k));
        }
      } else if (!stopOp.stop(absElem, absElems, prec)) { // !retrievalPossible
        return ImmutableSet.of();
      }
    }
    return reachedSubSet;
  }
}
