// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class CompositeStopJoinOperator extends CompositeStopOperator {

  CompositeStopJoinOperator(ImmutableList<StopOperator> stopOperators) {
    super(stopOperators);
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet = getStopSepStates(element, reached, precision);
    return stopJoin(element, reachedSubSet, precision);
  }

  @Override
  public Collection<AbstractState> getCoveringStates(
      AbstractState pElement, Collection<AbstractState> pReachedSet, Precision pPrecision)
      throws CPAException, InterruptedException {
    Collection<AbstractState> reachedSubSet = getStopSepStates(pElement, pReachedSet, pPrecision);
    if (stopJoin(pElement, reachedSubSet, pPrecision)) {
      return reachedSubSet;
    } else {
      return ImmutableSet.of();
    }
  }

  private Collection<AbstractState> getStopSepStates(
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
      // TODO: probably not a good way to distinguish StopOperator types
      if (stopOp instanceof StopJoinOperator) {
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

  private boolean stopJoin(
      AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    if (reached.isEmpty()) {
      return false;
    }

    List<AbstractState> compositeElements = ((CompositeState) element).getWrappedStates();
    List<Precision> compositePrecisions = ((CompositePrecision) precision).getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);
      if (!(stopOp instanceof StopJoinOperator)) {
        continue;
      }

      List<AbstractState> absElems = new ArrayList<>();
      for (AbstractState e : reached) {
        absElems.add(((CompositeState) e).get(idx));
      }

      AbstractState absElem = compositeElements.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem, absElems, prec)) {
        return false;
      }
    }

    return true;
  }
}
