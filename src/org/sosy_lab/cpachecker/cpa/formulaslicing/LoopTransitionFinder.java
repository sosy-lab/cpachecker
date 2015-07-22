package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Return a path-formula describing all possible transitions inside the loop.
 */
@Options(prefix="cpa.slicing")
public class LoopTransitionFinder {

  @Option(secure=true, description="Apply AND- LBE transformation to loop "
      + "transition relation.")
  private boolean applyANDtransformation = true;

  @Option(secure=true, description="Apply OR- LBE transformation to loop "
      + "transition relation.")
  private boolean applyORtransformation = true;

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

  public PathFormula generateLoopTransition(
      SSAMap start,
      PointerTargetSet pts,
      CFANode loopHead)
      throws CPATransferException, InterruptedException {

    Preconditions.checkState(cfa.getAllLoopHeads().get().contains(loopHead));

    // Looping edges: intersection of forwards-reachable
    // and backwards-reachable.
    Set<CFAEdge> edgesInLoop = getEdgesInSCC(loopHead);

    // Otherwise it's not a loop.
    Preconditions.checkState(!edgesInLoop.isEmpty());

    List<PathFormula> out = LBE(start, pts, loopHead, edgesInLoop);

    PathFormula first = out.iterator().next();

    for (PathFormula t : out) {
      if (t == first) continue;
      first = pfmgr.makeOr(first, t);
    }

    return first.updateFormula(fmgr.simplify(first.getFormula()));
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

  /**
   * Apply at  large-block-encoding.
   *
   * 1) A - s_1 -> B, B - s_2 ->C is converted to A - s_1 /\ s_2 -> C.
   *
   * 2) A - s_1 -> B, A - s_2 -> B is converted to A - s_1 \/ s_2 -> B.
   * Runs in quadratic time.
   */
  private List<PathFormula> LBE(
      SSAMap start,
      PointerTargetSet pts,
      CFANode loopHead,
      Set<CFAEdge> edgesInLoop)
      throws CPATransferException, InterruptedException {

    logger.log(Level.INFO, "Set of edges in the loop: ", edgesInLoop);

    Set<EdgeWrapper> out = convert(edgesInLoop);

    boolean changed;
    do {
      changed = false;
      if (applyANDtransformation && andLBETransformation(loopHead, out) ||
          applyORtransformation && orLBETransformation(out)) {
        changed = true;
      }
    } while (changed);

    List<PathFormula> outPF = new ArrayList<>(out.size());
    PathFormula empty = new PathFormula(
        fmgr.getBooleanFormulaManager().makeBoolean(true),
        start, pts, 0);
    for (EdgeWrapper e : out) {
      outPF.add(e.makeAnd(empty));
    }

    return outPF;
  }

  /**
   * Apply and- transformation, return whether the passed set was changed.
   */
  private boolean andLBETransformation(
      CFANode loopHead,
      Set<EdgeWrapper> out) {
    for (EdgeWrapper e : out) {

      if (e.getSuccessor() == loopHead) continue;

      EdgeWrapper candidate = null;

      for (EdgeWrapper other : out) {
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
        out.remove(e);
        out.remove(candidate);
        EdgeWrapper added = new AndEdge(ImmutableList.of(e, candidate));
        out.add(added);

        logger.log(Level.ALL, "Removing", e, "and", candidate,
            "adding", added);
        return true;
      }
    }
    return false;
  }

  /**
   * Apply or- transformation, return whether the passed set was changed.
   */
  private boolean orLBETransformation(Set<EdgeWrapper> out) {
    for (EdgeWrapper e : out) {

      List<EdgeWrapper> candidates = new ArrayList<>();
      candidates.add(e);

      for (EdgeWrapper other : out) {
        if (e == other) continue;

        if (other.getPredecessor() == e.getPredecessor() &&
            other.getSuccessor() == e.getSuccessor()) {

          candidates.add(other);
        }
      }

      if (candidates.size() > 1) {
        EdgeWrapper added = new OrEdge(candidates);
        for (EdgeWrapper toRemove : candidates) {
          out.remove(toRemove);
        }
        out.add(added);

        logger.log(Level.ALL, "Removing", candidates,
            "adding", added);
        return true;
      }
    }
    return false;
  }

  private Set<EdgeWrapper> convert(Collection<CFAEdge> edges) {
    Set<EdgeWrapper> out = new HashSet<>(edges.size());
    for (CFAEdge e : edges) {
      out.add(new SingleEdge(e));
    }
    return out;
  }

  private interface EdgeWrapper {
    CFANode getPredecessor();
    CFANode getSuccessor();

    /**
     * Convert to {@link PathFormula} with a given start.
     */
    PathFormula makeAnd(PathFormula prev)
        throws CPATransferException, InterruptedException;
  }

  private class SingleEdge implements EdgeWrapper {
    CFAEdge edge;
    SingleEdge(CFAEdge e) {
      edge = e;
    }

    @Override
    public CFANode getPredecessor() {
      return edge.getPredecessor();
    }

    @Override
    public CFANode getSuccessor() {
      return edge.getSuccessor();
    }

    @Override
    public PathFormula makeAnd(PathFormula prev)
        throws CPATransferException, InterruptedException {
      return pfmgr.makeAnd(prev, edge);
    }
  }

  private class AndEdge implements EdgeWrapper {
    List<EdgeWrapper> edges;
    CFANode predecessor;
    CFANode successor;

    AndEdge(List<EdgeWrapper> pEdges) {
      Preconditions.checkState(!pEdges.isEmpty());
      edges = ImmutableList.copyOf(pEdges);
      successor = Iterables.getLast(pEdges).getSuccessor();
      predecessor = edges.iterator().next().getPredecessor();
    }

    @Override
    public CFANode getPredecessor() {
      return predecessor;
    }

    @Override
    public CFANode getSuccessor() {
      return successor;
    }

    @Override
    public PathFormula makeAnd(PathFormula prev)
        throws CPATransferException, InterruptedException {
      for (EdgeWrapper edge : edges) {
        prev = edge.makeAnd(prev);
      }
      return prev;
    }
  }

  private class OrEdge implements EdgeWrapper {
    List<EdgeWrapper> edges;
    CFANode predecessor;
    CFANode successor;

    OrEdge(List<EdgeWrapper> pEdges) {
      Preconditions.checkState(!pEdges.isEmpty());
      edges = ImmutableList.copyOf(pEdges);
      predecessor = edges.iterator().next().getPredecessor();
      successor = edges.iterator().next().getSuccessor();
    }

    @Override
    public CFANode getPredecessor() {
      return predecessor;
    }

    @Override
    public CFANode getSuccessor() {
      return successor;
    }

    @Override
    public PathFormula makeAnd(PathFormula prev)
        throws CPATransferException, InterruptedException {
      Preconditions.checkState(!edges.isEmpty());

      EdgeWrapper first = edges.iterator().next();
      PathFormula out = first.makeAnd(prev);

      for (EdgeWrapper edge : edges) {
        if (edge == first) continue;
        out = pfmgr.makeOr(out, edge.makeAnd(prev));
      }
      return out;
    }
  }
}
