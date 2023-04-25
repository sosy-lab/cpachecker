// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisSummaryCache {

  private static ValueAnalysisSummaryCache cache;
  private final Multimap<Block, ValueAnalysisSummary> summaries;
  private final BlockPartitioning partitioning;
  private final Multimap<String, ValueAnalysisSummary> locationStringToSummaries;
  private final Multimap<String, String> impacts;

  public ValueAnalysisSummaryCache(BlockPartitioning pPartitioning) {
    partitioning = pPartitioning;
    summaries = LinkedHashMultimap.create();
    locationStringToSummaries = LinkedHashMultimap.create();
    impacts = LinkedHashMultimap.create();
  }

  public static void initialize(BlockPartitioning pPartitioning) {
    cache = new ValueAnalysisSummaryCache(pPartitioning);
  }

  public static ValueAnalysisSummaryCache getInstance() {
    return cache;
  }

  public void addSummaryForReachedSet(ReachedSet pReachedSet, BAMCache pBAMCache) {

    // do not generate a summary, if the block is not fully analyzed
    if (pReachedSet.hasWaitingState()) return;

    var entryState = pReachedSet.getFirstState();

    var entryNode = AbstractStates.extractLocation(entryState);
    var block = partitioning.getBlockForCallNode(entryNode);

    var bamEntry = pBAMCache.get(entryState, pReachedSet.getPrecision(entryState), block);

    if (bamEntry.getExitStates().size() != 1) return;

    var exitState = Iterables.getOnlyElement(bamEntry.getExitStates());

    var entryValueState = AbstractStates.extractStateByType(entryState, ValueAnalysisState.class);
    var exitValueState = AbstractStates.extractStateByType(exitState, ValueAnalysisState.class);
    var precision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(pReachedSet);

    var newSummary = new ValueAnalysisSummary(block, entryValueState, exitValueState, precision);

    var blockSummaries = summaries.get(block);

    boolean addSummary = true;

    for (var summary : blockSummaries) {
      if (summary.getEntryState().equals(entryValueState)) {
        if (exitValueState.isLessOrEqual(summary.getExitState())) {
          // summary is more precise than old summary -> replace it
          summaries.remove(block, summary);
          break;
        } else {
          // summary is less precise than old summary -> do not add
          addSummary = false;
        }
      }
    }

    if (addSummary) {
      summaries.put(block, newSummary);
    }
  }

  public void add(String pLocation, ValueAnalysisSummary pSummary) {
    locationStringToSummaries.put(pLocation, pSummary);
  }

  private record NodePair(CFANode oldNode, CFANode newNode) { }

  public void removeSummariesForChangedBlocks(CFA pCfa, CFA pLastCfa) {
    List<String> LocationsToRemove = new ArrayList<>();
    for (var location : locationStringToSummaries.keySet()) {
      CFANode oldStartNode = null;
      boolean matchFound = false;
      if (location.contains(" ")) {
        var x = location.split(" ");
        var funcName = x[0];
        var line = Integer.parseInt(x[1]);
        var oldLoops = pLastCfa.getLoopStructure().get().getLoopsForFunction(funcName);
        Loop oldLoop = null;

        for (Loop loop : oldLoops) {
          var incEdge = Iterables.getOnlyElement(loop.getIncomingEdges());
          if (incEdge.getLineNumber() == line) {
            oldStartNode = incEdge.getSuccessor();
            oldLoop = loop;
            break;
          }
        }

        var loops = pCfa.getLoopStructure().get().getLoopsForFunction(funcName);
        for (var loop : loops) {
          if (loop.getLoopNodes().size() == oldLoop.getLoopNodes().size()) {
            var incEdge = Iterables.getOnlyElement(loop.getIncomingEdges());
            var startNode = incEdge.getSuccessor();
            var exitNode = startNode;

            if (!nodesMatch(oldStartNode, startNode)) {
              continue;
            }

            var edge = startNode.getLeavingEdge(0);
            var oldEdge = oldStartNode.getLeavingEdge(0);

            if (!edgesMatch(oldEdge, edge)) {
              continue;
            }

            var node = edge.getSuccessor();
            var oldNode = oldEdge.getSuccessor();

            Queue<NodePair> waitList = new ArrayDeque<>();
            waitList.add(new NodePair(oldNode, node));

            if (blockMatches(location, waitList, exitNode)) {
              matchFound = true;
              for (var summary : locationStringToSummaries.get(location)) {
                var callNode = incEdge.getPredecessor();
                summary.setBlock(partitioning.getBlockForCallNode(callNode));
              }
              break;
            }
          }
        }

      } else {
        oldStartNode = pLastCfa.getFunctionHead(location);
        var startNode = pCfa.getFunctionHead(location);
        if (startNode != null) {
          var exitNode = startNode.getExitNode().get();

          Queue<NodePair> waitList = new ArrayDeque<>();
          waitList.add(new NodePair(oldStartNode, startNode));

          if (blockMatches(location, waitList, exitNode)) {
            matchFound = true;
            for (var summary : locationStringToSummaries.get(location)) {
              summary.setBlock(partitioning.getBlockForCallNode(startNode));
            }
          }
        }
      }
      if (!matchFound) {
        LocationsToRemove.add(location);
      }
    }

    for (var location : LocationsToRemove) {
      removeSummariesForLocation(location);
    }

    for (var summary : locationStringToSummaries.values()) {
      summaries.put(summary.getBlock(), summary);
      summary.assignTypes(locationToType);
    }
  }

  private void removeSummariesForLocation(String pLocation) {
    locationStringToSummaries.removeAll(pLocation);
    var impactedLocations = impacts.get(pLocation).stream().toList();
    impacts.removeAll(pLocation);
    for (var impactedLocation : impactedLocations) {
      removeSummariesForLocation(impactedLocation);
    }
  }

  private boolean blockMatches(String currentLocation, Queue<NodePair> waitList, CFANode exitNode) {
    Set<CFANode> finishedNodes = new HashSet<>();
    finishedNodes.add(exitNode);

    while (!waitList.isEmpty()) {
      var currentNodes = waitList.remove();
      var oldNode = currentNodes.oldNode;
      var newNode = currentNodes.newNode;
      if (finishedNodes.contains(newNode)) continue;

      if (!nodesMatch(oldNode, newNode)) {
        return false;
      }

      if (oldNode.isLoopStart() && newNode.isLoopStart()) {
        impacts.put(oldNode.getFunctionName() + " " + oldNode.getLeavingEdge(0).getLineNumber(), currentLocation);
      }

      for (int i = 0; i < newNode.getNumLeavingEdges(); i++) {
        var oldEdge = oldNode.getLeavingEdge(i);
        var newEdge = newNode.getLeavingEdge(i);

        if (oldEdge instanceof FunctionCallEdge oldCallEdge
            && newEdge instanceof FunctionCallEdge newCallEdge) {
          oldEdge = oldCallEdge.getSummaryEdge();
          newEdge = newCallEdge.getSummaryEdge();

          var calledFunction = ((FunctionSummaryEdge) oldEdge).getFunctionEntry().getFunctionName();
          impacts.put(calledFunction, currentLocation);
        }

        if (!edgesMatch(oldEdge, newEdge)) {
          return false;
        }

        var newSuccessor = newEdge.getSuccessor();
        var oldSuccessor = oldEdge.getSuccessor();

        waitList.add(new NodePair(oldSuccessor, newSuccessor));
      }

      finishedNodes.add(newNode);
    }
    return true;
  }

  private boolean nodesMatch(CFANode oldNode, CFANode newNode) {
    return newNode.getClass().equals(oldNode.getClass())
        && newNode.getNumLeavingEdges() == oldNode.getNumLeavingEdges();
  }

  private HashMap<MemoryLocation, Type> locationToType = new HashMap<>();

  private boolean edgesMatch(CFAEdge oldEdge, CFAEdge newEdge) {
    AStatement statement = null;
    if (oldEdge instanceof AStatementEdge edge) {
      statement = edge.getStatement();
    } else if (oldEdge instanceof FunctionSummaryEdge edge) {
      statement = edge.getExpression();
    } else if (oldEdge instanceof AssumeEdge edge) {
      if (edge.getExpression() instanceof ABinaryExpression expr) {
        if (expr.getOperand1() instanceof AIdExpression idExpression) {
          var location = MemoryLocation.fromQualifiedName(idExpression.getDeclaration().getQualifiedName());
          var type = idExpression.getExpressionType();
          if (!locationToType.containsKey(location)) {
            locationToType.put(location, type);
          }
        }
        if (expr.getOperand2() instanceof AIdExpression idExpression) {
          var location = MemoryLocation.fromQualifiedName(idExpression.getDeclaration().getQualifiedName());
          var type = idExpression.getExpressionType();
          if (!locationToType.containsKey(location)) {
            locationToType.put(location, type);
          }
        }
      }
    }

    if (statement instanceof AAssignment assignment) {
      if (assignment.getLeftHandSide() instanceof AIdExpression idExpression) {
        var location = MemoryLocation.fromQualifiedName(idExpression.getDeclaration().getQualifiedName());
        var type = idExpression.getExpressionType();
        if (!locationToType.containsKey(location)) {
          locationToType.put(location, type);
        }
      }
    }

    return oldEdge.getRawStatement().equals(newEdge.getRawStatement())
        && oldEdge.getEdgeType().equals(newEdge.getEdgeType());
  }

  public Set<Block> getBlocks() {
    return summaries.keySet();
  }

  public Collection<ValueAnalysisSummary> getSummaries() {
    return summaries.values();
  }

  public ValueAnalysisSummary getApplicableSummary(
      CFANode pCallNode, ValueAnalysisState pEntryState) {
    return getApplicableSummary(partitioning.getBlockForCallNode(pCallNode), pEntryState);
  }

  public ValueAnalysisState applySummaryOrForget(
      CFANode pCallNode, ValueAnalysisState pEntryState) {
    var summary = getApplicableSummary(partitioning.getBlockForCallNode(pCallNode), pEntryState);
    if (summary == null) {
      var exitState = ValueAnalysisState.copyOf(pEntryState);
      for (var constant : exitState.getConstants()) {
        exitState.forget(constant.getKey());
      }
      summary = new ValueAnalysisSummary(partitioning.getBlockForCallNode(pCallNode), pEntryState, exitState, null);
    }
    return summary.applyToState(pEntryState);
  }

  public ValueAnalysisSummary getApplicableSummary(Block pBlock, ValueAnalysisState pEntryState) {
    pEntryState =
        new ValueAnalysisReducer()
            .getVariableReducedState0(pEntryState, pBlock, pBlock.getCallNode());

    var blockSummaries = summaries.get(pBlock);
    ValueAnalysisSummary applicableSummary = null;

    for (var summary : blockSummaries) {
      if (pEntryState.isLessOrEqual(summary.getEntryState())) {
        if (applicableSummary == null) {
          applicableSummary = summary;
        } else if (summary.getEntryState().isLessOrEqual(applicableSummary.getEntryState())) {
          applicableSummary = summary;
        }
      }
    }

    return applicableSummary;
  }
}
