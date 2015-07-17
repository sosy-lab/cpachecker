package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Return a path-formula describing all possible transitions inside the loop.
 */
public class LoopTransitionFinder {
  private final CFA cfa;
  private final PathFormulaManager pfmgr;

  public LoopTransitionFinder(CFA pCfa, PathFormulaManager pPfmgr) {
    cfa = pCfa;
    pfmgr = pPfmgr;
  }

  public PathFormula generateLoopTransition(CFANode loopHead)
      throws CPATransferException, InterruptedException {
    Preconditions.checkState(cfa.getAllLoopHeads().get().contains(loopHead));

    // todo: pre-apply large block encoding for a smaller amount of edges.
    // has a potential to increase both precision and speed.

    // Looping edges: intersection of forwards-reachable
    // and backwards-reachable.
    EdgeCollectingCFAVisitor forwardVisitor = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(loopHead, forwardVisitor);
    Set<CFAEdge> forwardEdges = ImmutableSet.copyOf(forwardVisitor.getVisitedEdges());
    EdgeCollectingCFAVisitor backwardVisitor = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().backwards().traverse(loopHead, backwardVisitor);
    Set<CFAEdge> backwardEdges = ImmutableSet.copyOf(
        backwardVisitor.getVisitedEdges());
    Set<CFAEdge> edgesInLoop = Sets.intersection(forwardEdges, backwardEdges);

    CFAEdge first = Iterables.getFirst(edgesInLoop, null);
    PathFormula formula = pfmgr.makeFormulaForPath(ImmutableList.of(first));
    for (CFAEdge edge : edgesInLoop) {
      if (edge == first) continue;
      formula = pfmgr.makeOr(formula, pfmgr.makeFormulaForPath(ImmutableList.of(edge)));
    }

    return formula;
  }
}
