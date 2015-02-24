/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * This is an extension of {@link AbstractARGBasedRefiner} that takes care of
 * flattening the ARG before calling {@link #performRefinement0(ARGReachedSet, MutableARGPath)}.
 *
 * Warning: Although the ARG is flattened at this point, the elements in it have
 * not been expanded due to performance reasons.
 */
public abstract class AbstractBAMBasedRefiner extends AbstractARGBasedRefiner {

  final Timer computePathTimer = new Timer();
  final Timer computeSubtreeTimer = new Timer();
  final Timer computeCounterexampleTimer = new Timer();

  private final BAMTransferRelation transfer;
  private final BAMCPA bamCpa;
  private final Map<ARGState, ARGState> subgraphStatesToReachedState = new HashMap<>();
  private ARGState rootOfSubgraph = null;

  final static BackwardARGState DUMMY_STATE_FOR_MISSING_BLOCK = new BackwardARGState(new ARGState(null, null));

  protected AbstractBAMBasedRefiner(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pCpa);

    bamCpa = (BAMCPA)pCpa;
    transfer = bamCpa.getTransferRelation();
    bamCpa.getStatistics().addRefiner(this);
  }

  /**
   * When inheriting from this class, implement this method instead of
   * {@link #performRefinement(ReachedSet)}.
   */
  protected abstract CounterexampleInfo performRefinement0(ARGReachedSet pReached, ARGPath pPath) throws CPAException, InterruptedException;

  @Override
  protected final CounterexampleInfo performRefinement(ARGReachedSet pReached, ARGPath pPath) throws CPAException, InterruptedException {
    assert pPath == null || pPath.size() > 0;

    if (pPath == null) {
      // The counter-example-path could not be constructed, because of missing blocks (aka "holes").
      // We directly return SPURIOUS and let the CPA-algorithm run again.
      // During the counter-example-path-building we already re-added the start-states of all blocks,
      // that lead to the missing block, to the waitlists of those blocks.
      // Thus missing blocks are analyzed and rebuild again in the next CPA-algorithm.
      return CounterexampleInfo.spurious();
    } else {
      return performRefinement0(new BAMReachedSet(transfer, pReached, pPath, subgraphStatesToReachedState, rootOfSubgraph), pPath);
    }
  }

  @Override
  protected final ARGPath computePath(ARGState pLastElement, ARGReachedSet pReachedSet) throws InterruptedException, CPATransferException {
    assert pLastElement.isTarget();

    subgraphStatesToReachedState.clear();

    computePathTimer.start();
    try {
      computeSubtreeTimer.start();
      try {
        rootOfSubgraph = transfer.computeCounterexampleSubgraph(pLastElement, pReachedSet, subgraphStatesToReachedState);
        if (rootOfSubgraph == DUMMY_STATE_FOR_MISSING_BLOCK) {
          return null;
        }
      } finally {
        computeSubtreeTimer.stop();
      }

      computeCounterexampleTimer.start();
      try {
        // We assume, that every path in the subgraph reaches the target state. Thus we choose randomly.
        return ARGUtils.getRandomPath(rootOfSubgraph);
      } finally {
        computeCounterexampleTimer.stop();
      }
    } finally {
      computePathTimer.stop();
    }
  }

  private static class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

    private final BAMTransferRelation transfer;
    private final ARGPath path;
    private final ARGState rootOfSubgraph;
    private final Collection<AbstractState> subgraph;
    private final Map<ARGState, ARGState> subgraphStatesToReachedState;

    private final Function<AbstractState, Precision> GET_PRECISION = new Function<AbstractState, Precision>() {
      @Nullable
      @Override
      public Precision apply(@Nullable AbstractState state) {
        return transfer.getPrecisionForState(Preconditions.checkNotNull(subgraphStatesToReachedState.get(state)), delegate.asReachedSet());
      }
    };

    private BAMReachedSet(BAMTransferRelation pTransfer, ARGReachedSet pMainReachedSet, ARGPath pPath,
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
      if (true) {
        return delegate.asReachedSet();
      }

      // TODO there is a bug with precision handling in BAM-PredicateAnalysis,
      //      when using the new ReachedSet instead of only the MainReachedSet.
      //      However for ValuaAnalysis-refinement we need the new ReachedSet.
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
        public int getWaitlistSize() {
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
}
