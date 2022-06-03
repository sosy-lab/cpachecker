// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;

/**
 * Implementation of ReachedSet that forwards all calls to another instance. The target instance is
 * changable.
 */
public class ForwardingReachedSet implements ReachedSet, StatisticsProvider {

  private volatile ReachedSet delegate;

  public ForwardingReachedSet(ReachedSet pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  public ReachedSet getDelegate() {
    return delegate;
  }

  public void setDelegate(ReachedSet pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Set<AbstractState> asCollection() {
    return delegate.asCollection();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return delegate.iterator();
  }

  @Override
  public Stream<AbstractState> stream() {
    return delegate.stream();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return delegate.getPrecisions();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    return delegate.getReached(pState);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return delegate.getReached(pLocation);
  }

  @Override
  public AbstractState getFirstState() {
    return delegate.getFirstState();
  }

  @Override
  public AbstractState getLastState() {
    return delegate.getLastState();
  }

  @Override
  public boolean hasWaitingState() {
    return delegate.hasWaitingState();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return delegate.getWaitlist();
  }

  @Override
  public Precision getPrecision(AbstractState pState) throws UnsupportedOperationException {
    return delegate.getPrecision(pState);
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    delegate.forEach(pAction);
  }

  @Override
  public boolean contains(AbstractState pState) {
    return delegate.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) throws IllegalArgumentException {
    delegate.add(pState, pPrecision);
  }

  @Override
  public void addNoWaitlist(AbstractState pState, Precision pPrecision)
      throws IllegalArgumentException {
    delegate.addNoWaitlist(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    delegate.addAll(pToAdd);
  }

  @Override
  public void reAddToWaitlist(AbstractState pE) {
    delegate.reAddToWaitlist(pE);
  }

  @Override
  public void updatePrecision(AbstractState pE, Precision pNewPrecision) {
    delegate.updatePrecision(pE, pNewPrecision);
  }

  @Override
  public void remove(AbstractState pState) {
    delegate.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    delegate.removeAll(pToRemove);
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState pState) {
    delegate.removeOnlyFromWaitlist(pState);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public void clearWaitlist() {
    delegate.clearWaitlist();
  }

  @Override
  public AbstractState popFromWaitlist() {
    return delegate.popFromWaitlist();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    checkNotNull(statsCollection);
    if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(statsCollection);
    }
  }

  @Override
  public boolean wasTargetReached() {
    return delegate.wasTargetReached();
  }

  @Override
  public Collection<TargetInformation> getTargetInformation() {
    return delegate.getTargetInformation();
  }

  @Override
  public ImmutableMap<String, AbstractStatValue> getStatistics() {
    return delegate.getStatistics();
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return delegate.getCPA();
  }
}
