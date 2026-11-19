// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

/** Copies a CFA for DSS instrumentation, repairing only unreachable orphaned call sites. */
final class DssCfaCopy {

  private DssCfaCopy() {}

  static MutableCFA copyOf(CFA pCfa, Configuration pConfig, LogManager pLogger) {
    CfaMutableNetwork graph = CfaMutableNetwork.of(pCfa);
    Set<CFANode> reachable = new HashSet<>();
    Deque<CFANode> waiting = new ArrayDeque<>();
    reachable.add(pCfa.getMainFunction());
    waiting.add(pCfa.getMainFunction());
    while (!waiting.isEmpty()) {
      for (CFAEdge edge : graph.outEdges(waiting.removeFirst())) {
        if (edge instanceof CFunctionSummaryEdge) {
          // A syntactic summary can remain even for a call whose callee never returns.
          continue;
        }
        if (edge instanceof CFunctionReturnEdge returning
            && !reachable.contains(returning.getCallNode())) {
          continue;
        }
        if (edge instanceof CFunctionCallEdge call) {
          checkState(
              graph.edges().contains(call.getSummaryEdge())
                  && call.getSummaryEdge().getPredecessor() == call.getPredecessor()
                  && call.getSummaryEdge().getFunctionEntry() == call.getSuccessor(),
              "Missing or inconsistent summary for call edge: %s",
              call);
          // The callee's exit may have been processed before this call site was reached.
          // In that case its matching return must be reconsidered for the new caller.
          for (CFAEdge incoming : graph.inEdges(call.getSummaryEdge().getSuccessor())) {
            if (incoming instanceof CFunctionReturnEdge returning
                && returning.getSummaryEdge() == call.getSummaryEdge()
                && reachable.contains(returning.getPredecessor())) {
              enqueue(returning.getSuccessor(), reachable, waiting);
            }
          }
        }
        enqueue(graph.incidentNodes(edge).nodeV(), reachable, waiting);
      }
    }

    for (CFAEdge edge : ImmutableList.copyOf(graph.edges())) {
      if (!(edge instanceof CFunctionSummaryEdge summary)) {
        continue;
      }
      CFANode caller = summary.getPredecessor();
      if (graph.outEdges(caller).stream().anyMatch(CFunctionCallEdge.class::isInstance)) {
        continue;
      }
      // Reachability ignores data and combines callee exits across all reached callers, but
      // requires both the call site and the callee exit before following a return. This is an
      // overapproximation of executable paths, including recursion and non-returning functions.
      // Restoring a call at a site outside this set cannot add an execution from the entry.
      checkState(
          !reachable.contains(caller), "Missing call edge at reachable call site: %s", summary);
      checkState(
          graph.nodes().contains(summary.getFunctionEntry())
              && pCfa.entryNodes().contains(summary.getFunctionEntry()),
          "Missing callee for orphaned summary edge: %s",
          summary);
      for (CFAEdge incoming : graph.inEdges(summary.getSuccessor())) {
        if (incoming instanceof CFunctionReturnEdge returning
            && returning.getSummaryEdge() == summary) {
          checkState(
              returning.getPredecessor().getEntryNode() == summary.getFunctionEntry(),
              "Inconsistent callee for orphaned summary edge: %s",
              summary);
        }
      }
      // Modify only the temporary network. Neither the original nodes nor their edges are changed.
      graph.addEdge(
          caller,
          summary.getFunctionEntry(),
          new CFunctionCallEdge(
              summary.getRawStatement(),
              summary.getFileLocation(),
              caller,
              summary.getFunctionEntry(),
              summary.getExpression(),
              summary));
    }

    CFA copy = CCfaTransformer.createCfa(pConfig, pLogger, pCfa, graph, (edge, ast) -> ast);
    NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>();
    TreeMultimap<String, CFANode> nodes = TreeMultimap.create();
    for (CFANode node : copy.nodes()) {
      nodes.put(node.getFunctionName(), node);
      if (node instanceof FunctionEntryNode entry) {
        functions.put(node.getFunctionName(), entry);
      }
    }
    return new MutableCFA(functions, nodes, copy.getMetadata());
  }

  private static void enqueue(CFANode pNode, Set<CFANode> pReached, Deque<CFANode> pWaiting) {
    if (pReached.add(pNode)) {
      pWaiting.addLast(pNode);
    }
  }
}
