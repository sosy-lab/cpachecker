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
import java.util.List;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CoveringStateSetProvider;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements the stop operator for {@link CompositeCPA}.
 *
 * <p>When checking whether to stop, i.e. whether the given state is covered by the reached set, the
 * {@link CompositeStopOperator} first uses the component CPAs with <i>stop-sep</i> operators as a
 * filter to get a subset of the reached set, in which any state can cover the given state
 * individually; then the component CPAs with <i>stop-join</i> operators determine if the states in
 * this subset of the reached set jointly cover the given state.
 */
class CompositeStopOperator implements ForcedCoveringStopOperator, CoveringStateSetProvider {
  final ImmutableList<StopOperator> stopOperators;

  CompositeStopOperator(ImmutableList<StopOperator> stopOperators) {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    return !getCoveringStates(element, reached, precision).isEmpty();
  }

  @Override
  public Collection<AbstractState> getCoveringStates(
      AbstractState pElement, Collection<AbstractState> pReachedSet, Precision pPrecision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet =
        getStopSepCoveringStates(pElement, pReachedSet, pPrecision);
    return getStopJoinCoveringStates(pElement, reachedSubSet, pPrecision);
  }

  private Collection<AbstractState> getStopSepCoveringStates(
      AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    ImmutableSet.Builder<AbstractState> reachedSubSet = ImmutableSet.builder();
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    checkArgument(
        compositeState.getWrappedStates().size() == stopOperators.size(),
        "State with wrong number of component states given");

    boolean noStopJoin = !containStopJoinOperator();
    for (AbstractState e : reached) {
      if (stopSep(compositeState, (CompositeState) e, compositePrecision)) {
        reachedSubSet.add(e);
        if (noStopJoin) {
          return reachedSubSet.build();
        }
      }
    }
    return reachedSubSet.build();
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
      if (!stopOp.stop(absElem1, ImmutableSet.of(absElem2), prec)) {
        return false;
      }
    }
    return true;
  }

  /** Check if at least one of the component CPA uses the stop-join operator */
  private boolean containStopJoinOperator() {
    for (StopOperator op : stopOperators) {
      if (op instanceof StopJoinOperator) {
        return true;
      }
    }
    return false;
  }

  /** Checks whether all the stop-join operators implement {@link CoveringStateSetProvider} */
  private boolean retrieveCoveringStatesPossible() {
    assert containStopJoinOperator();
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
    if (!containStopJoinOperator()) {
      return ImmutableSet.copyOf(reached);
    }

    boolean retrievalPossible = retrieveCoveringStatesPossible();
    ImmutableSet.Builder<AbstractState> reachedSubSet =
        ImmutableSet.builderWithExpectedSize(reached.size());
    // we will basically return the whole reached set, if we cannot retrieve covering subset,
    // and if every the stop-join check goes through
    if (!retrievalPossible) {
      reachedSubSet.addAll(reached);
    }
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

      // if retrieval of covering set is possible, we can potentially collect a minimal covering set
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
    return reachedSubSet.build();
  }

  boolean isCoveredBy(
      AbstractState pElement, AbstractState pOtherElement, List<ConfigurableProgramAnalysis> cpas)
      throws CPAException, InterruptedException {
    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeOtherElement = (CompositeState) pOtherElement;

    List<AbstractState> componentElements = compositeState.getWrappedStates();
    List<AbstractState> componentOtherElements = compositeOtherElement.getWrappedStates();

    if (componentElements.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < componentElements.size(); idx++) {
      ProofChecker componentProofChecker = (ProofChecker) cpas.get(idx);

      AbstractState absElem1 = componentElements.get(idx);
      AbstractState absElem2 = componentOtherElements.get(idx);

      if (!componentProofChecker.isCoveredBy(absElem1, absElem2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement, AbstractState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeReachedState = (CompositeState) pReachedState;
    CompositePrecision compositePrecision = (CompositePrecision) pPrecision;

    List<AbstractState> compositeElements = compositeState.getWrappedStates();
    List<AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();
    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState wrappedState = compositeElements.get(idx);
      AbstractState wrappedReachedState = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      boolean possible;
      if (stopOp instanceof ForcedCoveringStopOperator) {

        possible =
            ((ForcedCoveringStopOperator) stopOp)
                .isForcedCoveringPossible(wrappedState, wrappedReachedState, prec);

      } else {
        possible = stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), prec);
      }

      if (!possible) {
        return false;
      }
    }

    return true;
  }
}
