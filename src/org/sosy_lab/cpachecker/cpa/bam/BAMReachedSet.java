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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.time.Timer;
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
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;


public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final BAMCPA bamCpa;
  private final ARGPath path;
  private final ARGState rootOfSubgraph;
  private final Collection<AbstractState> subgraph;
  private final Map<ARGState, ARGState> subgraphStatesToReachedState;
  private final Timer removeCachedSubtreeTimer;

  private final Function<AbstractState, Precision> GET_PRECISION = new Function<AbstractState, Precision>() {
    @Nullable
    @Override
    public Precision apply(@Nullable AbstractState state) {
      return delegate.asReachedSet().getPrecision(delegate.asReachedSet().getLastState());
      // TODO do we really need the target-precision for refinements and not the actual one?
      // return transfer.getPrecisionForState(Preconditions.checkNotNull(subgraphStatesToReachedState.get(state)), delegate.asReachedSet());
    }
  };

  public BAMReachedSet(BAMCPA cpa, ARGReachedSet pMainReachedSet, ARGPath pPath,
      Map<ARGState, ARGState> pSubgraphStatesToReachedState, ARGState pRootOfSubgraph,
      Timer pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    this.bamCpa = cpa;
    this.path = pPath;
    this.subgraphStatesToReachedState = pSubgraphStatesToReachedState;
    this.rootOfSubgraph = pRootOfSubgraph;
    this.subgraph = Collections.<AbstractState>unmodifiableCollection(subgraphStatesToReachedState.keySet());
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;

    assert subgraph.containsAll(path.asStatesList()) : "path should traverse reached states";
    assert pRootOfSubgraph == path.getFirstState() : "path should start with root-state";
    assert subgraph.containsAll(pRootOfSubgraph.getSubgraph()) : "reached states should match states reachable from root";
    assert pRootOfSubgraph.getSubgraph().containsAll(subgraph) : "states reachable from root should match reached states";
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
        return Collections2.transform(subgraph, GET_PRECISION);
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
        return subgraph.contains(state);
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

  @SuppressWarnings("unchecked")
  @Override
  public void removeSubtree(ARGState element, Precision newPrecision,
      Predicate<? super Precision> pPrecisionType) {
    removeSubtree(element, Lists.newArrayList(newPrecision), Lists.<Predicate<? super Precision>>newArrayList(pPrecisionType));
  }

  @Override
  public void removeSubtree(ARGState element, List<Precision> newPrecisions, List<Predicate<? super Precision>> pPrecisionTypes) {
    Preconditions.checkArgument(newPrecisions.size()==pPrecisionTypes.size());
    assert subgraphStatesToReachedState.containsKey(element);
    final ARGSubtreeRemover argSubtreeRemover = new ARGSubtreeRemover(bamCpa, removeCachedSubtreeTimer);
    argSubtreeRemover.removeSubtree(delegate, path, element,
            newPrecisions, pPrecisionTypes);

    // post-processing, cleanup data-structures.
    // We remove all states reachable from 'element'. This step is not precise,
    // because sub-reached-sets might be changed and we do not remove the corresponding states.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    for (ARGState state : element.getSubgraph()) {
      subgraphStatesToReachedState.remove(state);
      // subgraph is backed by subgraphStatesToReachedState.keys(), so we do not need to update it.
      state.removeFromARG();
    }
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