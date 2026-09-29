// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Computes an overapproximation of the possible entry stacks, without program data.
 *
 * <p>All branches are explored, and equal (block, stack) entries and local (location, stack)
 * positions are visited once. A context is published only after the whole exploration completes and
 * only for blocks with one possible entry stack. Recursion, unsupported transfers, or the work
 * limit discard the entire result: a partially explored call graph cannot establish uniqueness. No
 * reachability verdict is derived from this preanalysis.
 */
public final class DssBlockCallstackAnalysis {
  private DssBlockCallstackAnalysis() {}

  private record Entry(BlockNode block, CallstackStateEqualsWrapper stack) {}

  private record Position(CFANode node, CallstackStateEqualsWrapper stack, boolean initial) {}

  public static Map<BlockNode, CallstackState> compute(
      BlockGraph graph, CFA cfa, LogManager logger, ShutdownNotifier shutdown)
      throws InterruptedException, InvalidConfigurationException {
    return compute(graph, cfa, logger, shutdown, 100_000);
  }

  static Map<BlockNode, CallstackState> compute(
      BlockGraph graph, CFA cfa, LogManager logger, ShutdownNotifier shutdown, int workLimit)
      throws InterruptedException, InvalidConfigurationException {
    CallstackTransferRelation transfer =
        new CallstackCPA(Configuration.defaultConfiguration(), logger).getTransferRelation();
    Map<String, BlockNode> blocks = new HashMap<>();
    graph.getNodes().forEach(b -> blocks.put(b.getId(), b));
    Map<BlockNode, Set<CallstackStateEqualsWrapper>> entries = new HashMap<>();
    ArrayDeque<Entry> waiting = new ArrayDeque<>();
    waiting.add(
        new Entry(
            graph.getRoot(),
            new CallstackStateEqualsWrapper(
                new CallstackState(
                    null, cfa.getMainFunction().getFunctionName(), cfa.getMainFunction()))));
    int work = 0;
    try {
      while (!waiting.isEmpty()) {
        shutdown.shutdownIfNecessary();
        Entry entry = waiting.removeFirst();
        if (!entries.computeIfAbsent(entry.block(), unused -> new HashSet<>()).add(entry.stack())) {
          continue;
        }
        ArrayDeque<Position> local = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        local.add(new Position(entry.block().getInitialLocation(), entry.stack(), true));
        while (!local.isEmpty()) {
          shutdown.shutdownIfNecessary();
          if (++work > workLimit) {
            return Map.of(); // A partial exploration cannot establish uniqueness.
          }
          Position position = local.removeFirst();
          if (!visited.add(position)) {
            continue;
          }
          if (position.node().equals(entry.block().getFinalLocation())
              && (!position.initial() || entry.block().getEdges().isEmpty())) {
            for (String successor : entry.block().getSuccessorIds()) {
              waiting.add(new Entry(blocks.get(successor), position.stack()));
            }
            continue;
          }
          for (var edge : position.node().getLeavingEdges()) {
            if (!entry.block().getEdges().contains(edge)
                || edge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
              continue;
            }
            for (AbstractState next :
                transfer.getAbstractSuccessorsForEdge(
                    position.stack().getState(), SingletonPrecision.getInstance(), edge)) {
              local.add(
                  new Position(
                      edge.getSuccessor(),
                      new CallstackStateEqualsWrapper((CallstackState) next),
                      false));
            }
          }
        }
      }
    } catch (CPATransferException e) {
      logger.log(Level.FINE, "Keeping unknown entry callstacks:", e.getMessage());
      return Map.of();
    }
    Map<BlockNode, CallstackState> unique = new HashMap<>();
    entries.forEach(
        (block, stacks) -> {
          if (stacks.size() == 1) {
            unique.put(block, stacks.iterator().next().getState());
          }
        });
    return unique;
  }
}
