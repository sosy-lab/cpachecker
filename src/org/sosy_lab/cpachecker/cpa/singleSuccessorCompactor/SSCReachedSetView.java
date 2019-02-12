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
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

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
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor.SSCSubgraphComputer.SSCARGState;

public class SSCReachedSetView implements UnmodifiableReachedSet {

  private final Collection<AbstractState /* better: SSCARGState */> subgraph;
  private final ARGPath path;
  private final UnmodifiableReachedSet reachedSet;
  private final Function<AbstractState, Precision> prec;

  public SSCReachedSetView(ARGPath pPath, UnmodifiableReachedSet pReachedSet) {
    subgraph = Collections.unmodifiableCollection(pPath.getFirstState().getSubgraph());
    path = pPath;
    reachedSet = pReachedSet;
    prec = s -> reachedSet.getPrecision(((SSCARGState) s).getSSCState());
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
    return Collections2.transform(subgraph, prec);
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
    // SSC-reached-set has no waiting states
    return false;
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    // SSC-reached-set has no waiting states
    return Collections.emptySet();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    return prec.apply(state);
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    subgraph.forEach(state -> pAction.accept(state, prec.apply(state)));
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
