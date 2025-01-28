// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class STBridges {

  public static final class BridgeComponents {
    private final ImmutableList<CFAEdge> bridges;
    private final ImmutableList<ImmutableList<CFANode>> connections;

    public BridgeComponents(List<CFAEdge> pBridges, List<Deque<CFANode>> pConnections) {
      bridges = ImmutableList.copyOf(pBridges);
      connections = transformedImmutableListCopy(pConnections, ImmutableList::copyOf);
    }

    public ImmutableList<CFAEdge> getBridges() {
      return bridges;
    }

    public ImmutableList<ImmutableList<CFANode>> getConnections() {
      return connections;
    }

    public ImmutableList<BlockNodeWithoutGraphInformation> connectionsWithEdges(String idPrefix) {
      int id = 1;
      ImmutableList.Builder<BlockNodeWithoutGraphInformation> edges = ImmutableList.builder();

      // Process bridges
      for (CFAEdge bridge : bridges) {
        edges.add(
            new BlockNodeWithoutGraphInformation(
                idPrefix + id++,
                bridge.getPredecessor(),
                bridge.getSuccessor(),
                ImmutableSet.of(bridge.getPredecessor(), bridge.getSuccessor()),
                ImmutableSet.of(bridge)));
      }

      // Process connections
      for (ImmutableList<CFANode> connection : connections) {
        if (connection.size() == 1) {
          continue;
        } else {
          ImmutableSet<CFANode> includes = ImmutableSet.copyOf(connection);
          ImmutableSet.Builder<CFAEdge> connectionEdges = ImmutableSet.builder();
          CFANode last = null;
          CFANode first = null;

          for (CFANode cfaNode : connection) {
            boolean hasSuccessorInIncludes = false;
            boolean hasSuccessorOutOfIncludes = false;
            for (CFAEdge leavingEdge : CFAUtils.leavingEdges(cfaNode)) {
              if (includes.contains(leavingEdge.getSuccessor())) {
                connectionEdges.add(leavingEdge);
                hasSuccessorInIncludes = true;
              } else {
                hasSuccessorOutOfIncludes = true;
              }
            }

            if (!hasSuccessorInIncludes) {
              // terminating functions may cause 0 successors, too
              if (last == null
                  || last.getNumLeavingEdges() == 0
                  || cfaNode.getNumLeavingEdges() > 0) {
                last = cfaNode;
              }
            } else if (hasSuccessorOutOfIncludes) {
              last = cfaNode;
            }

            boolean hasPredecessorInIncludes = false;
            boolean hasPredecessorOutOfIncludes = false;
            for (CFAEdge enteringEdge : CFAUtils.enteringEdges(cfaNode)) {
              if (includes.contains(enteringEdge.getPredecessor())) {
                hasPredecessorInIncludes = true;
              } else {
                hasPredecessorOutOfIncludes = true;
              }
            }

            if (!hasPredecessorInIncludes) {
              first = cfaNode;
            } else if (hasPredecessorOutOfIncludes && first == null) {
              first = cfaNode;
            }
          }

          // Ensure first and last nodes are found
          checkState(
              first != null && last != null, "First or last node not found in the connection.");

          edges.add(
              new BlockNodeWithoutGraphInformation(
                  idPrefix + id++,
                  Objects.requireNonNull(first),
                  Objects.requireNonNull(last),
                  includes,
                  connectionEdges.build()));
        }
      }
      return edges.build();
    }

    public boolean isEmpty() {
      return bridges.isEmpty() && connections.isEmpty();
    }

    @Override
    public String toString() {
      return "BridgeComponents[" + "bridges=" + bridges + ", " + "connections=" + connections + ']';
    }
  }

  private static List<CFAEdge> findPathFromMainToExit(BlockNodeWithoutGraphInformation blockNode) {
    Deque<List<CFAEdge>> paths = new ArrayDeque<>();
    CFANode mainFunction = blockNode.getInitialLocation();
    CFANode exitNode = blockNode.getFinalLocation();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(mainFunction)) {

      if (blockNode.getEdges().contains(leavingEdge)) {
        List<CFAEdge> path = new ArrayList<>();
        path.add(leavingEdge);
        paths.add(path);
      }
    }
    while (!paths.isEmpty()) {
      List<CFAEdge> currentPath = paths.removeFirst();
      CFAEdge lastEdge = currentPath.get(currentPath.size() - 1);
      CFANode lastNode = lastEdge.getSuccessor();
      if (lastNode.equals(exitNode)) {
        return currentPath;
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(lastNode)) {
        if (blockNode.getEdges().contains(leavingEdge)) {
          List<CFAEdge> copy = new ArrayList<>(currentPath);
          copy.add(leavingEdge);
          paths.add(copy);
        }
      }
    }
    return ImmutableList.of();
  }

  /**
   * Finds all <a href="https://doi.org/10.1016/j.dam.2021.08.026">s-t bridges</a> in a given CFA.
   * An s-t bridge is a CFAEdge that all syntactic paths have to traverse.
   *
   * @param blockNode CFA for which
   * @return All s-t bridges in <code>pCFA</code>.
   */
  public static BridgeComponents computeBridges(BlockNodeWithoutGraphInformation blockNode) {
    Map<CFANode, Integer> comp = new LinkedHashMap<>();
    CFANode exitNode = blockNode.getFinalLocation();

    for (CFANode node : blockNode.getNodes()) {
      comp.put(node, 0);
    }
    List<CFAEdge> path = findPathFromMainToExit(blockNode);
    if (path.isEmpty()) {
      return new BridgeComponents(ImmutableList.of(), ImmutableList.of());
    }
    Map<Integer, Deque<CFANode>> bridgeComponents = new HashMap<>();
    Deque<CFANode> nodeQueue = new ArrayDeque<>();
    List<CFAEdge> bridges = new ArrayList<>();
    int association = 1;
    while (comp.get(exitNode) == 0) {
      if (association == 1) {
        nodeQueue.add(blockNode.getInitialLocation());
        putOrUpdate(bridgeComponents, association, blockNode.getInitialLocation());
        comp.put(blockNode.getInitialLocation(), 1);

      } else {
        for (int i = path.size() - 1; i >= 0; i--) {
          CFAEdge currentEdge = path.get(i);
          if (comp.get(currentEdge.getPredecessor()) != 0) {
            bridges.add(currentEdge);
            nodeQueue.add(currentEdge.getSuccessor());
            putOrUpdate(bridgeComponents, association, currentEdge.getSuccessor());
            comp.put(currentEdge.getSuccessor(), association);
            break;
          }
        }
      }
      while (!nodeQueue.isEmpty()) {
        CFANode predecessor = nodeQueue.removeFirst();
        for (CFANode successor : findFlippedSuccessors(predecessor, path, blockNode)) {
          if (comp.get(successor) == 0) {
            nodeQueue.add(successor);
            putOrUpdate(bridgeComponents, association, successor);
            comp.put(successor, association);
          }
        }
      }
      association++;
    }
    return new BridgeComponents(bridges, ImmutableList.copyOf(bridgeComponents.values()));
  }

  /**
   * Find successors of a given node. Consider flipped edges on the fly.
   *
   * @param pNode searches for successors of this node
   * @param pFlipped these edges are flipped in the original CFA
   * @return successors of <code>pNode</code>
   */
  private static Iterable<CFANode> findFlippedSuccessors(
      CFANode pNode, List<CFAEdge> pFlipped, BlockNodeWithoutGraphInformation blockNode) {
    ImmutableSet<CFAEdge> copy = ImmutableSet.copyOf(pFlipped);
    ImmutableSet.Builder<CFANode> successors = ImmutableSet.builder();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pNode)) {
      if (!copy.contains(leavingEdge) && blockNode.getEdges().contains(leavingEdge)) {
        successors.add(leavingEdge.getSuccessor());
      }
    }
    for (CFAEdge cfaEdge : copy) {
      if (cfaEdge.getSuccessor().equals(pNode)) {
        successors.add(cfaEdge.getPredecessor());
      }
    }
    return successors.build();
  }

  private static void putOrUpdate(Map<Integer, Deque<CFANode>> c, int i, CFANode node) {
    if (c.containsKey(i)) {
      c.get(i).add(node);
    } else {
      c.put(i, new ArrayDeque<>(ImmutableSet.of(node)));
    }
  }
}
