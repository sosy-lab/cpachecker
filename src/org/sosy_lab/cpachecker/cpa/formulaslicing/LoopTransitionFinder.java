package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Return a path-formula describing all possible transitions inside the loop.
 */
public class LoopTransitionFinder {
  private final CFA cfa;
  private final PathFormulaManager pfmgr;
  private final SetMultimap<CFANode, CFAEdge> loopEdgesCache;
  private final FormulaManagerView fmgr;
  private final LogManager logger;

  public LoopTransitionFinder(CFA pCfa, PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, LogManager pLogger) {
    cfa = pCfa;
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    logger = pLogger;
    loopEdgesCache = HashMultimap.create();
  }

  public PathFormula generateLoopTransition(CFANode loopHead)
      throws CPATransferException, InterruptedException {
    Preconditions.checkState(cfa.getAllLoopHeads().get().contains(loopHead));

    // Looping edges: intersection of forwards-reachable
    // and backwards-reachable.
    Set<CFAEdge> edgesInLoop = getEdgesInSCC(loopHead);
    edgesInLoop = LBE(loopHead, edgesInLoop);

    // Otherwise it's not a loop.
    Preconditions.checkState(!edgesInLoop.isEmpty());

    CFAEdge first = edgesInLoop.iterator().next();
    PathFormula formula = pfmgr.makeFormulaForPath(ImmutableList.of(first));
    for (CFAEdge edge : edgesInLoop) {
      if (edge == first) continue;
      formula = pfmgr.makeOr(formula, pfmgr.makeFormulaForPath(ImmutableList.of(edge)));
    }

    return formula.updateFormula(fmgr.simplify(formula.getFormula()));
  }

  /**
   * Apply at least (partial) large-block-encoding.
   * A->B, B->C is converted to A->C.
   * Runs in quadratic time.
   *
   * Unfortunately this interface is not possible if we want to handle
   * disjunctions as well.
   */
  private Set<CFAEdge> LBE(
      CFANode loopHead,
      Set<CFAEdge> edgesInLoop) {
    while (true) { // Fixpoint computation.
      boolean changed = false;

      for (CFAEdge e : edgesInLoop) {
        if (e.getSuccessor() == loopHead) continue;
        CFAEdge candidate = null;

        for (CFAEdge other : edgesInLoop) {
          if (e == other) continue;

          if (other.getPredecessor() == e.getSuccessor()) {
            if (candidate == null) {
              candidate = other;
            } else {

              // Do not perform replacement,
              // if there are two candidates we should stop.
              candidate = null;
              break;
            }
          }
        }

        if (candidate != null) {
          edgesInLoop.remove(e);
          edgesInLoop.remove(candidate);
          CFAEdge added = new MultiEdge(e.getPredecessor(),
              candidate.getSuccessor(), ImmutableList.of(e, candidate));
          edgesInLoop.add(added);

          logger.log(Level.INFO, "Removing", e, "and", candidate,
              "adding", added);
          changed = true;
          break;
        }
      }

      if (!changed) break;
    }

    return edgesInLoop;
  }

  public Set<CFAEdge> getEdgesInSCC(CFANode loopHead) {
    Preconditions.checkState(cfa.getAllLoopHeads().get().contains(loopHead));

    Set<CFAEdge> out = loopEdgesCache.get(loopHead);

    // Note that loop has to contain at least one looping edge.
    if (out.isEmpty()) {

      // Populate the cache on demand.
      EdgeCollectingCFAVisitor forwardVisitor = new EdgeCollectingCFAVisitor();
      CFATraversal.dfs().traverse(loopHead, forwardVisitor);
      Set<CFAEdge> forwardEdges = ImmutableSet.copyOf(
          forwardVisitor.getVisitedEdges());
      EdgeCollectingCFAVisitor backwardVisitor = new EdgeCollectingCFAVisitor();
      CFATraversal.dfs().backwards().traverse(loopHead, backwardVisitor);
      Set<CFAEdge> backwardEdges = ImmutableSet.copyOf(
          backwardVisitor.getVisitedEdges());
      out = Sets.intersection(forwardEdges, backwardEdges);

      loopEdgesCache.putAll(loopHead, out);
    }

    return out;
  }
}
