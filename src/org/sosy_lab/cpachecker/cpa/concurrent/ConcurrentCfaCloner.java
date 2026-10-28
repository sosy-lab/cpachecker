// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaClonerData;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * Clones the entire CFA (nodes and edges) for a specific thread ID, renaming all variable
 * references with the thread-ID prefix using {@link ConcurrentAstCloner}. The cloning is performed
 * lazily on first access for each thread ID and then cached.
 *
 * <p>For {@link CFunctionEntryNode}s, the function declaration is also cloned so that parameter
 * declarations carry the renamed qualified names.
 */
final class ConcurrentCfaCloner {

  /**
   * Thread cloners, scoped to the CFA they were cloned from. The CFA key is essential: several
   * CPAchecker analyses can run in one JVM (the JUnit suites, and the restart/parallel algorithms),
   * and a cache keyed on the thread ID alone would hand the second analysis the thread clones of
   * the *first* analysis's program — silently verifying the wrong CFA. Keyed by identity (a CFA has
   * no value equality) and weakly, so a finished analysis's clones become collectable with it
   * rather than being retained for the life of the JVM.
   */
  private static final Map<CFA, Map<Integer, ConcurrentCfaCloner>> perCfaCache =
      new MapMaker().weakKeys().makeMap();

  /** Map from original node to cloned node. */
  private final IdentityHashMap<CFANode, CFANode> nodeMap = new IdentityHashMap<>();

  /** Map from original edge to cloned edge. */
  private final IdentityHashMap<CFAEdge, CFAEdge> edgeMap = new IdentityHashMap<>();

  private final CfaClonerData clonerData = new CfaClonerData();

  private final int threadId;
  private final ConcurrentAstCloner astCloner;

  private ConcurrentCfaCloner(int pThreadId) {
    this(pThreadId, true);
  }

  private ConcurrentCfaCloner(int pThreadId, boolean pUseThreadSpecificNames) {
    threadId = pThreadId;
    astCloner = new ConcurrentAstCloner(pThreadId, pUseThreadSpecificNames);
  }

  /**
   * Get or create the CFA clone of {@code pCfa} for the given thread ID. Cloning is performed once
   * per (CFA, thread ID) pair and then cached.
   */
  static ConcurrentCfaCloner getOrCreate(int pThreadId, CFA pCfa) {
    return perCfaCache
        .computeIfAbsent(pCfa, unused -> new HashMap<>())
        .computeIfAbsent(
            pThreadId,
            threadId -> {
              ConcurrentCfaCloner cloner = new ConcurrentCfaCloner(threadId);
              cloner.cloneEntireCfa(pCfa);
              return cloner;
            });
  }

  /** Creates a standalone structural clone of a CFA for project-wide compatibility tests. */
  static ImmutableCFA cloneForTesting(ImmutableCFA pCfa) {
    checkState(pCfa.getLanguage() == Language.C, "only C supported");
    ConcurrentCfaCloner cloner = new ConcurrentCfaCloner(0, false);
    cloner.cloneEntireCfa(pCfa);

    TreeMap<String, FunctionEntryNode> functions = new TreeMap<>();
    TreeMultimap<String, CFANode> nodes = TreeMultimap.create();
    cloner
        .nodeMap
        .values()
        .forEach(
            node -> {
              nodes.put(node.getFunctionName(), node);
              if (node instanceof FunctionEntryNode entry) {
                functions.put(entry.getFunctionName(), entry);
              }
            });

    var mainFunction = functions.get(pCfa.getMainFunction().getFunctionName());
    var metadata = pCfa.getMetadata().withMainFunctionEntry(mainFunction);
    if (pCfa.getLiveVariables().isPresent()) {
      metadata =
          metadata.withLiveVariables(
              pCfa.getLiveVariables().orElseThrow().copyWithNodeMapping(cloner.nodeMap));
    }
    MutableCFA clonedCfa = new MutableCFA(functions, nodes, metadata);
    if (pCfa.getLoopStructure().isPresent()) {
      ImmutableListMultimap.Builder<String, Loop> clonedLoops = ImmutableListMultimap.builder();
      for (String functionName : pCfa.getAllFunctionNames()) {
        for (Loop loop : pCfa.getLoopStructure().orElseThrow().getLoopsForFunction(functionName)) {
          clonedLoops.put(
              functionName,
              Loop.fromLoopHeadsAndNodes(
                  loop.getLoopHeads().stream()
                      .map(cloner.nodeMap::get)
                      .collect(ImmutableSet.toImmutableSet()),
                  loop.getLoopNodes().stream()
                      .map(cloner.nodeMap::get)
                      .collect(ImmutableSet.toImmutableSet())));
        }
      }
      clonedCfa.setLoopStructure(LoopStructure.of(clonedLoops.build()));
    }
    return clonedCfa.immutableCopy();
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

  /** Returns the thread ID this cloner is for. */
  int getThreadId() {
    return threadId;
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
    // Function entries need their exit node in their constructor. Clone exits first so the
    // corresponding clone can be passed while cloning each entry node.
    for (CFANode node : pCfa.nodes()) {
      if (node instanceof FunctionExitNode && !nodeMap.containsKey(node)) {
        registerClonedNode(node);
      }
    }

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
    cloned.setReversePostorderId(pOriginalNode.getReversePostorderId());
    if (!pOriginalNode.getOutOfScopeVariables().isEmpty()) {
      cloned.addOutOfScopeVariables(
          pOriginalNode.getOutOfScopeVariables().stream().map(astCloner::cloneAst).toList());
    }
    nodeMap.put(pOriginalNode, cloned);
    clonerData.registerNode(cloned, pOriginalNode, threadId);
    cloned.setClonerData(clonerData);
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
    clonerData.registerEdge(clonedEdge, originalEdge);
    checkState(clonedEdge instanceof AbstractCFAEdge);
    ((AbstractCFAEdge) clonedEdge).setClonerData(clonerData);

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
    return switch (originalNode) {
      case CFunctionEntryNode entryNode -> {
        // Clone the function declaration with renamed parameters
        CFunctionDeclaration clonedFuncDecl = astCloner.cloneAst(entryNode.getFunctionDefinition());

        // Clone the return variable if present
        Optional<CVariableDeclaration> clonedReturnVar =
            entryNode.getReturnVariable().map(retVar -> astCloner.cloneAstLeftSide(retVar));

        FunctionExitNode clonedExitNode =
            entryNode
                .getExitNode()
                .map(nodeMap::get)
                .map(FunctionExitNode.class::cast)
                .orElse(null);
        CFunctionEntryNode clonedEntry =
            new CFunctionEntryNode(
                entryNode.getFileLocation(), clonedFuncDecl, clonedExitNode, clonedReturnVar);

        if (entryNode.isLoopStart()) {
          clonedEntry.setLoopStart();
        }

        yield clonedEntry;
      }
      case FunctionExitNode exitNode -> {
        FunctionExitNode clonedExit = new FunctionExitNode(exitNode.getFunction());

        if (exitNode.isLoopStart()) {
          clonedExit.setLoopStart();
        }

        yield clonedExit;
      }
      // CFATerminationNode never has leaving edges, no need to set loop start
      case CFATerminationNode termNode -> new CFATerminationNode(termNode.getFunction());
      case CFALabelNode labelNode -> {
        CFALabelNode clonedLabel = new CFALabelNode(labelNode.getFunction(), labelNode.getLabel());

        if (labelNode.isLoopStart()) {
          clonedLabel.setLoopStart();
        }

        yield clonedLabel;
      }
      default -> {
        // Plain CFANode
        CFANode clonedNode = new CFANode(originalNode.getFunction());

        if (originalNode.isLoopStart()) {
          clonedNode.setLoopStart();
        }

        yield clonedNode;
      }
    };
  }

  /**
   * After all nodes are created, wire up FunctionEntryNode ↔ FunctionExitNode references. This is
   * called at the end of cloneAllNodes but we handle it during cloneAllEdges since we need all
   * nodes to be created first.
   */
  private void wireEntryExitNodes() {
    for (Map.Entry<CFANode, CFANode> entry : nodeMap.entrySet()) {
      CFANode original = entry.getKey();
      CFANode cloned = entry.getValue();

      if (original instanceof CFunctionEntryNode origEntry
          && cloned instanceof CFunctionEntryNode clonedEntry) {
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
      case FunctionReturnEdge -> cloneFunctionReturnEdge(originalEdge, loc, clonedPred, clonedSucc);
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
