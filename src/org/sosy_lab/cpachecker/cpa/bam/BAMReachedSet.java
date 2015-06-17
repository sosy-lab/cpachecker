/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;


public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final BAMTransferRelation transfer;
  private final ARGPath path;
  private final ARGState rootOfSubgraph;
  private final Collection<AbstractState> subgraph;
  private final Map<ARGState, ARGState> subgraphStatesToReachedState;

  private final Function<AbstractState, Precision> GET_PRECISION = new Function<AbstractState, Precision>() {
    @Nullable
    @Override
    public Precision apply(@Nullable AbstractState state) {
      return delegate.asReachedSet().getPrecision(delegate.asReachedSet().getLastState());
      // TODO do we really need the target-precision for refinements and not the actual one?
      // return transfer.getPrecisionForState(Preconditions.checkNotNull(subgraphStatesToReachedState.get(state)), delegate.asReachedSet());
    }
  };

  public BAMReachedSet(BAMTransferRelation pTransfer, ARGReachedSet pMainReachedSet, ARGPath pPath,
      Map<ARGState, ARGState> pSubgraphStatesToReachedState, ARGState pRootOfSubgraph) {
    super(pMainReachedSet);
    this.transfer = pTransfer;
    this.path = pPath;
    this.subgraphStatesToReachedState = pSubgraphStatesToReachedState;
    this.rootOfSubgraph = pRootOfSubgraph;
    this.subgraph = Lists.<AbstractState>newArrayList(rootOfSubgraph.getSubgraph());
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new UnmodifiableReachedSet() {
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
        return Lists.transform(path.asStatesList(), GET_PRECISION);
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
        return path.getLastState();
      }

      @Override
      public boolean hasWaitingState() {
        throw new UnsupportedOperationException("should not be needed");
      }

      @Override
      public Collection<AbstractState> getWaitlist() {
        throw new UnsupportedOperationException("should not be needed");
      }

      @Override
      public Precision getPrecision(AbstractState state) {
        return GET_PRECISION.apply(state);
      }

      @Override
      public boolean contains(AbstractState state) {
        return subgraph.contains(subgraphStatesToReachedState.get(state));
      }

      @Override
      public boolean isEmpty() {
        throw new UnsupportedOperationException("should not be needed");
      }

      @Override
      public int size() {
        throw new UnsupportedOperationException("should not be needed");
      }
    };
  }

  @Override
  public void removeSubtree(ARGState element, Precision newPrecision,
      Predicate<? super Precision> pPrecisionType) {
    ArrayList<Precision> listP = new ArrayList<>();
    listP.add(newPrecision);
    ArrayList<Predicate<? super Precision>> listPT = new ArrayList<>();
    listPT.add(pPrecisionType);
    removeSubtree(element, listP, listPT);
  }

  @Override
  public void removeSubtree(ARGState element, List<Precision> newPrecisions, List<Predicate<? super Precision>> pPrecisionTypes) {
    Preconditions.checkArgument(newPrecisions.size()==pPrecisionTypes.size());
    transfer.removeSubtree(delegate, path, element, newPrecisions, pPrecisionTypes, subgraphStatesToReachedState);
  }

  @Override
  public void removeSubtree(ARGState pE) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(){
    return "BAMReachedSet {{" + delegate.asReachedSet().asCollection().toString() + "}}";
  }
}