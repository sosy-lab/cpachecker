/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Helper class can build a <code>BlockPartitioning</code> from a partition of a program's CFA into blocks.
 */
public class BlockPartitioningBuilder {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION = CFATraversal.dfs().ignoreFunctionCalls();

  protected final Map<CFANode, Set<ReferencedVariable>> referencedVariablesMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> callNodesMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> returnNodesMap = new HashMap<>();
  protected final Map<CFANode, Set<FunctionEntryNode>> innerFunctionCallsMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> blockNodesMap = new HashMap<>();

  public BlockPartitioningBuilder() {}

  public BlockPartitioning build(CFA cfa) {

    // using Multimaps causes an overhead here,
    // because large sets could be copied once at 'putAll' and then they are thrown away

    // first pre-compute the block-structure for functions. we use it for optimization
    Map<FunctionEntryNode, Set<CFANode>> functions = new HashMap<>();
    Map<FunctionEntryNode, Set<ReferencedVariable>> referencedVariables = new HashMap<>();
    Map<FunctionEntryNode, Set<FunctionEntryNode>> innerFunctionCalls = new HashMap<>();
    for (FunctionEntryNode head : cfa.getAllFunctionHeads()) {
      final Set<CFANode> body = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(head);
      functions.put(head, body);
      referencedVariables.put(head, collectReferencedVariables(body));
      innerFunctionCalls.put(head, collectInnerFunctionCalls(body));
    }

    // then get directly called functions and sum up all indirectly called functions
    Map<CFANode, Set<FunctionEntryNode>> blockFunctionCalls = new HashMap<>();
    for (CFANode callNode : callNodesMap.keySet()) {
      Set<FunctionEntryNode> calledFunctions = getAllCalledFunctions(innerFunctionCalls,
          collectInnerFunctionCalls(blockNodesMap.get(callNode)));
      blockFunctionCalls.put(callNode, calledFunctions);
    }

    //now we can create the Blocks   for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<>();
    for (Entry<CFANode, Set<CFANode>> entry : callNodesMap.entrySet()) {
      CFANode callNode = entry.getKey();

      // we collect nodes and variables from all inner function calls
      Collection<Iterable<ReferencedVariable>> variables = new ArrayList<>();
      Collection<Iterable<CFANode>> blockNodes = new ArrayList<>();
      Set<CFANode> directNodes = blockNodesMap.get(callNode);
      blockNodes.add(directNodes);
      variables.add(referencedVariablesMap.get(callNode));
      for (FunctionEntryNode calledFunction : blockFunctionCalls.get(callNode)) {
        blockNodes.add(functions.get(calledFunction));
        variables.add(referencedVariables.get(calledFunction));
      }

      blocks.add(
          new Block(
              Iterables.concat(variables),
              entry.getValue(),
              returnNodesMap.get(callNode),
              Iterables.concat(blockNodes)));
    }

    return new BlockPartitioning(blocks, cfa.getMainFunction());
  }

  private static Set<FunctionEntryNode> getAllCalledFunctions(
      Map<FunctionEntryNode, Set<FunctionEntryNode>> innerFunctionCalls,
      Set<FunctionEntryNode> directFunctions) {
    Set<FunctionEntryNode> calledFunctions = new HashSet<>();
    Deque<FunctionEntryNode> waitlist = new ArrayDeque<>();
    waitlist.addAll(directFunctions);
    while (!waitlist.isEmpty()) {
      FunctionEntryNode entry  = waitlist.pop();
      if (calledFunctions.add(entry)) {
        waitlist.addAll(innerFunctionCalls.get(entry));
      }
    }
    return calledFunctions;
  }

  /**
   * @param nodes Nodes from which Block should be created;
   *              if the set of nodes contains inner function calls, the called
   *              function body should NOT be included.
   * @param blockHead Entry point for the block.
   */
  public void addBlock(Set<CFANode> nodes, CFANode blockHead) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes);
    Set<CFANode> returnNodes = collectReturnNodes(nodes);
    Set<FunctionEntryNode> innerFunctionCalls = collectInnerFunctionCalls(nodes);

    if (callNodes.isEmpty()) {
     /* What shall we do with function, which is not called from anywhere?
      * There are problems with them at partitioning building stage
      */
      return;
    }

    CFANode registerNode = null;
    for (CFANode node : callNodes) {
      registerNode = node;
      if (node instanceof FunctionEntryNode) {
        break;
      }
    }

    referencedVariablesMap.put(registerNode, referencedVariables);
    callNodesMap.put(registerNode, callNodes);
    returnNodesMap.put(registerNode, returnNodes);
    innerFunctionCallsMap.put(registerNode, innerFunctionCalls);
    blockNodesMap.put(registerNode, nodes);
  }

  /** get all inner function calls of the current block.
   * Precondition: the block does not yet include function-calls
   * (except we have a function-block of a recursive function)
   *
   *  @return all directly called functions (transitive function calls not included) */
  private Set<FunctionEntryNode> collectInnerFunctionCalls(Set<CFANode> pNodes) {
    Builder<FunctionEntryNode> result = ImmutableSet.builder();
    for (CFANode node : pNodes) {
      for (CFAEdge e : CFAUtils.leavingEdges(node).filter(CFunctionCallEdge.class)) {
        result.add(((CFunctionCallEdge)e).getSuccessor());
      }
    }
    return result.build();
  }

  /**
   * get all call-nodes of the current block.
   *
   * <p>Precondition: the block does not yet include function-calls
   */
  private Set<CFANode> collectCallNodes(Set<CFANode> pNodes) {
    Builder<CFANode> result = ImmutableSet.builder();
    for (CFANode node : pNodes) {

      // handle a bug in CFA creation: there are ugly CFA-nodes ... and we ignore them.
      if (node.getNumEnteringEdges() == 0 && node.getNumLeavingEdges() == 0) {
        continue;
      }

      if (node.getNumEnteringEdges() == 0 && node.getEnteringSummaryEdge() == null) {
        // entry of main function
        result.add(node);
        continue;
      }

      for (CFAEdge edge : CFAUtils.allEnteringEdges(node)) {
        if (edge.getEdgeType() != CFAEdgeType.FunctionReturnEdge
            && !pNodes.contains(edge.getPredecessor())) {
          // entering edge from "outside" of the given set of nodes.
          // this case also handles blocks from recursive function-calls,
          // because at least one callee should be outside of the block.
          result.add(node);
        }
      }
    }
    return result.build();
  }

  /**
   * get all exit-nodes of the current block
   *
   * <p>Precondition: the block does not yet include function-calls
   */
  private Set<CFANode> collectReturnNodes(Set<CFANode> pNodes) {
    Builder<CFANode> result = ImmutableSet.builder();
    for (CFANode node : pNodes) {

      // handle a bug in CFA creation: there are ugly CFA-nodes ... and we ignore them.
      if (node.getNumEnteringEdges() == 0 && node.getNumLeavingEdges() == 0) {
        continue;
      }

      if (node.getNumLeavingEdges() == 0 && !(node instanceof CFATerminationNode)) {
        // exit of main function
        result.add(node);
        continue;
      }

      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge.getEdgeType() != CFAEdgeType.FunctionCallEdge
            && !pNodes.contains(edge.getSuccessor())) {
          // leaving edge from inside of the given set of nodes to outside
          // -> this is a either return-node or a function call
          // this case also handles blocks from recursive function-calls,
          // because at least one callee should be outside of the block.
          result.add(node);
        }
      }
    }
    return result.build();
  }

  private Set<ReferencedVariable> collectReferencedVariables(Set<CFANode> nodes) {
    return (new ReferencedVariablesCollector(nodes)).getVars();
  }
}
