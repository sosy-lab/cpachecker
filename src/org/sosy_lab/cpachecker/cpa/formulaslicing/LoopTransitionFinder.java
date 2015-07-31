package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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

  @Option(secure=true, description="Instead of considering the entire SCC, "
      + "ignore the function nested in the loop. UNSOUND!")
  private boolean ignoreFunctionCallsInLoop = false;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final LoopStructure loopStructure;
  private final FormulaSlicingStatistics statistics;

  public LoopTransitionFinder(
      Configuration config,
      CFA pCfa, PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, LogManager pLogger,
      FormulaSlicingStatistics pStatistics)
      throws InvalidConfigurationException {
    config.inject(this);
    statistics = pStatistics;
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    logger = pLogger;
    loopStructure = pCfa.getLoopStructure().get();
  }

  public PathFormula generateLoopTransition(
      SSAMap start,
      PointerTargetSet pts,
      CFANode loopHead)
      throws CPATransferException, InterruptedException {

    Preconditions.checkState(loopStructure.getAllLoopHeads()
        .contains(loopHead));

      // Looping edges: intersection of forwards-reachable
      // and backwards-reachable.
      List<CFAEdge> edgesInLoop = getEdgesInSCC(loopHead);

      // Otherwise it's not a loop.
      Preconditions.checkState(!edgesInLoop.isEmpty());

    PathFormula out;
    statistics.LBEencodingTimer.start();
    try {
      out = LBE(start, pts, edgesInLoop);
    } finally {
      statistics.LBEencodingTimer.stop();
    }

    return out.updateFormula(fmgr.simplify(out.getFormula()));
  }


  /**
   * @return all edges in the local {@link Loop} associated with the {@code node},
   * or an empty set, if {@code node} is not a loop-head.
   */
  private List<CFAEdge> getEdgesInSCC(CFANode node) {
    if (ignoreFunctionCallsInLoop) {
      // Returns *local* loop.
      Set<CFAEdge> out = new HashSet<>();
      for (Loop loop :
          loopStructure.getLoopsForLoopHead(node)) {
        out.addAll(loop.getInnerLoopEdges());
      }
      return ImmutableList.copyOf(out);
    } else {
      EdgeCollectingCFAVisitor v1 = new EdgeCollectingCFAVisitor();
      EdgeCollectingCFAVisitor v2 = new EdgeCollectingCFAVisitor();

      // Note that we CAN NOT ignore function calls, as the loop
      // may contain functions inside which do affect the global state.
      CFATraversal.dfs().ignoreSummaryEdges().traverse(node, v1);
      CFATraversal.dfs().ignoreSummaryEdges().backwards().traverse(node, v2);

      Set<CFAEdge> s1 = ImmutableSet.copyOf(v1.getVisitedEdges());
      Set<CFAEdge> s2 = ImmutableSet.copyOf(v2.getVisitedEdges());
      Set<CFAEdge> intersection = Sets.intersection(s1, s2);
      return ImmutableList.copyOf(intersection);
    }
  }

  /**
   * Apply large-block-encoding to a list of {@link PathFormula}s.
   *
   * 1) A - s_1 -> B, B - s_2 ->C,
   *    no other incoming edge to B, B is not a loop-head,
   *    is converted to A - s_1 /\ s_2 -> C.
   *
   * 2) A - s_1 -> B, A - s_2 -> B is converted to A - s_1 \/ s_2 -> B.
   *
   * Runs in cubic time, uses fixpoint computation.
   * todo: faster computation.
   */
  private PathFormula LBE(
      SSAMap start,
      PointerTargetSet pts,
      List<CFAEdge> edgesInLoop)
      throws CPATransferException, InterruptedException {

    Set<EdgeWrapper> out = convert(edgesInLoop);

    boolean changed;
    do {
      changed = false;
      if (applyANDtransformation && andLBETransformation(out) ||
          applyORtransformation && orLBETransformation(out)) {
        changed = true;
      }
    } while (changed);

    PathFormula empty = new PathFormula(
        fmgr.getBooleanFormulaManager().makeBoolean(true),
        start, pts, 0);
    EdgeWrapper outEdge;
    if (out.size() == 1) {
      outEdge = out.iterator().next();
    } else {
      outEdge = new OrEdge(ImmutableList.copyOf(out));
    }
    return outEdge.toPathFormula(empty);
  }

  /**
   * Apply and- transformation, return whether the passed set was changed.
   */
  private boolean andLBETransformation(
      Set<EdgeWrapper> out) {
    for (EdgeWrapper e : out) {

      CFANode predecessor = e.getPredecessor();

      // Do not perform reduction on nodes ending in a loop-head.
      if (loopStructure.getAllLoopHeads().contains(predecessor)) continue;

      EdgeWrapper candidate = null;

      for (EdgeWrapper other : out) {
        if (e == other) continue;

        if (other.getSuccessor() == predecessor) {
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
        EdgeWrapper added = new AndEdge(ImmutableList.of(candidate, e));
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
    PathFormula toPathFormula(PathFormula prev)
        throws CPATransferException, InterruptedException;

    /**
     * Pretty-print with a given prefix.
     */
    String prettyPrint(String prefix);
  }

  private class SingleEdge implements EdgeWrapper {
    private final CFAEdge edge;

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
    public PathFormula toPathFormula(PathFormula prev)
        throws CPATransferException, InterruptedException {
      return pfmgr.makeAnd(prev, edge);
    }

    @Override
    public String toString() {
      return prettyPrint("");
    }

    @Override
    public String prettyPrint(String prefix) {
      return String.format(
          "%s%s->%s(%s)",
          prefix,
          getPredecessor(),
          getSuccessor(),
          edge.getCode()
      );
    }
  }

  private class AndEdge implements EdgeWrapper {
    private final List<EdgeWrapper> edges;
    private final CFANode predecessor;
    private final CFANode successor;

    AndEdge(List<EdgeWrapper> pEdges) {
      Preconditions.checkState(!pEdges.isEmpty());
      List<EdgeWrapper> l = new ArrayList<>();
      for (EdgeWrapper w : pEdges) {
        if (w instanceof AndEdge) {

          // Simplification.
          l.addAll(((AndEdge) w).edges);
        } else {
          l.add(w);
        }
      }
      edges = ImmutableList.copyOf(l);
      predecessor = edges.iterator().next().getPredecessor();
      successor = Iterables.getLast(edges).getSuccessor();
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
    public PathFormula toPathFormula(PathFormula prev)
        throws CPATransferException, InterruptedException {
      for (EdgeWrapper edge : edges) {
        prev = edge.toPathFormula(prev);
      }
      return prev;
    }

    @Override
    public String toString() {
      return prettyPrint("");
    }

    @Override
    public String prettyPrint(String prefix) {
      return prettyPrintHelper(edges, prefix, "AND");
    }
  }

  private class OrEdge implements EdgeWrapper {
    private final List<EdgeWrapper> edges;
    private final CFANode predecessor;
    private final CFANode successor;

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
    public PathFormula toPathFormula(PathFormula prev)
        throws CPATransferException, InterruptedException {
      Preconditions.checkState(!edges.isEmpty());

      EdgeWrapper first = edges.iterator().next();
      PathFormula out = first.toPathFormula(prev);

      for (EdgeWrapper edge : edges) {
        if (edge == first) continue;
        out = pfmgr.makeOr(out, edge.toPathFormula(prev));
      }
      return out;
    }

    @Override
    public String toString() {
      return prettyPrint("");
    }

    @Override
    public String prettyPrint(String prefix) {
      return prettyPrintHelper(edges, prefix, "OR");
    }
  }

  private String prettyPrintHelper(
      Iterable<EdgeWrapper> edges,
      String prefix, String funcName) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix).append(funcName).append("(\n");
    for (EdgeWrapper w : edges) {
      sb.append(w.prettyPrint(prefix + "\t")).append(",\n");
    }
    sb.append(prefix).append(")");
    return sb.toString();
  }
}
