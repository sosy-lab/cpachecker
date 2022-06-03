// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;

/**
 * This reached-set counts the accesses to some method and counts how many states are added,
 * updated, or removed. All operations are forwarded to a nested reached-set.
 */
class StatisticsReachedSet extends ForwardingReachedSet {

  private static class StatHist2 extends StatHist {
    public StatHist2(String pTitle) {
      super(pTitle);
    }

    @Override
    public String toString() {
      // overriding, because printing all data is not that interesting
      return String.format(
          "%.2f (count=%d, dev=%.2f, min=%s, max=%s)",
          getAvg(), getUpdateCount(), getStdDeviation(), getMin(), getMax());
    }
  }

  private final StatCounter numPopFromWaitlist =
      new StatCounter("Number of calls to 'popFromWaitlist'");
  private final StatCounter numRemoveOnlyFromWaitlist =
      new StatCounter("Number of calls to 'removeOnlyFromWaitlist'");
  private final StatCounter numContains = new StatCounter("Number of calls to 'contains'");
  private final StatHist numAdd = new StatHist2("Average size of states for 'add'");
  private final StatHist numRemove = new StatHist2("Average size of states for 'remove'");
  private final StatCounter numReAdd = new StatCounter("Number of calls to 'reAdd'");
  private final StatCounter numUpdatePrecision =
      new StatCounter("Number of calls to 'updatePrecision'");
  private final StatHist sizeOfGetReached =
      new StatHist2("Average size of states from 'getReached'");

  public StatisticsReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    final Collection<AbstractState> reached = super.getReached(pState);
    sizeOfGetReached.insertValue(reached.size());
    return reached;
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    final Collection<AbstractState> reached = super.getReached(pLocation);
    sizeOfGetReached.insertValue(reached.size());
    return reached;
  }

  @Override
  public boolean contains(AbstractState pState) {
    numContains.inc();
    return super.contains(pState);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) throws IllegalArgumentException {
    numAdd.insertValue(1);
    super.add(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    numAdd.insertValue(Iterables.size(pToAdd));
    super.addAll(pToAdd);
  }

  @Override
  public void reAddToWaitlist(AbstractState pState) {
    numReAdd.inc();
    super.reAddToWaitlist(pState);
  }

  @Override
  public void updatePrecision(AbstractState pState, Precision pNewPrecision) {
    numUpdatePrecision.inc();
    super.updatePrecision(pState, pNewPrecision);
  }

  @Override
  public void remove(AbstractState pState) {
    numRemove.insertValue(1);
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    numRemove.insertValue(Iterables.size(pToRemove));
    super.removeAll(pToRemove);
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState pState) {
    numRemoveOnlyFromWaitlist.inc();
    super.removeOnlyFromWaitlist(pState);
  }

  @Override
  public AbstractState popFromWaitlist() {
    numPopFromWaitlist.inc();
    return super.popFromWaitlist();
  }

  @Override
  public ImmutableMap<String, AbstractStatValue> getStatistics() {
    ImmutableMap.Builder<String, AbstractStatValue> builder = ImmutableMap.builder();
    builder.putAll(super.getStatistics());
    put(builder, sizeOfGetReached);
    put(builder, numPopFromWaitlist);
    put(builder, numRemoveOnlyFromWaitlist);
    put(builder, numContains);
    put(builder, numAdd);
    put(builder, numReAdd);
    put(builder, numRemove);
    put(builder, numUpdatePrecision);
    return builder.buildOrThrow();
  }

  private static void put(
      ImmutableMap.Builder<String, AbstractStatValue> builder, AbstractStatValue s) {
    builder.put(s.getTitle(), s);
  }
}
