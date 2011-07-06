/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;

/**
 * This is an extension of {@link AbstractARTBasedRefiner} that takes care of
 * flattening the ART before calling {@link #performRefinement0(ReachedSet)}.
 *
 * Warning: Although the ART is flattened at this point, the elements in it have
 * not been expanded due to performance reasons.
 */
public abstract class AbstractABMBasedRefiner extends AbstractARTBasedRefiner {

  final Timer computePathTimer = new Timer();
  final Timer computeSubtreeTimer = new Timer();
  final Timer computeCounterexampleTimer = new Timer();

  private final ABMTransferRelation transfer;

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
  protected abstract boolean performRefinement0(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException;

  @Override
  protected final boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {
    if (pPath == null) {
      return true;
    } else {
      return performRefinement0(new ABMReachedSet(transfer, pReached, pPath), pPath);
    }
  }

  @Override
  protected final Path computePath(ARTElement pLastElement, ReachedSet pReachedSet) throws InterruptedException, CPATransferException {
    assert pLastElement.isTarget();

    computePathTimer.start();
    try {
      ARTElement subgraph;
      computeSubtreeTimer.start();
      try {
        subgraph = transfer.computeCounterexampleSubgraph(pLastElement, pReachedSet, new ARTElement(pLastElement.getWrappedElement(), null));
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

  private Path computeCounterexample(ARTElement root) {
    Path path = new Path();
    ARTElement currentElement = root;
    while(currentElement.getChildren().size() > 0) {
      ARTElement child = currentElement.getChildren().iterator().next();

      CFAEdge edge = currentElement.getEdgeToChild(child);
      path.add(Pair.of(currentElement, edge));

      currentElement = child;
    }
    path.add(Pair.of(currentElement, currentElement.retrieveLocationElement().getLocationNode().getLeavingEdge(0)));
    return path;
  }

  private static class ABMReachedSet extends ARTReachedSet.ForwardingARTReachedSet {

    private final ABMTransferRelation transfer;
    private final Path path;

    private ABMReachedSet(ABMTransferRelation pTransfer, ARTReachedSet pReached, Path pPath) {
      super(pReached);
      this.transfer = pTransfer;
      this.path = pPath;
    }

    @Override
    public void removeSubtree(ARTElement element, Precision newPrecision) {
      Precision oldPrecision = Precisions.extractPrecisionByType(getPrecision(getLastElement()), newPrecision.getClass());

      if (newPrecision.equals(oldPrecision)) {
        //Strategy 2
        //restart the analysis
        //TODO: this can be implemented less drastic -> only remove lazy caches (on path)
        System.err.println("Warning: restarting analysis");
        restartAnalysis();

      } else {

        transfer.removeSubtree(delegate, path, element, newPrecision);
      }
    }

    private void restartAnalysis() {

      Precision precision = getPrecision(getLastElement());
      ARTElement child = Iterables.getOnlyElement(getFirstElement().getChildren());
      delegate.removeSubtree(child, precision);

      transfer.clearCaches();
    }

    @Override
    public void removeCoverage(ARTElement pElement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeSubtree(ARTElement pE) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceWithBottom(ARTElement pE) {
      throw new UnsupportedOperationException();
    }
  }
}