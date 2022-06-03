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
import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

/**
 * Live view of an unmodifiable reached state set, where states and precision are transformed by
 * mapping functions.
 */
public class UnmodifiableReachedSetView implements UnmodifiableReachedSet {
  private final UnmodifiableReachedSet underlying;
  private final Function<? super AbstractState, AbstractState> mapStateFunction;
  private final Function<? super Precision, Precision> mapPrecisionFunction;

  public UnmodifiableReachedSetView(
      UnmodifiableReachedSet pUnderlyingSet,
      Function<? super AbstractState, AbstractState> pMapStateFunction,
      Function<? super Precision, Precision> pMapPrecisionFunction) {
    checkNotNull(pUnderlyingSet);
    checkNotNull(pMapStateFunction);
    checkNotNull(pMapPrecisionFunction);

    underlying = pUnderlyingSet;
    mapStateFunction = pMapStateFunction;
    mapPrecisionFunction = pMapPrecisionFunction;
  }

  @Override
  public @Nullable AbstractState getFirstState() {
    return checkNotNull(mapStateFunction.apply(underlying.getFirstState()));
  }

  @Override
  public AbstractState getLastState() {
    return mapStateFunction.apply(underlying.getLastState());
  }

  @Override
  public Precision getPrecision(AbstractState pState) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping prevents reverse mapping");
  }

  @Override
  public Collection<AbstractState> asCollection() {
    return Collections2.transform(underlying.asCollection(), mapStateFunction);
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping prevents knowing the partition");
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return Collections2.transform(underlying.getReached(pLocation), mapStateFunction);
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections2.transform(underlying.getPrecisions(), mapPrecisionFunction);
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return Collections2.transform(underlying.getWaitlist(), mapStateFunction);
  }

  @Override
  public boolean hasWaitingState() {
    return underlying.hasWaitingState();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterators.transform(underlying.iterator(), mapStateFunction);
  }

  @Override
  public Stream<AbstractState> stream() {
    return underlying.stream().map(mapStateFunction);
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    checkNotNull(pAction);
    underlying.forEach(
        (state, precision) ->
            pAction.accept(mapStateFunction.apply(state), mapPrecisionFunction.apply(precision)));
  }

  @Override
  public boolean contains(AbstractState pState) {
    throw new UnsupportedOperationException("Unwrapping may prevent to check contains");
  }

  @Override
  public boolean isEmpty() {
    return underlying.isEmpty();
  }

  @Override
  public int size() {
    return underlying.size();
  }

  @Override
  public boolean wasTargetReached() {
    return underlying.wasTargetReached();
  }

  @Override
  public Collection<TargetInformation> getTargetInformation() {
    return underlying.getTargetInformation();
  }
}
