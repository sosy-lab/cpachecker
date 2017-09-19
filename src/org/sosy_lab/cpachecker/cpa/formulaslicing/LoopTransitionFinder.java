package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;

/**
 * Return a path-formula describing all possible transitions inside the loop.
 */
@Options(prefix="cpa.slicing")
public class LoopTransitionFinder implements StatisticsProvider {

  /**
   * Statistics for formula slicing.
   */
  private static class Stats implements Statistics {
    final Timer LBEencodingTimer = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.printf("Time spent in LBE Encoding: %s (Max: %s), (Avg: %s)%n",
          LBEencodingTimer,
          LBEencodingTimer.getMaxTime().formatAs(TimeUnit.SECONDS),
          LBEencodingTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
    }

    @Override
    public String getName() {
      return "LBE Encoding of Loops";
    }
  }

  @Option(secure=true, description="Apply AND- LBE transformation to loop "
      + "transition relation.")
  private boolean applyLBETransformation = true;

  @Option(
      secure = true,
      description =
          "Time for loop generation before aborting.\n"
              + "(Use seconds or specify a unit; 0 for infinite)"
  )
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
   private TimeSpan timeForLoopGeneration = TimeSpan.ofSeconds(0);

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final LoopStructure loopStructure;
  private final Stats statistics;
  private final ShutdownNotifier shutdownNotifier;

  private final Map<CFANode, Table<CFANode, CFANode, EdgeWrapper>> LBEcache;

  public LoopTransitionFinder(
      Configuration config,
      LoopStructure pLoopStructure, PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    shutdownNotifier = pShutdownNotifier;
    config.inject(this);
    statistics = new Stats();
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    logger = pLogger;
    loopStructure = pLoopStructure;

    LBEcache = new HashMap<>();
  }

  public PathFormula generateLoopTransition(
      SSAMap start,
      PointerTargetSet pts,
      CFANode loopHead)
      throws CPATransferException, InterruptedException {

    Preconditions.checkState(loopStructure.getAllLoopHeads()
        .contains(loopHead));

    ShutdownManager loopGenerationShutdown = ShutdownManager.createWithParent(shutdownNotifier);
    ResourceLimitChecker limits = null;
    if (!timeForLoopGeneration.isEmpty()) {
      WalltimeLimit l = WalltimeLimit.fromNowOn(timeForLoopGeneration);
      limits =
          new ResourceLimitChecker(
              loopGenerationShutdown, Collections.singletonList(l));
      limits.start();
    }

    PathFormula out;
    statistics.LBEencodingTimer.start();
    try {
      out = performLargeBlockEncoding(loopHead, start, pts);
    } finally {
      statistics.LBEencodingTimer.stop();
    }

    if (!timeForLoopGeneration.isEmpty()) {
      limits.cancel();
    }

    return out;
  }


  /**
   * @return all edges in the local {@link Loop} associated with the {@code node},
   * or an empty set, if {@code node} is not a loop-head.
   */
  private List<CFAEdge> getEdgesInSCC(CFANode node) {
    SummarizingVisitor forwardVisitor = new SummarizingVisitorForward();
    SummarizingVisitor backwardsVisitor = new SummarizingVisitorBackwards();

    CFATraversal.dfs().traverse(node, forwardVisitor);
    CFATraversal.dfs().backwards().traverse(node, backwardsVisitor);

    Set<CFAEdge> forwardsReachable = ImmutableSet.copyOf(forwardVisitor.getVisitedEdges());
    Set<CFAEdge> backwardsReachable = ImmutableSet.copyOf(backwardsVisitor.getVisitedEdges());

    Set<CFAEdge> intersection = Sets.intersection(forwardsReachable, backwardsReachable);
    return ImmutableList.copyOf(intersection);
  }


  /**
   * Traverse the CFA, *including* function calls, yet on return edge, return only to functions
   * we have already been to.
   *
   * <p>Otherwise, inter-procedural traversal quickly traverses entire CFA, even unreachable
   * parts (e.g. consider a {@code log()} function called from everywhere).
   */
  private abstract static class SummarizingVisitor implements CFAVisitor {
    final Set<CFAEdge> visitedEdges = new HashSet<>();
    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      if (visitedEdges.contains(edge)) {
        return TraversalProcess.SKIP;
      }
      if (edge instanceof FunctionCallEdge) {
        return onCallEdge(edge);

      } else if (edge instanceof FunctionReturnEdge) {
        return onReturnEdge(edge);
      } else if (edge instanceof FunctionSummaryEdge) {
        return TraversalProcess.SKIP;
      } else {
        visitedEdges.add(edge);
        return TraversalProcess.CONTINUE;
      }
    }

    abstract TraversalProcess onCallEdge(CFAEdge callEdge);

    abstract TraversalProcess onReturnEdge(CFAEdge returnEdge);

    @Override
    public TraversalProcess visitNode(CFANode node) {
      return TraversalProcess.CONTINUE;
    }

    private ImmutableSet<CFAEdge> getVisitedEdges() {
      return ImmutableSet.copyOf(visitedEdges);
    }
  }

  /**
   * Jump only to join points which are reachable.
   */
  private static class SummarizingVisitorForward extends SummarizingVisitor {
    private final Set<CFANode> expectedJoinNodes = new HashSet<>();

    @Override
    TraversalProcess onCallEdge(CFAEdge callEdge) {
      visitedEdges.add(callEdge);
      expectedJoinNodes.add(callEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor());
      return TraversalProcess.CONTINUE;
    }

    @Override
    TraversalProcess onReturnEdge(CFAEdge returnEdge) {
      if (expectedJoinNodes.contains(returnEdge.getSuccessor())) {
        visitedEdges.add(returnEdge);
        return TraversalProcess.CONTINUE;
      } else {

        // Returning to a function we can never get to.
        return TraversalProcess.SKIP;
      }
    }
  }

  /**
   * Jump only to callsites which are reachable.
   */
  private static final class SummarizingVisitorBackwards extends SummarizingVisitor {

    private final Set<CFANode> expectedCallsites = new HashSet<>();

    @Override
    TraversalProcess onReturnEdge(CFAEdge edge) {
      CFANode callsite = edge.getSuccessor().getEnteringSummaryEdge().getPredecessor();
      expectedCallsites.add(callsite);
      visitedEdges.add(edge);
      return TraversalProcess.CONTINUE;
    }

    @Override
    TraversalProcess onCallEdge(CFAEdge edge) {
      if (expectedCallsites.contains(edge.getPredecessor())) {
        visitedEdges.add(edge);
        return TraversalProcess.CONTINUE;
      } else {

        // Returning to a function we can never get to.
        return TraversalProcess.SKIP;
      }
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
   * Runs in (at most) quadratic time.
   *
   */
  private PathFormula performLargeBlockEncoding(
      CFANode loopHead,
      SSAMap start,
      PointerTargetSet pts)
      throws CPATransferException, InterruptedException {

    // successor -> predecessor -> EdgeWrapper
    Table<CFANode, CFANode, EdgeWrapper> out;

    if (LBEcache.containsKey(loopHead)) {

      out = LBEcache.get(loopHead);
    } else {

      // Looping edges: intersection of forwards-reachable
      // and backwards-reachable.
      List<CFAEdge> edgesInLoop = getEdgesInSCC(loopHead);
      out = convert(edgesInLoop);

      // Otherwise it's not a loop.
      Preconditions.checkState(!edgesInLoop.isEmpty());
      boolean changed;
      do {
        shutdownNotifier.shutdownIfNecessary();
        changed = false;
        if (applyLBETransformation && applyLargeBlockEncodingTransformationPass(out)) {
          changed = true;
        }
      } while (changed);

      LBEcache.put(loopHead, out);
    }

    PathFormula empty = new PathFormula(
        fmgr.getBooleanFormulaManager().makeTrue(),
        start, pts, 0);
    EdgeWrapper outEdge;
    if (out.size() == 1) {
      outEdge = out.values().iterator().next();
    } else {
      outEdge = new OrEdge(ImmutableList.copyOf(out.values()));
    }
    return outEdge.toPathFormula(empty);
  }

  /**
   * Apply and- and or- LBE transformation,
   * return whether the passed table was changed.
   */
  private boolean applyLargeBlockEncodingTransformationPass(
      Table<CFANode, CFANode, EdgeWrapper> out) {

    // successor (row) -> predecessor (column) -> EdgeWrapper (value)
    for (Cell<CFANode, CFANode, EdgeWrapper> cell : out.cellSet()) {

      EdgeWrapper e = cell.getValue();
      CFANode predecessor = e.getPredecessor();

      // Do not perform reduction on edges which predecessor is a loop-head.
      if (loopStructure.getAllLoopHeads().contains(predecessor)) {
        continue;
      }
      if (e.getPredecessor().equals(e.getSuccessor())) {

        // Can not process self-looping edges.
        continue;
      }

      // Edges which successor node equal to the predecessor of the currently processed edge.
      Collection<EdgeWrapper> candidates = out.row(predecessor).values();

      if (candidates.size() >= 1) {
        out.remove(e.getSuccessor(), e.getPredecessor());
        logger.log(Level.ALL, "Removing", e);

        for (EdgeWrapper candidate : ImmutableList.copyOf(candidates)) {
          logger.log(Level.ALL, "Removing", candidate);
          out.remove(candidate.getSuccessor(), candidate.getPredecessor());

          EdgeWrapper added = new AndEdge(ImmutableList.of(candidate, e));

          // We need to check whether adding "added" would create a double entry.
          // We maintain invariant that there is always at most one alternative edge.
          EdgeWrapper alternative = out.get(added.getSuccessor(), added.getPredecessor());
          if (alternative != null) {
            added = new OrEdge(ImmutableList.of(added, alternative));

            logger.log(Level.ALL, "Removing", alternative);
            out.remove(alternative.getSuccessor(), alternative.getPredecessor());
          }

          logger.log(Level.ALL, "Adding", added);
          out.put(added.getSuccessor(), added.getPredecessor(), added);
        }

        // Terminate the iteration on first change.
        return true;
      }
    }
    return false;
  }

  private Table<CFANode, CFANode, EdgeWrapper> convert(Collection<CFAEdge> edges) {
    // successor -> predecessor -> EdgeWrapper.
    Table<CFANode, CFANode, EdgeWrapper> out = HashBasedTable.create();
    for (CFAEdge e : edges) {
      out.put(e.getSuccessor(), e.getPredecessor(), new SingleEdge(e));
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
        if (edge == first) {
          continue;
        }
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
