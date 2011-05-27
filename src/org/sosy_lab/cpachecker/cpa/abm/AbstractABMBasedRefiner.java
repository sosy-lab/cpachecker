package org.sosy_lab.cpachecker.cpa.abm;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
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
 * 
 * It also offers a {@link #removeSubtree(ARTReachedSet, Path, ARTElement, Precision)}
 * method which should be used instead of {@link ARTReachedSet#removeSubtree(ARTElement)}.
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
  public abstract boolean performRefinement0(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException;

  @Override
  public final boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {
    if (pPath == null) {
      //TODO:  this can be implemented less drastic -> only remove calls on counterexample
      restartAnalysis(pReached);
      return true;
    } else {
      return performRefinement0(pReached, pPath);
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
  
  protected final BlockPartitioning getBlockPartitioning() {
    return transfer.getBlockPartitioning();
  }
  
  /**
   * Call this method when you normally would call {@link ARTReachedSet#removeSubtree(ARTElement, Precision)}.
   */
  protected final void removeSubtree(ARTReachedSet reachSet, Path pPath, ARTElement element, Precision newPrecision) {
    Precision oldPrecision = Precisions.extractPrecisionByType(reachSet.getPrecision(reachSet.getLastElement()), newPrecision.getClass());

    if (newPrecision.equals(oldPrecision)) {
      //Strategy 2
      //restart the analysis
      //TODO: this can be implemented less drastic -> only remove lazy caches (on path)      
      restartAnalysis(reachSet);
      return;     
    }

    transfer.removeSubtree(reachSet, pPath, element, newPrecision);
  }
  
  
  private void restartAnalysis(ARTReachedSet reachSet) {
   
    Precision precision = reachSet.getPrecision(reachSet.getLastElement());
    ARTElement child = Iterables.getOnlyElement(reachSet.getFirstElement().getChildren());
    reachSet.removeSubtree(child, precision);
    
    transfer.clearCaches();
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
}