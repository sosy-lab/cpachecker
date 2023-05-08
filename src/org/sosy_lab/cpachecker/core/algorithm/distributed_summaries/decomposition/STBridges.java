// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
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
      for (CFAEdge bridge : bridges) {
        edges.add(
            new BlockNodeWithoutGraphInformation(
                idPrefix + id++,
                bridge.getPredecessor(),
                bridge.getSuccessor(),
                ImmutableSet.of(bridge.getPredecessor(), bridge.getSuccessor()),
                ImmutableSet.of(bridge)));
      }
      for (ImmutableList<CFANode> connection : connections) {
        if (connection.size() == 1) {
          continue;
        } else {
          ImmutableSet<CFANode> includes = ImmutableSet.copyOf(connection);
          ImmutableSet.Builder<CFAEdge> connectionEdges = ImmutableSet.builder();
          CFANode last = null;
          CFANode first = null;
          for (CFANode cfaNode : connection) {
            boolean hasSuccessor = false;
            for (CFAEdge leavingEdge : CFAUtils.leavingEdges(cfaNode)) {
              if (includes.contains(leavingEdge.getSuccessor())) {
                connectionEdges.add(leavingEdge);
                hasSuccessor = true;
              }
            }
            if (!hasSuccessor) {
              // terminating functions may cause 0 successors, too
              if (last == null
                  || last.getNumLeavingEdges() == 0
                  || cfaNode.getNumLeavingEdges() > 0) {
                last = cfaNode;
              }
            }
            boolean hasPredecessor = false;
            for (CFAEdge leavingEdge : CFAUtils.enteringEdges(cfaNode)) {
              if (includes.contains(leavingEdge.getPredecessor())) {
                hasPredecessor = true;
                break;
              }
            }
            if (!hasPredecessor) {
              first = cfaNode;
            }
          }
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

  private static List<CFAEdge> findPathFromMainToExit(CFA pCFA) {
    Deque<List<CFAEdge>> paths = new ArrayDeque<>();
    FunctionEntryNode mainFunction = pCFA.getMainFunction();
    FunctionExitNode exitNode = mainFunction.getExitNode().orElseThrow();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(mainFunction)) {
      List<CFAEdge> path = new ArrayList<>();
      path.add(leavingEdge);
      paths.add(path);
    }
    while (!paths.isEmpty()) {
      List<CFAEdge> currentPath = paths.removeFirst();
      CFAEdge lastEdge = currentPath.get(currentPath.size() - 1);
      CFANode lastNode = lastEdge.getSuccessor();
      if (lastNode.equals(exitNode)) {
        return currentPath;
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(lastNode)) {
        List<CFAEdge> copy = new ArrayList<>(currentPath);
        copy.add(leavingEdge);
        paths.add(copy);
      }
    }
    return ImmutableList.of();
  }

  /**
   * Finds all <a href="https://doi.org/10.1016/j.dam.2021.08.026">s-t bridges</a> in a given CFA.
   * An s-t bridge is a CFAEdge that all syntactic paths have to traverse.
   *
   * @param pCFA CFA for which
   * @return All s-t bridges in <code>pCFA</code>.
   */
  public static BridgeComponents computeBridges(CFA pCFA) {
    Map<CFANode, Integer> comp = new LinkedHashMap<>();
    FunctionExitNode exitNode = pCFA.getMainFunction().getExitNode().orElseThrow();
    for (CFANode node : pCFA.nodes()) {
      comp.put(node, 0);
    }
    List<CFAEdge> path = findPathFromMainToExit(pCFA);
    if (path.isEmpty()) {
      return new BridgeComponents(ImmutableList.of(), ImmutableList.of());
    }
    Map<Integer, Deque<CFANode>> bridgeComponents = new HashMap<>();
    Deque<CFANode> nodeQueue = new ArrayDeque<>();
    List<CFAEdge> bridges = new ArrayList<>();
    int association = 1;
    while (comp.get(exitNode) == 0) {
      if (association == 1) {
        nodeQueue.add(pCFA.getMainFunction());
        putOrUpdate(bridgeComponents, association, pCFA.getMainFunction());
        comp.put(pCFA.getMainFunction(), 1);
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
        for (CFANode successor : findFlippedSuccessors(predecessor, path)) {
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
  private static Iterable<CFANode> findFlippedSuccessors(CFANode pNode, List<CFAEdge> pFlipped) {
    ImmutableSet<CFAEdge> copy = ImmutableSet.copyOf(pFlipped);
    ImmutableSet.Builder<CFANode> successors = ImmutableSet.builder();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pNode)) {
      if (!copy.contains(leavingEdge)) {
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
