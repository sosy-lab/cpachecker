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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/** View on the set of states computed by {@link BAMCEXSubgraphComputer}. */
final class BAMReachedSetView implements UnmodifiableReachedSet {

  private final ARGState rootOfSubgraph;
  private final ARGState lastState;
  private final Function<AbstractState, Precision> precisionGetter;
  private final Collection<AbstractState> subgraph;

  BAMReachedSetView(ARGState pRootOfSubgraph, ARGState pLastState,
      Function<AbstractState, Precision> pPrecisionGetter) {
    rootOfSubgraph = pRootOfSubgraph;
    lastState = pLastState;
    precisionGetter = pPrecisionGetter;
    subgraph = Collections.unmodifiableCollection(pRootOfSubgraph.getSubgraph());
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
}