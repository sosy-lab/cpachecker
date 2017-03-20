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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;

/**
 *  Class implements more intelligent partitioning building,
 *  but it is still not working for loops, because they may have more then one entry node into block
 *  Current method much more faster handles difficult structures, containing recursive calls,
 *  because it doesn't wait a fixpoint
 */
public class ExtendedBlockPartitioningBuilder extends BlockPartitioningBuilder {

  private final LockTransferRelation ltransfer;

  public ExtendedBlockPartitioningBuilder(LockTransferRelation t) {
    ltransfer = t;
  }

  @Override
  public BlockPartitioning build(CFA cfa) {

    SetMultimap<CFANode, FunctionEntryNode> workCopyOfInnerFunctionCalls = HashMultimap.create();

    /* We chose one representative from every loop.
     * This map stores for the node its representative.
     */
    Map<CFANode, CFANode> loopMapping = new HashMap<>();

    //Deep clone, because we will delete nodes
    for (Entry<CFANode, Set<FunctionEntryNode>> entry : innerFunctionCallsMap.entrySet()) {
      workCopyOfInnerFunctionCalls.putAll(entry.getKey(), entry.getValue());
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
    Map<CFANode, ImmutableSet<LockIdentifier>> immutableLocksMap = new HashMap<>();
    //Resolve loop mapping
    for (Entry<CFANode, CFANode> nodeMapping : loopMapping.entrySet()) {
      CFANode node = nodeMapping.getKey();
      CFANode mappedNode = nodeMapping.getValue();
      while (loopMapping.containsKey(mappedNode)) {
        mappedNode = loopMapping.get(mappedNode);
      }
      /* We put the same object, because new Block() makes copy of these maps,
       * so we do not care about this equality
       */
      ImmutableSet<ReferencedVariable> resultVars;
      ImmutableSet<CFANode> resultNodes;
      ImmutableSet<LockIdentifier> resultLocks;
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
      if (!immutableLocksMap.containsKey(mappedNode)) {
        resultLocks = ImmutableSet.copyOf(capturedLocksMap.get(mappedNode));
        immutableLocksMap.put(mappedNode, resultLocks);
      } else {
        resultLocks = immutableLocksMap.get(mappedNode);
      }
      immutableLocksMap.put(node, resultLocks);
    }

    //now we can create the Blocks for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<>(returnNodesMap.keySet().size());
    for (Entry<CFANode, Set<CFANode>> returnNodesEntry : returnNodesMap.entrySet()) {
      CFANode key = returnNodesEntry.getKey();
      if (immutableVariablesMap.containsKey(key)) {
        assert immutableNodesMap.containsKey(key);
        blocks.add(new Block(immutableVariablesMap.get(key), callNodesMap.get(key), returnNodesMap.get(key), immutableNodesMap.get(key), immutableLocksMap.get(key)));
      } else {
        blocks.add(new Block(ImmutableSet.copyOf(referencedVariablesMap.get(key)), callNodesMap.get(key),
            returnNodesEntry.getValue(), ImmutableSet.copyOf(blockNodesMap.get(key)), ImmutableSet.copyOf(capturedLocksMap.get(key))));
      }
    }
    return new BlockPartitioning(blocks, cfa.getMainFunction());
  }

  private void joinFunctionPartitioning(CFANode node, CFANode caller) {
    Set<ReferencedVariable> functionVars = referencedVariablesMap.get(caller);
    Set<CFANode> functionBody = blockNodesMap.get(caller);
    referencedVariablesMap.get(node).addAll(functionVars);
    blockNodesMap.get(node).addAll(functionBody);
  }

  @Override
  public void addBlock(Set<CFANode> nodes, CFANode blockHead) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes);
    Set<CFANode> returnNodes = collectReturnNodes(nodes);
    Set<FunctionEntryNode> innerFunctionCalls = collectInnerFunctionCalls(nodes);
    Set<LockIdentifier> innerLocks = Sets.newHashSet();
    if (ltransfer != null) {
      innerLocks = collectLocks(nodes);
    }

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
    if (registerNode == null) {
      //It means, that there is no entry in this block. Don't add it
      return;
    }
    referencedVariablesMap.put(registerNode, referencedVariables);
    callNodesMap.put(registerNode, callNodes);
    returnNodesMap.put(registerNode, returnNodes);
    innerFunctionCallsMap.put(registerNode, innerFunctionCalls);
    blockNodesMap.put(registerNode, nodes);
    capturedLocksMap.put(registerNode, innerLocks);
  }

  private Set<LockIdentifier> collectLocks(Set<CFANode> pNodes) {
    Set<LockIdentifier> result = new HashSet<>();
    if (ltransfer == null) {
      return Collections.emptySet();
    }
    for (CFANode node : pNodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge e = node.getLeavingEdge(i);
        result.addAll(ltransfer.getAffectedLocks(e));
      }
    }
    return result;
  }
}
