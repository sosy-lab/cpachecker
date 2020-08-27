// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/**
 * This class provides an unmodifiable view on a subgraph of states. The subgraph is computed lazily
 * from the states reachable from the given first state. The view has an empty waitlist.
 */
public class UnmodifiableSubgraphReachedSetView implements UnmodifiableReachedSet {

  /** a path from first until last state. */
  private final ARGPath path;

  /** a function to compute a precision for every state in this reached-set. */
  protected final Function<AbstractState, Precision> precisionGetter;

  /** the full set of states in this reached-set. all states are reachable from the first state. */
  private ImmutableSet<AbstractState> subgraph; // lazy

  public UnmodifiableSubgraphReachedSetView(
      ARGPath pPath, Function<AbstractState, Precision> pPrecisionGetter) {
    path = checkNotNull(pPath);
    precisionGetter = checkNotNull(pPrecisionGetter);
  }

  @Override
  public Collection<AbstractState> asCollection() {
    if (subgraph == null) {
      subgraph = ImmutableSet.copyOf(path.getFirstState().getSubgraph());
      assert subgraph.containsAll(path.asStatesList());
    }
    return subgraph;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return asCollection().iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections2.transform(asCollection(), precisionGetter);
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
    return path.getFirstState();
  }

  @Override
  public AbstractState getLastState() {
    return path.getLastState();
  }

  @Override
  public boolean hasWaitingState() {
    return false;
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return ImmutableSet.of();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    return checkNotNull(precisionGetter.apply(checkNotNull(state)));
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    asCollection().forEach(state -> pAction.accept(state, precisionGetter.apply(state)));
  }

  @Override
  public boolean contains(AbstractState state) {
    return asCollection().contains(checkNotNull(state));
  }

  @Override
  public boolean isEmpty() {
    return asCollection().isEmpty();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException("should not be needed");
  }
}
