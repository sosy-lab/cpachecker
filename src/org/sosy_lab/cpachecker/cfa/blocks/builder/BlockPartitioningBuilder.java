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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;


/**
 * Helper class can build a <code>BlockPartitioning</code> from a partition of a program's CFA into blocks.
 */
public class BlockPartitioningBuilder {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION = CFATraversal.dfs().ignoreFunctionCalls();

  private final Map<CFANode, Set<ReferencedVariable>> referencedVariablesMap = new HashMap<>();
  private final Map<CFANode, Set<CFANode>> callNodesMap = new HashMap<>();
  private final Map<CFANode, Set<CFANode>> returnNodesMap = new HashMap<>();
  private final Map<CFANode, Set<FunctionEntryNode>> innerFunctionCallsMap = new HashMap<>();
  private final Map<CFANode, Set<CFANode>> blockNodesMap = new HashMap<>();

  public BlockPartitioningBuilder(Set<CFANode> mainFunctionBody) {
  }

  public BlockPartitioning build(CFANode mainFunction) {
    //fixpoint iteration to take inner function calls into account for referencedVariables and callNodesMap
    boolean changed = true;
    outer: while (changed) {
      changed = false;
      for (CFANode node : referencedVariablesMap.keySet()) {
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

          if (referencedVariablesMap.get(node).addAll(functionVars)) {
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
    for (CFANode key : returnNodesMap.keySet()) {
      blocks.add(new Block(referencedVariablesMap.get(key), callNodesMap.get(key), returnNodesMap.get(key), blockNodesMap.get(key)));
    }
    return new BlockPartitioning(blocks, mainFunction);
  }

  /**
   * @param nodes Nodes from which Block should be created; if the set of nodes contains inner function calls, the called function body should NOT be included
   */

  public void addBlock(Set<CFANode> nodes, CFANode mainFunction) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes, mainFunction);
    Set<CFANode> returnNodes = collectReturnNodes(nodes, mainFunction);
    Set<FunctionEntryNode> innerFunctionCalls = collectInnerFunctionCalls(nodes);

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
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge e = node.getLeavingEdge(i);
        if (e instanceof CFunctionCallEdge) {
          result.add(((CFunctionCallEdge)e).getSuccessor());
        }
      }
    }
    return result;
  }

  private Set<CFANode> collectCallNodes(Set<CFANode> pNodes, CFANode mainFunction) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node instanceof FunctionEntryNode &&
         node.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
        //main definition is always a call edge
        result.add(node);
        continue;
      }
      if (node.getEnteringSummaryEdge() != null) {
        CFANode pred = node.getEnteringSummaryEdge().getPredecessor();
        if (!pNodes.contains(pred)) {
          result.add(node);
        }
        //ignore inner function calls
        continue;
      }
      for (int i = 0; i < node.getNumEnteringEdges(); i++) {
        CFANode pred = node.getEnteringEdge(i).getPredecessor();
        if (!pNodes.contains(pred)) {
          //entering edge from "outside" of the given set of nodes
          //-> this is a call-node
          result.add(node);
        }
      }
    }
    return result;
  }

  private Set<CFANode> collectReturnNodes(Set<CFANode> pNodes, CFANode mainFunction) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node instanceof FunctionExitNode &&
         node.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
        //main exit nodes are always return nodes
        result.add(node);
        continue;
      }

      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFANode succ = node.getLeavingEdge(i).getSuccessor();
        if (!pNodes.contains(succ)) {
          //leaving edge from inside of the given set of nodes to outside
          //-> this is a either return-node or a function call
          if (!(node.getLeavingEdge(i) instanceof CFunctionCallEdge)) {
            //-> only add if its not a function call
            result.add(node);
          } else {
            //otherwise check if the summary edge is inside of the block
            CFANode sumSucc = ((CFunctionCallEdge)node.getLeavingEdge(i)).getSummaryEdge().getSuccessor();
            if (!pNodes.contains(sumSucc)) {
              //summary edge successor not in nodes set; this is a leaving edge
              //add entering nodes
              for (int j = 0; j < sumSucc.getNumEnteringEdges(); j++) {
                result.add(sumSucc.getEnteringEdge(j).getPredecessor());
              }
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
