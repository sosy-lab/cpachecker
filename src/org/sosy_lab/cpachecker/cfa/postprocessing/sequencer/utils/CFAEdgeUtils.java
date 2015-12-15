package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.ThreadScheduleEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * The CFAEdge utilities are able to make useful edge manipulations.
 *
 * <p>
 * Caveat: Due to the immortality of CFAEdges the use of this utilities can
 * cause a replacement of an CFAEdge by a new created one. This is especially the case if change of
 * the nodes in an CFAEdge is involved. This should be considered when using the
 * utilities.
 * </p>
 *
 */
public class CFAEdgeUtils {
  public final static CFANode DUMMY_NODE = CFASequenceBuilder.DUMMY_NODE;

  /**
   * This collection is for assertion purpose only. Edges which get new
   * successor or predecessor nodes have to be new created due to immortality
   * of CFAEdges. Every original copy of edges which should not be used anymore
   * will be stored in this list;
   */
  public static Collection<CFAEdge> trashCan = new HashSet<CFAEdge>();

  /**
   * <p>
   * This method bypasses the predecessor and successor nodes of the given edge.
   * Due to immortality of CFAEdges a copy of the given edge with the new
   * predecessor and successor will be created. The old edge will be erased from
   * the connected nodes.
   * </p>
   *
   * <p>
   * Similar to {@link #copyCEdge} but removes the successor and predecessor
   * CFANodes from original CFAEdge instead.
   * </p>
   *
   * <p>
   * Caveat: The given edge is no edge of the cfa anymore. It was replaced by a
   * new one. Attention should be paid if the edge is stored in any object aside
   * from the cfa.
   *
   * Use asserts with {@link #isEdgeForbiddenEdge} to verify your edge
   * consistency.
   * </p>
   *
   * @param edge
   *          the edge which will be bypassed
   * @param predecessor
   *          the new predecessor of the edge
   * @param successor
   *          the new successor of the edge
   * @return the bypassed edge. It is a new instance of the given edge which was
   *         created because of immortality of CFAEdge
   */
  public static CFAEdge bypassCEdgeNodes(CFAEdge edge, CFANode predecessor, CFANode successor) {
    CFAEdge newEdge = buildNewCEdges(edge, predecessor, successor);

    // remove original nodes
    CFANode originalPredecessor = edge.getPredecessor();
    CFANode originalSuccessor = edge.getSuccessor();

    if(CFAUtils.leavingEdges(originalPredecessor).contains(edge)) {
      originalPredecessor.removeLeavingEdge(edge);
    }

    if(CFAUtils.enteringEdges(originalSuccessor).contains(edge)) {
      originalSuccessor.removeEnteringEdge(edge);
    }

    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);

    trashCan.add(edge);
    return newEdge;
  }


  /**
   * <p>
   * This method copies an edge to an edge with the given predecessor and
   * successor nodes. It does not affect the original edge. In particular the
   * predecessor and successor node mustn't match with the predecessor and
   * successor of the original edge.
   * </p>
   *
   * <p>
   * Similar to {@link #bypassCEdgeNodes} but does not affect the original edge.
   * </p>
   * @param edge
   *          the edge which will be copied
   * @param predecessor
   *          the new predecessor of the copied edge
   * @param successor
   *          the new successor of the copied edge
   * @return the instance of the copied CFAEdge
   */
  public static CFAEdge copyCEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(areEdgesNewEdges(edge, predecessor, successor));
    CFAEdge newEdge = buildNewCEdges(edge, predecessor, successor);
    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
    return newEdge;
  }


  private static boolean areEdgesNewEdges(CFAEdge edge, CFANode predecessor, CFANode successor) {
    CFANode originalPredecessor = edge.getPredecessor();
    CFANode originalSuccessor = edge.getSuccessor();

    if(originalPredecessor.equals(DUMMY_NODE) && originalSuccessor.equals(DUMMY_NODE)) {
      return true;
    } else if(originalPredecessor.equals(DUMMY_NODE)) {
      return !originalSuccessor.equals(successor);
    } else if(originalSuccessor.equals(DUMMY_NODE)) {
      return !originalPredecessor.equals(predecessor);
    } else {
      return !originalSuccessor.equals(successor) && !originalPredecessor.equals(predecessor);
    }
  }

  /**
   * Gets an new instance of a AssumeEdge with an inverted AssumeExpression.
   *
   * @param assumeEdge
   *          - the edge prototype
   * @return a new instance of the given assumeEdge with inverted
   *         AssumeExpression
   */
  public static AssumeEdge getAssumeEdgeInvertedExpression(AssumeEdge assumeEdge) {
    if(assumeEdge instanceof CAssumeEdge) {
      CAssumeEdge cAssumeEdge = (CAssumeEdge) assumeEdge;
      return new CAssumeEdge(cAssumeEdge.getRawStatement(), cAssumeEdge.getFileLocation(), cAssumeEdge.getPredecessor(), cAssumeEdge.getSuccessor(), cAssumeEdge.getExpression(), !cAssumeEdge.getTruthAssumption());
    } else if(assumeEdge instanceof JAssumeEdge) {
      JAssumeEdge jAssumeEdge = (JAssumeEdge) assumeEdge;
      return new JAssumeEdge(jAssumeEdge.getRawStatement(), jAssumeEdge.getFileLocation(), jAssumeEdge.getPredecessor(), jAssumeEdge.getSuccessor(), jAssumeEdge.getExpression(), !jAssumeEdge.getTruthAssumption());
    }
    return null;
  }

  /**
   * <p>
   * Replaces an edge by an new edge. The new edge will be a copied and placed
   * between the predecessor and successor node of the original edge.
   * </p>
   *
   * <p>
   * The replaced edge will be removed from cfa and its nodes.
   * Use asserts with {@link #isEdgeForbiddenEdge} to verify your edge
   * consistency.
   * </p>
   * @param originalEdge
   *          the original edge which will be removed and replaced at the same
   *          location by a new edge which equals the new edge exemplar
   * @param newEdgeExemplar
   *          the pattern of the new edge which will be placed at the position
   *          of the original edge
   * @return the new edge which was inserted to the cfa. Note: the new
   *         EdgeExemplar was copied due to the immortality of the cfa edge to
   *         map the new successor and predecessor
   */
  public static CFAEdge replaceCEdgeWith(CFAEdge originalEdge, CFAEdge newEdgeExemplar) {
    CFANode predecessor = originalEdge.getPredecessor();
    CFANode successor = originalEdge.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(originalEdge);
    CFAEdge newEdge = bypassCEdgeNodes(newEdgeExemplar, predecessor, successor);

    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdgeExemplar);

    trashCan.add(originalEdge);
    return newEdge;
  }

  /**
   * This will inject a cfa sequence between the given node and its successor
   * edges. The edges will be wired directly to the assigned successors and the
   * original association at inject location will be drained. This means the
   * wiring will not be done with an Blank/Glue edge. SummaryEdges will be
   * ignored.
   *
   *
   * <p>
   * The sequence have to fulfill the following restrictions
   * <ul>
   * <li>The given end of the sequence musn't have leaving edges</li>
   * <li>The given end node of the sequence must be reachable by the given start
   * node</li>
   * </ul>
   * </p>
   *
   * <p>
   * Caveat: Due to immortality of CFAEdges some edges must be replaced to map
   * the new successor and predecessor nodes. Note that the original edges will
   * still exists with the original CFANotes which are still used in the cfa
   * stored in them.<br />
   * The successor edges of the node injectBeforeNode and the successor edges of
   * the cfa sequence will be replaced by new edges.
   *
   * Use asserts with {@link #isEdgeForbiddenEdge} to verify your edge
   * consistency.
   * </p>
   *
   * Note: the nodes of the new sequence will not be added to the cfa. However
   * the cfa is needed to delete replaced nodes from the cfa.
   *
   * @param injectBeforeNode
   *          - this node determines location in the cfa where the new cfa
   *          sequence will be injected. The location will be between the given
   *          node an its successor edges
   * @param injectedSequenceStart
   *          - the start node of the new injected cfa sequence
   * @param injectedSequenceEnd
   *          - the end node of the new injected cfa sequence. This node will no
   *          longer exist in the cfa
   * @param cfa
   *          - the cfa where the original edge is in. Replaced nodes will be
   *          deleted from the cfa
   * @return a pair of a set of edges. The first value is a set of all replaced
   *         edges from the front of the injection. The second value is a set of
   *         all replaced edges from the end of the injection
   */
  public static Pair<Set<CFAEdge>, Set<CFAEdge>> injectInBetween(CFANode injectBeforeNode, CFANode injectedSequenceStart, CFANode injectedSequenceEnd, MutableCFA cfa) {
    assert isNodeReachableByNode(injectedSequenceEnd, injectedSequenceStart);
    assert injectedSequenceEnd.getNumLeavingEdges() == 0;
    HashSet<CFAEdge> newStartEdge = new HashSet<CFAEdge>();
    HashSet<CFAEdge> newEndEdge = new HashSet<CFAEdge>();

    for(CFAEdge originalEnteringEdges : CFAUtils.enteringEdges(injectBeforeNode)) {
      CFAEdge newEdge = bypassCEdgeNodes(originalEnteringEdges, originalEnteringEdges.getPredecessor(), injectedSequenceStart);
      newStartEdge.add(newEdge);
    }

    for(CFAEdge sequenceEnteringEdges : CFAUtils.enteringEdges(injectedSequenceEnd)) {
      CFAEdge newEdge = bypassCEdgeNodes(sequenceEnteringEdges, sequenceEnteringEdges.getPredecessor(), injectBeforeNode);
      newEndEdge.add(newEdge);
    }

    cfa.removeNode(injectedSequenceEnd);

    return Pair.<Set<CFAEdge>, Set<CFAEdge>>of(newStartEdge, newEndEdge);
  }

  public static boolean isNodeReachableByNode(CFANode node, CFANode reachableBy) {
    NodeReachable reachableChecker = new NodeReachable(node);
    CFATraversal.dfs().traverseOnce(reachableBy, reachableChecker);
    return reachableChecker.isReachable();
  }


  public static CFAEdge bypassJavaEdgeNodes(CFAEdge edge, CFANode predecessor, CFANode successor) {
    // TODO implement
    throw new RuntimeException("java functions are not implemented yet!");
  }


  public static void removeLeavingEdges(CFANode node) {
    checkNotNull(node);
    for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
      node.removeLeavingEdge(edge);
    }
  }

  public static void removeEnteringEdges(CFANode node) {
    checkNotNull(node);
    for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
      node.removeEnteringEdge(edge);
    }
  }


  public static class NodeReachable extends CFATraversal.DefaultCFAVisitor {

    private boolean isReachable = false;
    private CFANode target;

    public NodeReachable(CFANode toCheckNode) {
      assert toCheckNode != null;
      target = toCheckNode;
    }

    @Override
    public TraversalProcess visitNode(CFANode edge) {
      if (target.equals(edge)) {
        isReachable = true;
        return TraversalProcess.ABORT;
      }
      return TraversalProcess.CONTINUE;
    }

    public boolean isReachable() {
      return isReachable;
    }
  }

  /**
   * <p>
   * Checks if any edge given in the collection will match to a forbidden edge.
   * A forbidden edge is a CFAEdge which was replaced from CFAEdgeUtils by
   * another to change its entity. This edge is most likely no valid edge in the
   * cfa
   * </p>
   *
   * @see CFAEdgeUtils#bypassCEdgeNodes
   * @see CFAEdgeUtils#injectInBetween
   * @see CFAEdgeUtils#replaceCEdgeWith
   */
  public static boolean isEdgeForbiddenEdge(Collection<CFAEdge> edges) {
    for(CFAEdge edge : edges) {
      if(isEdgeForbiddenEdge(edge)) {
        return true;
      }
    }
    return false;
  }
  public static boolean isEdgeForbiddenEdge(CFAEdge edge) {
    return trashCan.contains(edge);
  }

  /**
   * Return an {@link java.lang.Iterable} that contains the leaving edges of a given CFANode,
   * excluding the summary edge. Similar to
   * {@link CFAUtils#leavingEdges(CFANode)} but returns a fixed Iterable
   * which means there are not side-effects if the edge are deleted during
   * iteration. (especially needed for {@link #bypassCEdgeNodes})
   */
  public static Iterable<CFAEdge> getLeavingEdges(CFANode node) {
    Builder<CFAEdge> builder = ImmutableSet.<CFAEdge>builder();
    for(int i = 0; i < node.getNumLeavingEdges(); i++) {
      builder.add(node.getLeavingEdge(i));
    }
    return builder.build();
  }

  /**
   * Return an {@link java.lang.Iterable} that contains all leaving edges of a given
   * CFANode, including the summary edge if the node as one. Similar to
   * {@link CFAUtils#allLeavingEdges(CFANode)} but returns a fixed Iterable
   * which means there are not side-effects if the edge are deleted during
   * iteration. (especially needed for {@link #bypassCEdgeNodes})
   */
  public static Iterable<CFAEdge> getAllLeavingEdges(CFANode node) {
    Builder<CFAEdge> builder = ImmutableSet.<CFAEdge>builder();
    if(node.getLeavingSummaryEdge() != null) {
      builder.add(node.getLeavingSummaryEdge());
    }
    builder.addAll(getLeavingEdges(node));
    return builder.build();
  }

  /**
   * Return an {@link java.lang.Iterable} that contains the entering edges of a given CFANode,
   * excluding the summary edge. Similar to
   * {@link CFAUtils#enteringEdges(CFANode)} but returns a fixed Iterable
   * which means there are not side-effects if the edge are deleted during
   * iteration. (especially needed for {@link #bypassCEdgeNodes})
   */
  public static Iterable<CFAEdge> getEnteringEdges(CFANode node) {
    Builder<CFAEdge> builder = ImmutableSet.<CFAEdge>builder();
    for(int i = 0; i < node.getNumEnteringEdges(); i++) {
      builder.add(node.getEnteringEdge(i));
    }
    return builder.build();
  }

  /**
   * Return an {@link java.lang.Iterable} that contains all entering edges of a given CFANode,
   * including the summary edge if the node as one. Similar to
   * {@link CFAUtils#allEnteringEdges(CFANode)} but returns a fixed Iterable
   * which means there are not side-effects if the edge are deleted during
   * iteration. (especially needed for {@link #bypassCEdgeNodes})
   */
  public static Iterable<CFAEdge> getAllEnteringEdges(CFANode node) {
    Builder<CFAEdge> builder = ImmutableSet.<CFAEdge>builder();
    if(node.getEnteringSummaryEdge() != null) {
      builder.add(node.getEnteringSummaryEdge());
    }
    builder.addAll(getEnteringEdges(node));
    return builder.build();
  }

  private static CFAEdge buildNewCEdges(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkNotNull(edge);

    CFAEdge newEdge;
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      newEdge = buildCAssumeEdge(edge, predecessor, successor);
      break;
    case BlankEdge:
      newEdge = buildBlankEdge(edge, predecessor, successor);
      break;
    case CallToReturnEdge:
      newEdge = buildCCallToReturnEdge(edge, predecessor, successor);
      break;
    case DeclarationEdge:
      newEdge = buildCDeclarationEdge(edge, predecessor, successor);
      break;
    case FunctionCallEdge:
      newEdge = buildFunctionCallEdge(edge, predecessor, successor);
      break;
    case FunctionReturnEdge:
      newEdge = buildCFunctionReturnEdge(edge, predecessor, successor);
      break;
    case MultiEdge:
      newEdge = buildMultiEdge(edge, predecessor, successor);
      break;
    case ReturnStatementEdge:
      newEdge = buildCReturnStatementEdge(edge, predecessor, successor);
      break;
    case StatementEdge:
      newEdge = buildCStatementEdge(edge, predecessor, successor);
      break;
    case ContextSwtichEdge:
      newEdge = buildAContextSwitchEdge(edge, predecessor, successor);
      break;
    case ThreadScheduleEdge:
      newEdge = buildThreadScheduleEdge(edge, predecessor, successor);
      break;
    default:
      throw new IllegalArgumentException("Cannot clone edge without edge type: " + edge.getEdgeType());
    }
    return newEdge;
  }


  private static CFAEdge buildCStatementEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
      Preconditions.checkArgument(edge instanceof CStatementEdge);
      CStatementEdge oldCStatementEdge = (CStatementEdge) edge;
      return new CStatementEdge(oldCStatementEdge.getRawStatement(), oldCStatementEdge.getStatement(), oldCStatementEdge.getFileLocation(),
          predecessor, successor);
  }

  private static CFAEdge buildCReturnStatementEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof CReturnStatementEdge);
    CReturnStatementEdge oldCReturnStatementEdge = (CReturnStatementEdge) edge;
    Preconditions.checkArgument(oldCReturnStatementEdge.getRawAST().isPresent());
    Preconditions.checkArgument(successor instanceof FunctionExitNode);

    return new CReturnStatementEdge(oldCReturnStatementEdge.getRawStatement(), oldCReturnStatementEdge.getRawAST().get(),
        oldCReturnStatementEdge.getFileLocation(), predecessor, (FunctionExitNode) successor);
  }

  private static CFAEdge buildMultiEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof MultiEdge);
    MultiEdge oldMultiEdge = (MultiEdge) edge;

    return new MultiEdge(predecessor, successor, oldMultiEdge.getEdges());
  }

  private static CFAEdge buildCFunctionReturnEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(successor instanceof FunctionExitNode);
    Preconditions.checkArgument(edge instanceof CReturnStatementEdge);
    Preconditions.checkArgument(edge.getRawAST().isPresent());
    FunctionExitNode functionExitNode = (FunctionExitNode) successor;
    CReturnStatementEdge originalEdge = (CReturnStatementEdge) edge;

    return new CReturnStatementEdge(edge.getRawStatement(), originalEdge.getRawAST().get(), edge.getFileLocation(), predecessor, functionExitNode);
  }

  private static CFAEdge buildFunctionCallEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof CFunctionCallEdge);
    CFunctionCallEdge oldFunctionCallEdge = (CFunctionCallEdge) edge;
    Preconditions.checkArgument(oldFunctionCallEdge.getRawAST().isPresent());

    Preconditions.checkArgument(successor instanceof CFunctionEntryNode);

    return new CFunctionCallEdge(edge.getRawStatement(), edge.getFileLocation(), predecessor, (CFunctionEntryNode) successor,
        oldFunctionCallEdge.getRawAST().get(), oldFunctionCallEdge.getSummaryEdge());
  }

  private static CFAEdge buildCDeclarationEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof CDeclarationEdge);
    CDeclarationEdge oldDeclarationEdge = (CDeclarationEdge) edge;

    return new CDeclarationEdge(oldDeclarationEdge.getRawStatement(), oldDeclarationEdge.getFileLocation(), predecessor, successor,
        oldDeclarationEdge.getDeclaration());
  }

  private static CFAEdge buildCCallToReturnEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof CFunctionSummaryEdge);
    CFunctionSummaryEdge oldCFunctionSummaryEdge = (CFunctionSummaryEdge) edge;

    return new CFunctionSummaryEdge(oldCFunctionSummaryEdge.getRawStatement(), oldCFunctionSummaryEdge.getFileLocation(), predecessor,
        successor, oldCFunctionSummaryEdge.getExpression(), oldCFunctionSummaryEdge.getFunctionEntry());

  }

  private static CFAEdge buildBlankEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof BlankEdge);
    BlankEdge oldBlankEdge = (BlankEdge) edge;

    return new BlankEdge(oldBlankEdge.getRawStatement(), oldBlankEdge.getFileLocation(), predecessor, successor,
        oldBlankEdge.getDescription());
  }

  private static AssumeEdge buildCAssumeEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof AssumeEdge);
    AssumeEdge oldAssumeEdge = (AssumeEdge) edge;

    Preconditions.checkArgument(oldAssumeEdge.getExpression() instanceof CExpression);

    return new CAssumeEdge(oldAssumeEdge.getRawStatement(), oldAssumeEdge.getFileLocation(), predecessor, successor,
        (CExpression) oldAssumeEdge.getExpression(), oldAssumeEdge.getTruthAssumption());
  }

  private static ContextSwitchEdge buildAContextSwitchEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof ContextSwitchEdge);
    ContextSwitchEdge oldContextSwitchEdge = (ContextSwitchEdge) edge;

    Preconditions.checkNotNull(oldContextSwitchEdge.getContextSwitch());

    return new ContextSwitchEdge(oldContextSwitchEdge.getContextSwitch(), edge.getRawStatement(), edge.getFileLocation(), predecessor, successor, oldContextSwitchEdge.isToScheduler());
  }

  private static CFAEdge buildThreadScheduleEdge(CFAEdge edge, CFANode predecessor, CFANode successor) {
    Preconditions.checkArgument(edge instanceof ThreadScheduleEdge);
    ThreadScheduleEdge oldThreadScheduleEdge = (ThreadScheduleEdge) edge;
    AThread thread = oldThreadScheduleEdge.getThreadContext();

    return new ThreadScheduleEdge(predecessor, successor, thread);
  }

}
