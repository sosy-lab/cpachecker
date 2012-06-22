/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.abm;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This is an extension of {@link AbstractARGBasedRefiner} that takes care of
 * flattening the ARG before calling {@link #performRefinement0(ReachedSet)}.
 *
 * Warning: Although the ARG is flattened at this point, the elements in it have
 * not been expanded due to performance reasons.
 */
public abstract class AbstractABMBasedRefiner extends AbstractARGBasedRefiner {

  final Timer computePathTimer = new Timer();
  final Timer computeSubtreeTimer = new Timer();
  final Timer computeCounterexampleTimer = new Timer();

  private final ABMTransferRelation transfer;
  private final Map<ARGState, ARGState> pathStateToReachedState = new HashMap<ARGState, ARGState>();

  protected AbstractABMBasedRefiner(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pCpa);

    ABMCPA abmCpa = (ABMCPA)pCpa;
    transfer = abmCpa.getTransferRelation();
    abmCpa.getStatistics().addRefiner(this);
  }

  /**
   * When inheriting from this class, implement this method instead of
   * {@link #performRefinement(ReachedSet)}.
   */
  protected abstract CounterexampleInfo performRefinement0(ARGReachedSet pReached, Path pPath) throws CPAException, InterruptedException;

  @Override
  protected final CounterexampleInfo performRefinement(ARGReachedSet pReached, Path pPath) throws CPAException, InterruptedException {
    if (pPath == null) {
      return CounterexampleInfo.spurious();
    } else {
      return performRefinement0(new ABMReachedSet(transfer, pReached, pPath, pathStateToReachedState), pPath);
    }
  }

  @Override
  protected final Path computePath(ARGState pLastElement, ARGReachedSet pReachedSet) throws InterruptedException, CPATransferException {
    assert pLastElement.isTarget();

    pathStateToReachedState.clear();

    computePathTimer.start();
    try {
      ARGState subgraph;
      computeSubtreeTimer.start();
      try {
        subgraph = transfer.computeCounterexampleSubgraph(pLastElement, pReachedSet, new ARGState(pLastElement.getWrappedState(), null), pathStateToReachedState);
        if (subgraph == null) {
          return null;
        }
      } finally {
        computeSubtreeTimer.stop();
      }

      computeCounterexampleTimer.start();
      try {
        return computeCounterexample(subgraph);
      } finally {
        computeCounterexampleTimer.stop();
      }
    } finally {
      computePathTimer.stop();
    }
  }

  private Path computeCounterexample(ARGState root) {
    Path path = new Path();
    ARGState currentElement = root;
    while (currentElement.getChildren().size() > 0) {
      ARGState child = currentElement.getChildren().iterator().next();

      CFAEdge edge = currentElement.getEdgeToChild(child);
      path.add(Pair.of(currentElement, edge));

      currentElement = child;
    }
    path.add(Pair.of(currentElement, extractLocation(currentElement).getLeavingEdge(0)));
    return path;
  }

  private static class ABMReachedSet extends ARGReachedSet.ForwardingARTReachedSet {

    private final ABMTransferRelation transfer;
    private final Path path;
    private final Map<ARGState, ARGState> pathStateToReachedState;

    private ABMReachedSet(ABMTransferRelation pTransfer, ARGReachedSet pReached, Path pPath, Map<ARGState, ARGState> pPathElementToReachedState) {
      super(pReached);
      this.transfer = pTransfer;
      this.path = pPath;
      this.pathStateToReachedState = pPathElementToReachedState;
    }

    @Override
    public void removeSubtree(ARGState element, Precision newPrecision) {
      transfer.removeSubtree(delegate, path, element, newPrecision, pathStateToReachedState);
    }

    @Override
    public void removeSubtree(ARGState pE) {
      throw new UnsupportedOperationException();
    }
  }
}