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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;

/**
 * This is an extension of {@link AbstractARGBasedRefiner} that takes care of
 * flattening the ARG before calling {@link #performRefinement0(ReachedSet)}.
 *
 * Warning: Although the ARG is flattened at this point, the elements in it have
 * not been expanded due to performance reasons.
 */
public abstract class AbstractBAMBasedRefiner extends AbstractARGBasedRefiner {

  final Timer computePathTimer = new Timer();
  final Timer computeSubtreeTimer = new Timer();
  final Timer computeCounterexampleTimer = new Timer();

  private final BAMTransferRelation transfer;
  private final Map<ARGState, ARGState> pathStateToReachedState = new HashMap<>();

  protected AbstractBAMBasedRefiner(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pCpa);

    BAMCPA bamCpa = (BAMCPA)pCpa;
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
    if (pPath == null) {
      return CounterexampleInfo.spurious();
    } else {
      return performRefinement0(new BAMReachedSet(transfer, pReached, pPath, pathStateToReachedState), pPath);
    }
  }

  @Override
  protected final ARGPath computePath(ARGState pLastElement, ARGReachedSet pReachedSet) throws InterruptedException, CPATransferException {
    assert pLastElement.isTarget();

    pathStateToReachedState.clear();

    computePathTimer.start();
    try {
      ARGState subgraph;
      computeSubtreeTimer.start();
      try {
        subgraph = transfer.computeCounterexampleSubgraph(pLastElement, pReachedSet, pathStateToReachedState);
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

  private ARGPath computeCounterexample(ARGState root) {
    ARGPath path = new ARGPath();
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

  private static class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

    private final BAMTransferRelation transfer;
    private final ARGPath path;
    private final Map<ARGState, ARGState> pathStateToReachedState;

    private BAMReachedSet(BAMTransferRelation pTransfer, ARGReachedSet pReached, ARGPath pPath, Map<ARGState, ARGState> pPathElementToReachedState) {
      super(pReached);
      this.transfer = pTransfer;
      this.path = pPath;
      this.pathStateToReachedState = pPathElementToReachedState;
    }

    @Override
    public void removeSubtree(ARGState element, Precision newPrecision,
        Class<? extends Precision> pPrecisionType) {
      ArrayList<Precision> listP = new ArrayList<>();
      listP.add(newPrecision);
      ArrayList<Class<? extends Precision>> listPT = new ArrayList<>();
      listPT.add(pPrecisionType);
      removeSubtree(element, listP, listPT);
    }

    @Override
    public void removeSubtree(ARGState element, List<Precision> newPrecisions, List<Class<? extends Precision>> pPrecisionTypes) {
      Preconditions.checkArgument(newPrecisions.size()==pPrecisionTypes.size());
      transfer.removeSubtree(delegate, path, element, newPrecisions, pPrecisionTypes, pathStateToReachedState);
    }

    @Override
    public void removeSubtree(ARGState pE) {
      throw new UnsupportedOperationException();
    }
  }
}
