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
package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
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
  private Collection<AbstractState> subgraph; // lazy

  public UnmodifiableSubgraphReachedSetView(
      ARGPath pPath, Function<AbstractState, Precision> pPrecisionGetter) {
    path = checkNotNull(pPath);
    precisionGetter = checkNotNull(pPrecisionGetter);
  }

  @Override
  public Collection<AbstractState> asCollection() {
    if (subgraph == null) {
      subgraph = Collections.unmodifiableCollection(path.getFirstState().getSubgraph());
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
