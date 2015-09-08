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
import java.util.Iterator;
import java.util.LinkedList;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;


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

  public BlockPartitioningBuilder() {}

  public BlockPartitioning build(CFANode mainFunction) {

    Map<CFANode, Set<FunctionEntryNode>> workCopyOfInnerFunctionCalls = new HashMap<>();

    /* We chose one representative from every loop.
     * This map stores for the node its representative.
     */
    Map<CFANode, CFANode> loopMapping = new HashMap<>();

    //Deep clone, because we will delete nodes
    for (CFANode node : innerFunctionCallsMap.keySet()) {
      Set<FunctionEntryNode> newSet = Sets.newHashSet(innerFunctionCallsMap.get(node));
      workCopyOfInnerFunctionCalls.put(node, newSet);
    }

    //Set of not handled CFANodes
    Set<CFANode> workCopyOfCFANodes = Sets.newHashSet(referencedVariablesMap.keySet());
    do {
      //loop for finding single nodes
      Set<CFANode> SingleNodes = new HashSet<>();
      do {
        SingleNodes.clear();
        for (CFANode node : workCopyOfCFANodes) {
          Set<FunctionEntryNode> calls = workCopyOfInnerFunctionCalls.get(node);
          //Remove single-node loop
          calls.remove(node);
          if (calls.isEmpty()) {
            SingleNodes.add(node);
          }
        }
        if (!SingleNodes.isEmpty()) {
          workCopyOfCFANodes.removeAll(SingleNodes);

          for (CFANode node : workCopyOfCFANodes) {
            Iterator<FunctionEntryNode> iterator = workCopyOfInnerFunctionCalls.get(node).iterator();
            while (iterator.hasNext()) {
              FunctionEntryNode calledFun = iterator.next();
              if (SingleNodes.contains(calledFun)) {
                iterator.remove();
                joinFunctionPartitioning(node, calledFun);
              }
            }
          }
        }
      } while (!SingleNodes.isEmpty());

      if (workCopyOfCFANodes.isEmpty()) {
        break;
      }
      //Detect a recursion loop
      LinkedList<CFANode> foundedRecursionLoop = new LinkedList<>();
      /* Create a random path by getting the first function call from every node
       * and find the first node which is repeated
       */
      CFANode representativeNode = workCopyOfCFANodes.iterator().next();
      while (!foundedRecursionLoop.contains(representativeNode)) {
        foundedRecursionLoop.add(representativeNode);
        representativeNode = workCopyOfInnerFunctionCalls.get(representativeNode).iterator().next();
        assert (representativeNode != null);
      }

      Set<FunctionEntryNode> functionsCalledFromTheLoop = new HashSet<>();
      //Remove the first elements, which are not included in the loop
      while (!foundedRecursionLoop.pollFirst().equals(representativeNode)) {}
      //Join all partitions of functions from the loop
      for (CFANode recursiveCaller : foundedRecursionLoop) {
        loopMapping.put(recursiveCaller, representativeNode);
        joinFunctionPartitioning(representativeNode, recursiveCaller);
        workCopyOfCFANodes.remove(recursiveCaller);
        functionsCalledFromTheLoop.addAll(workCopyOfInnerFunctionCalls.get(recursiveCaller));
        //Remove the handled node from others
        for (CFANode node : workCopyOfCFANodes) {
          Set<FunctionEntryNode> callers = workCopyOfInnerFunctionCalls.get(node);
          if (callers.remove(recursiveCaller)) {
            //Do not add single node loops
            if (!node.equals(representativeNode)) {
              // We should add to callers one node from the loop instead of removed one.
              callers.add((FunctionEntryNode) representativeNode);
            }
          }
        }
      }
      foundedRecursionLoop.add(representativeNode);
      functionsCalledFromTheLoop.removeAll(foundedRecursionLoop);
      //Add to chosen node (representative) all function calls from removed functions
      workCopyOfInnerFunctionCalls.get(representativeNode).addAll(functionsCalledFromTheLoop);

    } while (!workCopyOfCFANodes.isEmpty()) ;

    //Try to optimize the memory
    Map<CFANode, ImmutableSet<ReferencedVariable>> immutableVariablesMap = new HashMap<>();
    Map<CFANode, ImmutableSet<CFANode>> immutableNodesMap = new HashMap<>();
    //Resolve loop mapping
    for (CFANode node : loopMapping.keySet()) {
      CFANode mappedNode = loopMapping.get(node);
      while (loopMapping.containsKey(mappedNode)) {
        mappedNode = loopMapping.get(mappedNode);
      }
      /* We put the same object, because new Block() makes copy of these maps,
       * so we do not care about this equality
       */
      ImmutableSet<ReferencedVariable> resultVars;
      ImmutableSet<CFANode> resultNodes;
      if (!immutableVariablesMap.containsKey(mappedNode)) {
        resultVars = ImmutableSet.copyOf(referencedVariablesMap.get(mappedNode));
        immutableVariablesMap.put(mappedNode, resultVars);
      } else {
        resultVars = immutableVariablesMap.get(mappedNode);
      }
      immutableVariablesMap.put(node, resultVars);
      if (!immutableNodesMap.containsKey(mappedNode)) {
        resultNodes = ImmutableSet.copyOf(blockNodesMap.get(mappedNode));
        immutableNodesMap.put(mappedNode, resultNodes);
      } else {
        resultNodes = immutableNodesMap.get(mappedNode);
      }
      immutableNodesMap.put(node, resultNodes);
    }

    //now we can create the Blocks for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<>(returnNodesMap.keySet().size());
    for (CFANode key : returnNodesMap.keySet()) {
      if (immutableVariablesMap.containsKey(key)) {
        assert immutableNodesMap.containsKey(key);
        blocks.add(new Block(immutableVariablesMap.get(key), callNodesMap.get(key), returnNodesMap.get(key), immutableNodesMap.get(key)));
      } else {
        blocks.add(new Block(ImmutableSet.copyOf(referencedVariablesMap.get(key)), callNodesMap.get(key),
            returnNodesMap.get(key), ImmutableSet.copyOf(blockNodesMap.get(key))));
      }
    }
    return new BlockPartitioning(blocks, mainFunction);
  }

  private void joinFunctionPartitioning(CFANode node, CFANode caller) {
    Set<ReferencedVariable> functionVars = referencedVariablesMap.get(caller);
    Set<CFANode> functionBody = blockNodesMap.get(caller);
    referencedVariablesMap.get(node).addAll(functionVars);
    blockNodesMap.get(node).addAll(functionBody);
  }

  /**
   * @param nodes Nodes from which Block should be created; if the set of nodes contains inner function calls, the called function body should NOT be included
   */

  public void addBlock(Set<CFANode> nodes, CFANode mainFunction) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes, mainFunction);
    Set<CFANode> returnNodes = collectReturnNodes(nodes, mainFunction);
    Set<FunctionEntryNode> innerFunctionCalls = collectInnerFunctionCalls(nodes);

    if (callNodes.isEmpty()) {
      /* What shall we do with function, which is not called from anywhere?
       * I remove it.
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
