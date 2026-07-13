// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.MapMaker;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * Clones the entire CFA (nodes and edges) for a specific thread ID, renaming all variable
 * references with the thread-ID prefix using {@link PorAstCloner}. The cloning is performed lazily
 * on first access for each thread ID and then cached.
 *
 * <p>For {@link CFunctionEntryNode}s, the function declaration is also cloned so that parameter
 * declarations carry the renamed qualified names.
 */
final class PorCfaCloner {

  /**
   * Thread cloners, scoped to the CFA they were cloned from. The CFA key is essential: several
   * CPAchecker analyses can run in one JVM (the JUnit suites, and the restart/parallel algorithms),
   * and a cache keyed on the thread ID alone would hand the second analysis the thread clones of
   * the *first* analysis's program — silently verifying the wrong CFA. Keyed by identity (a CFA has
   * no value equality) and weakly, so a finished analysis's clones become collectable with it
   * rather than being retained for the life of the JVM.
   */
  private static final Map<CFA, Map<Integer, PorCfaCloner>> perCfaCache =
      new MapMaker().weakKeys().makeMap();

  /**
   * Reverse node map across all thread cloners: cloned node -> original node. Used for dependency
   * analysis which needs to traverse the original CFA. Keyed by the cloned node, which is unique to
   * one cloner, so entries of different analyses cannot collide; weak keys let them die with the
   * cloner that owns them.
   */
  private static final Map<CFANode, CFANode> globalReverseNodeMap =
      new MapMaker().weakKeys().makeMap();

  /**
   * Cloned node -> thread ID it was cloned for. Used by the witness export and validation to
   * recover which thread a cloned edge belongs to.
   */
  private static final Map<CFANode, Integer> globalNodeThreadIdMap =
      new MapMaker().weakKeys().makeMap();

  /**
   * Reverse edge map across all thread cloners: cloned edge -> original edge. Used for dependency
   * analysis which needs to extract variables from original edges.
   */
  private static final Map<CFAEdge, CFAEdge> globalReverseEdgeMap =
      new MapMaker().weakKeys().makeMap();

  /** Map from original node to cloned node. */
  private final IdentityHashMap<CFANode, CFANode> nodeMap = new IdentityHashMap<>();

  /** Map from original edge to cloned edge. */
  private final IdentityHashMap<CFAEdge, CFAEdge> edgeMap = new IdentityHashMap<>();

  /** Reverse map: cloned edge -> original edge, for detecting already-cloned edges. */
  private final IdentityHashMap<CFAEdge, CFAEdge> reverseEdgeMap = new IdentityHashMap<>();

  private final int threadId;
  private final PorAstCloner astCloner;

  private PorCfaCloner(int pThreadId) {
    threadId = pThreadId;
    astCloner = new PorAstCloner(pThreadId);
  }

  /**
   * Get or create the CFA clone of {@code pCfa} for the given thread ID. Cloning is performed once
   * per (CFA, thread ID) pair and then cached.
   */
  static PorCfaCloner getOrCreate(int pThreadId, CFA pCfa) {
    return perCfaCache
        .computeIfAbsent(pCfa, unused -> new HashMap<>())
        .computeIfAbsent(
            pThreadId,
            pid -> {
              PorCfaCloner cloner = new PorCfaCloner(pid);
              cloner.cloneEntireCfa(pCfa);
              return cloner;
            });
  }

  /**
   * Returns the cloned counterpart of the given node. If the node is an original CFA node, the
   * corresponding cloned node is returned. If it is already a cloned node (or unknown), it is
   * returned as-is.
   */
  CFANode getClonedNode(CFANode pOriginalNode) {
    CFANode cloned = nodeMap.get(pOriginalNode);
    return cloned != null ? cloned : pOriginalNode;
  }

  /**
   * Returns the cloned counterpart of the given edge. If the edge is an original CFA edge, the
   * corresponding cloned edge is returned. If it is already a cloned edge (or unknown), it is
   * returned as-is.
   */
  CFAEdge getClonedEdge(CFAEdge pOriginalEdge) {
    CFAEdge cloned = edgeMap.get(pOriginalEdge);
    return cloned != null ? cloned : pOriginalEdge;
  }

  /** Returns whether the given edge is a cloned edge produced by this cloner. */
  boolean isClonedEdge(CFAEdge pEdge) {
    return reverseEdgeMap.containsKey(pEdge);
  }

  /** Returns the thread ID this cloner is for. */
  int getThreadId() {
    return threadId;
  }

  /**
   * Returns the original CFA node corresponding to the given (potentially cloned) node. If the
   * node is not a cloned node, it is returned as-is.
   */
  static CFANode getOriginalNode(CFANode pNode) {
    CFANode original = globalReverseNodeMap.get(pNode);
    return original != null ? original : pNode;
  }

  /**
   * Returns the thread ID (PID) the given node was cloned for, or empty if the node is not a cloned
   * POR node.
   */
  static OptionalInt getThreadIdForNode(CFANode pNode) {
    Integer pid = globalNodeThreadIdMap.get(pNode);
    return pid != null ? OptionalInt.of(pid) : OptionalInt.empty();
  }

  /**
   * Returns the original CFA edge corresponding to the given (potentially cloned) edge. If the
   * edge is not a cloned edge, it is returned as-is.
   */
  static CFAEdge getOriginalEdge(CFAEdge pEdge) {
    CFAEdge original = globalReverseEdgeMap.get(pEdge);
    return original != null ? original : pEdge;
  }

  // ---- CFA cloning ----

  private void cloneEntireCfa(CFA pCfa) {
    // Phase 1: Clone all nodes (BFS from all function heads)
    cloneAllNodes(pCfa);
    // Phase 2: Wire FunctionEntryNode <-> FunctionExitNode references
    wireEntryExitNodes();
    // Phase 3: Clone all edges and wire them to cloned nodes
    cloneAllEdges();
  }

  private void cloneAllNodes(CFA pCfa) {
    // Use BFS from all function entry nodes to discover every reachable node
    Queue<CFANode> worklist = new ArrayDeque<>();
    for (CFANode node : pCfa.nodes()) {
      if (!nodeMap.containsKey(node)) {
        registerClonedNode(node);
        worklist.add(node);
      }
    }

    while (!worklist.isEmpty()) {
      CFANode current = worklist.poll();
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        CFANode succ = current.getLeavingEdge(i).getSuccessor();
        if (!nodeMap.containsKey(succ)) {
          registerClonedNode(succ);
          worklist.add(succ);
        }
      }
      for (int i = 0; i < current.getNumEnteringEdges(); i++) {
        CFANode pred = current.getEnteringEdge(i).getPredecessor();
        if (!nodeMap.containsKey(pred)) {
          registerClonedNode(pred);
          worklist.add(pred);
        }
      }
    }
  }

  /** Clones the given original node and registers it in all node maps. */
  private CFANode registerClonedNode(CFANode pOriginalNode) {
    CFANode cloned = cloneNode(pOriginalNode);
    nodeMap.put(pOriginalNode, cloned);
    globalReverseNodeMap.put(cloned, pOriginalNode);
    globalNodeThreadIdMap.put(cloned, threadId);
    return cloned;
  }

  @SuppressWarnings("deprecation") // FunctionExitNode.getLeavingEdges is deprecated but we need it
  private void cloneAllEdges() {
    IdentityHashMap<CFAEdge, Boolean> visited = new IdentityHashMap<>();

    // First pass: clone summary edges (they're referenced by function call and return edges)
    for (CFANode originalNode : nodeMap.keySet()) {
      FunctionSummaryEdge leavingSummary = originalNode.getLeavingSummaryEdge();
      if (leavingSummary != null && !visited.containsKey(leavingSummary)) {
        visited.put(leavingSummary, Boolean.TRUE);
        cloneAndRegisterEdge(leavingSummary);
      }
    }

    // Second pass: clone all regular edges
    IdentityHashMap<CFANode, Boolean> nodeVisited = new IdentityHashMap<>();
    Queue<CFANode> edgeWorklist = new ArrayDeque<>(nodeMap.keySet());
    while (!edgeWorklist.isEmpty()) {
      CFANode originalNode = edgeWorklist.poll();
      if (nodeVisited.containsKey(originalNode)) {
        continue;
      }
      nodeVisited.put(originalNode, Boolean.TRUE);

      // getLeavingEdges() for regular nodes, getLeavingReturnEdges() for FunctionExitNode
      Iterable<CFAEdge> leavingEdges;
      if (originalNode instanceof FunctionExitNode exitNode) {
        leavingEdges = exitNode.getLeavingEdges();
      } else {
        leavingEdges = originalNode.getLeavingEdges();
      }

      for (CFAEdge edge : leavingEdges) {
        if (!visited.containsKey(edge)) {
          visited.put(edge, Boolean.TRUE);
          cloneAndRegisterEdge(edge);
        }
      }
    }
  }

  private void cloneAndRegisterEdge(CFAEdge originalEdge) {
    CFANode clonedPred = nodeMap.get(originalEdge.getPredecessor());
    CFANode clonedSucc = nodeMap.get(originalEdge.getSuccessor());
    if (clonedPred == null || clonedSucc == null) {
      // Edge references nodes we haven't cloned (shouldn't happen in normal CFA)
      return;
    }

    CFAEdge clonedEdge = cloneEdge(originalEdge, clonedPred, clonedSucc);
    edgeMap.put(originalEdge, clonedEdge);
    reverseEdgeMap.put(clonedEdge, originalEdge);
    globalReverseEdgeMap.put(clonedEdge, originalEdge);

    // Register the cloned edge with its cloned nodes
    if (clonedEdge instanceof FunctionSummaryEdge summaryEdge) {
      clonedPred.addLeavingSummaryEdge(summaryEdge);
      clonedSucc.addEnteringSummaryEdge(summaryEdge);
    } else {
      clonedPred.addLeavingEdge(clonedEdge);
      clonedSucc.addEnteringEdge(clonedEdge);
    }
  }

  // ---- Node cloning ----

  @SuppressWarnings("unchecked")
  private CFANode cloneNode(CFANode originalNode) {
    if (originalNode instanceof CFunctionEntryNode entryNode) {
      // Clone the function declaration with renamed parameters
      CFunctionDeclaration clonedFuncDecl = astCloner.cloneAst(entryNode.getFunctionDefinition());

      // Clone the return variable if present
      Optional<CVariableDeclaration> clonedReturnVar =
          entryNode
              .getReturnVariable()
              .map(retVar -> astCloner.cloneAstLeftSide(retVar));

      // We need to create exit node first if it exists, but we'll wire it later via the map
      // For now, pass null for exit node - we'll set it afterward
      CFunctionEntryNode clonedEntry =
          new CFunctionEntryNode(
              entryNode.getFileLocation(), clonedFuncDecl, null, clonedReturnVar);

      if (entryNode.isLoopStart()) {
        clonedEntry.setLoopStart();
      }

      return clonedEntry;

    } else if (originalNode instanceof FunctionExitNode exitNode) {
      FunctionExitNode clonedExit = new FunctionExitNode(exitNode.getFunction());

      if (exitNode.isLoopStart()) {
        clonedExit.setLoopStart();
      }

      return clonedExit;

    } else if (originalNode instanceof CFATerminationNode termNode) {
      CFATerminationNode clonedTerm = new CFATerminationNode(termNode.getFunction());
      // CFATerminationNode never has leaving edges, no need to set loop start
      return clonedTerm;

    } else if (originalNode instanceof CFALabelNode labelNode) {
      CFALabelNode clonedLabel = new CFALabelNode(labelNode.getFunction(), labelNode.getLabel());

      if (labelNode.isLoopStart()) {
        clonedLabel.setLoopStart();
      }

      return clonedLabel;

    } else {
      // Plain CFANode
      CFANode clonedNode = new CFANode(originalNode.getFunction());

      if (originalNode.isLoopStart()) {
        clonedNode.setLoopStart();
      }

      return clonedNode;
    }
  }

  /**
   * After all nodes are created, wire up FunctionEntryNode ↔ FunctionExitNode references.
   * This is called at the end of cloneAllNodes but we handle it during cloneAllEdges
   * since we need all nodes to be created first.
   */
  private void wireEntryExitNodes() {
    for (Map.Entry<CFANode, CFANode> entry : nodeMap.entrySet()) {
      CFANode original = entry.getKey();
      CFANode cloned = entry.getValue();

      if (original instanceof CFunctionEntryNode origEntry && cloned instanceof CFunctionEntryNode clonedEntry) {
        origEntry
            .getExitNode()
            .ifPresent(
                origExit -> {
                  CFANode clonedExit = nodeMap.get(origExit);
                  if (clonedExit instanceof FunctionExitNode clonedFuncExit) {
                    clonedFuncExit.setEntryNode(clonedEntry);
                  }
                });
      }
    }
  }

  // ---- Edge cloning ----

  private CFAEdge cloneEdge(CFAEdge originalEdge, CFANode clonedPred, CFANode clonedSucc) {
    FileLocation loc = originalEdge.getFileLocation();
    String rawStatement = originalEdge.getRawStatement();

    return switch (originalEdge.getEdgeType()) {
      case BlankEdge -> cloneBlankEdge(originalEdge, clonedPred, clonedSucc);
      case AssumeEdge -> cloneAssumeEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
      case StatementEdge ->
          cloneStatementEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
      case DeclarationEdge ->
          cloneDeclarationEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
      case ReturnStatementEdge ->
          cloneReturnStatementEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
      case FunctionCallEdge ->
          cloneFunctionCallEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
      case FunctionReturnEdge ->
          cloneFunctionReturnEdge(originalEdge, loc, clonedPred, clonedSucc);
      case CallToReturnEdge ->
          cloneCallToReturnEdge(originalEdge, rawStatement, loc, clonedPred, clonedSucc);
    };
  }

  private CFAEdge cloneBlankEdge(CFAEdge edge, CFANode pred, CFANode succ) {
    return new BlankEdge(
        edge.getRawStatement(), edge.getFileLocation(), pred, succ, edge.getDescription());
  }

  private CFAEdge cloneAssumeEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CAssumeEdge cEdge) {
      return new CAssumeEdge(
          rawStatement,
          loc,
          pred,
          succ,
          astCloner.cloneAstRightSide(cEdge.getExpression()),
          cEdge.getTruthAssumption(),
          cEdge.isSwapped(),
          cEdge.isArtificialIntermediate());
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneStatementEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CFunctionSummaryStatementEdge cEdge) {
      return new CFunctionSummaryStatementEdge(
          rawStatement,
          astCloner.cloneAst(cEdge.getStatement()),
          loc,
          pred,
          succ,
          astCloner.cloneAst(cEdge.getFunctionCall()),
          cEdge.getFunctionName());
    } else if (edge instanceof CStatementEdge cEdge) {
      return new CStatementEdge(
          rawStatement, astCloner.cloneAst(cEdge.getStatement()), loc, pred, succ);
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneDeclarationEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CDeclarationEdge cEdge) {
      return new CDeclarationEdge(
          rawStatement, loc, pred, succ, astCloner.cloneAstLeftSide(cEdge.getDeclaration()));
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneReturnStatementEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CReturnStatementEdge cEdge) {
      assert succ instanceof FunctionExitNode
          : "Expected FunctionExitNode successor: " + succ + ", " + succ.getClass();
      return new CReturnStatementEdge(
          rawStatement,
          astCloner.cloneAst(cEdge.getReturnStatement()),
          loc,
          pred,
          (FunctionExitNode) succ);
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneFunctionCallEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CFunctionCallEdge cEdge) {
      assert succ instanceof CFunctionEntryNode
          : "Expected CFunctionEntryNode successor: " + succ + ", " + succ.getClass();
      // Look up the already-cloned summary edge
      CFunctionSummaryEdge originalSummary = cEdge.getSummaryEdge();
      CFAEdge clonedSummary = edgeMap.get(originalSummary);
      assert clonedSummary instanceof CFunctionSummaryEdge
          : "Summary edge must be cloned before function call edge";
      return new CFunctionCallEdge(
          rawStatement,
          loc,
          pred,
          (CFunctionEntryNode) succ,
          astCloner.cloneAst((CFunctionCall) cEdge.getRawAST().orElseThrow()),
          (CFunctionSummaryEdge) clonedSummary);
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneFunctionReturnEdge(
      CFAEdge edge, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CFunctionReturnEdge cEdge) {
      assert pred instanceof FunctionExitNode
          : "Expected FunctionExitNode predecessor: " + pred + ", " + pred.getClass();
      // Look up the already-cloned summary edge
      CFunctionSummaryEdge originalSummary = cEdge.getSummaryEdge();
      CFAEdge clonedSummary = edgeMap.get(originalSummary);
      assert clonedSummary instanceof CFunctionSummaryEdge
          : "Summary edge must be cloned before function return edge";
      return new CFunctionReturnEdge(
          loc, (FunctionExitNode) pred, succ, (CFunctionSummaryEdge) clonedSummary);
    }
    throw new AssertionError("only C supported");
  }

  private CFAEdge cloneCallToReturnEdge(
      CFAEdge edge, String rawStatement, FileLocation loc, CFANode pred, CFANode succ) {
    if (edge instanceof CFunctionSummaryEdge cEdge) {
      // Look up the cloned function entry node
      CFANode clonedEntry = nodeMap.get(cEdge.getFunctionEntry());
      assert clonedEntry instanceof CFunctionEntryNode
          : "Function entry node must be cloned: " + cEdge.getFunctionEntry();
      return new CFunctionSummaryEdge(
          rawStatement,
          loc,
          pred,
          succ,
          astCloner.cloneAst(cEdge.getExpression()),
          (CFunctionEntryNode) clonedEntry);
    }
    throw new AssertionError("only C supported");
  }
}
