/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


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

  public BlockPartitioning build(CFANode mainFunction) {
    //fixpoint iteration to take inner function calls into account for referencedVariables and callNodesMap
    boolean changed = true;
    outer: while (changed) {
      changed = false;
      for (Entry<CFANode, Set<ReferencedVariable>> entry : referencedVariablesMap.entrySet()) {
        CFANode node = entry.getKey();
        for (CFANode calledFun : innerFunctionCallsMap.get(node)) {
          Set<ReferencedVariable> functionVars = referencedVariablesMap.get(calledFun);
          Set<CFANode> functionBody = blockNodesMap.get(calledFun);
          if (functionVars == null || functionBody == null) {
            assert functionVars == null && functionBody == null;
            //compute it only the fly
            functionBody = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(calledFun);
            functionVars = collectReferencedVariables(functionBody);
            //and save it
            blockNodesMap.put(calledFun, functionBody);
            referencedVariablesMap.put(calledFun, functionVars);
            innerFunctionCallsMap.put(calledFun, collectInnerFunctionCalls(functionBody));
            changed = true;
            continue outer;
          }

          if (entry.getValue().addAll(functionVars)) {
            changed = true;
          }
          if (blockNodesMap.get(node).addAll(functionBody)) {
            changed = true;
          }
        }
      }
    }

    //now we can create the Blocks   for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<>(returnNodesMap.keySet().size());
    for (Entry<CFANode, Set<CFANode>> entry : returnNodesMap.entrySet()) {
      CFANode key = entry.getKey();
      blocks.add(new Block(referencedVariablesMap.get(key), callNodesMap.get(key), entry.getValue(), blockNodesMap.get(key)));
    }
    return new BlockPartitioning(blocks, mainFunction);
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

  private Set<FunctionEntryNode> collectInnerFunctionCalls(Set<CFANode> pNodes) {
    Set<FunctionEntryNode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      for (CFAEdge e : CFAUtils.leavingEdges(node).filter(CFunctionCallEdge.class)) {
        result.add(((CFunctionCallEdge)e).getSuccessor());
      }
    }
    return result;
  }

  private Set<CFANode> collectCallNodes(Set<CFANode> pNodes) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node.getEnteringSummaryEdge() != null) {
        // if we have an ingoing summaryEdge and the predecessor is not in the block, add the current node.
        // TODO what happens when the block begins inside the called function
        if (!pNodes.contains(node.getEnteringSummaryEdge().getPredecessor())) {
          result.add(node);
        }
        // ignore inner function calls, when the inner function is fully included
        continue;
      }
      if (node.getNumEnteringEdges() == 0) {
        assert node.getEnteringSummaryEdge() == null;
        // entry of main function
        result.add(node);
        continue;
      }
      for (CFANode pred : CFAUtils.predecessorsOf(node)) {
        if (!pNodes.contains(pred)) {
          // entering edge from "outside" of the given set of nodes
          result.add(node);
        }
      }
    }
    return result;
  }

  private Set<CFANode> collectReturnNodes(Set<CFANode> pNodes) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node.getNumLeavingEdges() == 0) {
        // exit of main function
        result.add(node);
        continue;
      }

      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
        CFANode succ = leavingEdge.getSuccessor();
        if (!pNodes.contains(succ)) {
          // leaving edge from inside of the given set of nodes to outside
          // -> this is a either return-node or a function call
          if (!(leavingEdge instanceof CFunctionCallEdge)) {
            // -> only add if its not a function call
            result.add(node);
          } else {
            // otherwise check if the summary edge is inside of the block
            CFANode sumSucc = ((CFunctionCallEdge) leavingEdge).getSummaryEdge().getSuccessor();
            if (!pNodes.contains(sumSucc)) {
              // summary edge successor not in nodes set; this is a leaving edge
              // add entering nodes
              Iterables.addAll(result, CFAUtils.predecessorsOf(sumSucc));
            }
          }
        }
      }
    }
    return result;
  }

  private Set<ReferencedVariable> collectReferencedVariables(Set<CFANode> nodes) {
    return (new ReferencedVariablesCollector(nodes)).getVars();
  }
}
