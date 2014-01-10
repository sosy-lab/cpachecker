/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import static org.sosy_lab.cpachecker.util.CFAUtils.findLoops;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;


public class CFASingleLoopTransformation {

  private final LogManager logger;

  private final Configuration config;

  public CFASingleLoopTransformation(LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException {
    this.logger = pLogger;
    this.config = pConfig;
  }

  public ImmutableCFA apply(CFA pInputCFA) throws InvalidConfigurationException {
    // Create new main function entry initializing the program counter
    FunctionEntryNode oldMainFunctionEntryNode = pInputCFA.getMainFunction();
    AFunctionDeclaration mainFunctionDeclaration = oldMainFunctionEntryNode.getFunctionDefinition();
    FileLocation mainLocation = mainFunctionDeclaration.getFileLocation();
    String mainFunctionName = oldMainFunctionEntryNode.getFunctionName();
    FunctionEntryNode start = oldMainFunctionEntryNode instanceof CFunctionEntryNode ?
        new CFunctionEntryNode(0, (CFunctionDeclaration) mainFunctionDeclaration, oldMainFunctionEntryNode.getExitNode(), oldMainFunctionEntryNode.getFunctionParameterNames()) :
        new JMethodEntryNode(0, (JMethodDeclaration) mainFunctionDeclaration, oldMainFunctionEntryNode.getExitNode(), oldMainFunctionEntryNode.getFunctionParameterNames());
    CFANode loopHead = new CFANode(0, mainFunctionName);

    // Declare program counter and initialize it to 0
    String pcVarName = "___pc";
    int pc = 0;
    CDeclaration pcDeclaration = new CVariableDeclaration(mainLocation, true, CStorageClass.AUTO, CNumericTypes.INT, pcVarName, pcVarName, pcVarName,
        new CInitializerExpression(mainLocation, new CIntegerLiteralExpression(mainLocation, CNumericTypes.INT, BigInteger.valueOf(pc))));
    CIdExpression pcIdExpression = new CIdExpression(mainLocation, CNumericTypes.INT, pcVarName, pcDeclaration);
    CFAEdge pcDeclarationEdge = new CDeclarationEdge(String.format("int %s = %d;", pcVarName, pc), 0, start, loopHead, pcDeclaration);
    start.addLeavingEdge(pcDeclarationEdge);
    loopHead.addEnteringEdge(pcDeclarationEdge);

    Queue<CFANode> nodes = new ArrayDeque<>(getAllNodes(pInputCFA));

    Map<Integer, CFANode> newPredecessorsToPC = new LinkedHashMap<>();
    Map<Integer, CFANode> newSuccessorsToPC = new LinkedHashMap<>();
    Map<CFANode, CFANode> globalNewToOld = new HashMap<>();
    Map<CFANode, CFANode> entryNodeConnectors = new HashMap<>();
    globalNewToOld.put(oldMainFunctionEntryNode, start);

    // Create new nodes and assume edges based on program counter values leading to the new nodes
    Set<CFANode> visited = new HashSet<>();
    while (!nodes.isEmpty()) {
      CFANode subgraphRoot = nodes.poll();

      // Mark an unvisited node as visited or discard a visited node
      if (!visited.add(subgraphRoot)) {
        continue;
      }

      /*
       * Handle the old main entry node: There is a new main entry node and
       * there must only be one main entry node, so while the old node must be
       * represented in the transformed graph, it must no longer be a main
       * entry node.
       */
      boolean isOldMainEntryNode = subgraphRoot.equals(oldMainFunctionEntryNode);
      if (isOldMainEntryNode) {
        subgraphRoot = new CFANode(subgraphRoot.getLineNumber(), subgraphRoot.getFunctionName());
        replaceInStructure(oldMainFunctionEntryNode, subgraphRoot);
        newSuccessorsToPC.put(0, getOrCreateNewFromOld(subgraphRoot, globalNewToOld));
      }

      // Get an acyclic sub graph
      Set<CFANode> subgraph = new LinkedHashSet<>();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(subgraphRoot);
      subgraph.add(subgraphRoot);
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        for (int i = 0; i < current.getNumLeavingEdges(); ++i) {
          CFAEdge edge = current.getLeavingEdge(i);
          CFANode next = edge.getSuccessor();

          // Eliminate a direct self edge by introducing a dummy node in between
          if (next == current) {
            removeFromNodes(edge);

            CFANode dummy = new CFANode(current.getLineNumber(), current.getFunctionName());
            BlankEdge dummyEdge = new BlankEdge("", edge.getLineNumber(), current, dummy, "");
            addToNodes(dummyEdge);

            CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(edge, dummy, next, globalNewToOld);
            addToNodes(replacementEdge);

            next = dummy;
            nodes.add(dummy);
            edge = dummyEdge;
          }

          // Add a node to the subgraph if no cycle is introduced by it
          if (!containsAny(getNodesBetween(subgraphRoot, current, subgraph), getSuccessors(next)) && visited.add(next)) {
            subgraph.add(next);
            waitlist.add(next);
          } else if (!subgraph.contains(next)) {
            // Cut off the edge leaving the subgraph
            removeFromNodes(edge);

            /*
             * Copy the old edge but connect it to a new successor which will
             * become part of the subgraph
             */
            Map<CFANode, CFANode> tmpNewToOld = new HashMap<>();
            tmpNewToOld.put(next, next);
            CFAEdge connectionEdge = copyCFAEdgeWithNewNodes(edge, tmpNewToOld);
            CFANode connectionNode = connectionEdge.getPredecessor();
            addToNodes(connectionEdge);

            /*
             * Record that this new node needs a connection into a subgraph via
             * the old node which becomes an entry node into a subgraph.
             */
            entryNodeConnectors.put(next, connectionNode);

            // Create the edge in the new graph
            CFAEdge newConnectionEdge = copyCFAEdgeWithNewNodes(connectionEdge, getOrCreateNewFromOld(connectionNode, globalNewToOld), getOrCreateNewFromOld(next, globalNewToOld), globalNewToOld);
            addToNodes(newConnectionEdge);

            // Compute the program counter for the replaced edge and map the nodes to it
            int pcToSuccessor = ++pc;
            newSuccessorsToPC.put(pcToSuccessor, getOrCreateNewFromOld(connectionNode, globalNewToOld));
            newPredecessorsToPC.put(pcToSuccessor, getOrCreateNewFromOld(current, globalNewToOld));
          }
        }
      }

      // Copy the subgraph
      Set<CFANode> newSubgraph = new LinkedHashSet<>();
      for (CFANode oldNode : subgraph) {
        CFANode newNode = getOrCreateNewFromOld(oldNode, globalNewToOld);
        newSubgraph.add(newNode);
      }
      for (CFANode oldNode : subgraph) {
        for (int leavingEdgeIndex = 0; leavingEdgeIndex < oldNode.getNumLeavingEdges(); ++leavingEdgeIndex) {
          CFAEdge oldEdge = oldNode.getLeavingEdge(leavingEdgeIndex);
          assert subgraph.contains(oldEdge.getSuccessor()) : "None of the nodes in the subgraph must have an edge leaving the subgraph at this point";
          CFAEdge newEdge = copyCFAEdgeWithNewNodes(oldEdge, globalNewToOld);
          newEdge.getPredecessor().addLeavingEdge(newEdge);
          newEdge.getSuccessor().addEnteringEdge(newEdge);
        }
      }
    }

    /*
     * Connect the subgraph tails to their successors via the loop head by
     * setting the corresponding program counter values.
     */
    connectSubgraphLeavingNodesToLoopHead(loopHead, newPredecessorsToPC, pcIdExpression, mainLocation);

    // Connect the subgraph entry nodes by assuming the program counter values
    connectLoopHeadToSubgraphEntryNodes(loopHead, newSuccessorsToPC, pcIdExpression, mainLocation,
        new CBinaryExpressionBuilder(pInputCFA.getMachineModel(), logger));

    // Build the CFA from the syntactically reachable nodes
    return buildCFA(start, pInputCFA.getMachineModel(), pInputCFA.getLanguage());
  }

  private ImmutableCFA buildCFA(FunctionEntryNode pStartNode, MachineModel pMachineModel, Language pLanguage) throws InvalidConfigurationException {
    Map<String, FunctionEntryNode> functions = null;
    SortedSetMultimap<String, CFANode> allNodes = null;
    boolean changed = true;
    while (changed) {
      changed = false;
      functions = new HashMap<>();
      allNodes = TreeMultimap.create();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(pStartNode);
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        String functionName = current.getFunctionName();
        if (allNodes.put(functionName, current)) {
          waitlist.addAll(FluentIterable.from(getSuccessors(current)).toList());
          if (current instanceof FunctionEntryNode) {
            functions.put(functionName, (FunctionEntryNode) current);
          }
        }
      }

      Set<String> functionsToRemove = new HashSet<>();
      for (String function : allNodes.keys()) {
        if (!functions.containsKey(function)) {
          changed = true;
          functionsToRemove.add(function);
        }
      }
      for (String function : functionsToRemove) {
        for (CFANode removed : allNodes.removeAll(function)) {
          removeFromGraph(removed);
        }
      }
    }

    // Instantiate the transformed graph in a preliminary form
    MutableCFA cfa = new MutableCFA(pMachineModel, functions, allNodes, pStartNode, pLanguage);

    // Assign reverse post order ids to the control flow nodes
    Collection<CFANode> nodesWithNoIdAssigned = getAllNodes(cfa);
    for (CFANode n : nodesWithNoIdAssigned) {
      n.setReversePostorderId(-1);
    }
    while (!nodesWithNoIdAssigned.isEmpty()) {
      CFAReversePostorder sorter = new CFAReversePostorder();
      sorter.assignSorting(nodesWithNoIdAssigned.iterator().next());
      nodesWithNoIdAssigned = FluentIterable.from(nodesWithNoIdAssigned).filter(new Predicate<CFANode>() {

        @Override
        public boolean apply(@Nullable CFANode pArg0) {
          if (pArg0 == null) {
            return false;
          }
          return pArg0.getReversePostorderId() < 0;
        }

      }).toList();
    }

    // Get information about the loop structure
    Optional<ImmutableMultimap<String, Loop>> loopStructure =
        getLoopStructure(pStartNode.getFunctionName(), new TreeSet<>(allNodes.values()), pLanguage, logger);

    // Get information about variables, required by some analyses
    final Optional<VariableClassification> varClassification
        = loopStructure.isPresent()
        ? Optional.of(new VariableClassification(cfa, config, logger, loopStructure.get()))
        : Optional.<VariableClassification>absent();

    // Finalize the transformed CFA
    return cfa.makeImmutableCFA(loopStructure, varClassification);
  }

  private void removeFromGraph(CFANode pToRemove) {
    while (pToRemove.getNumEnteringEdges() > 0) {
      removeFromNodes(pToRemove.getEnteringEdge(0));
    }
    while (pToRemove.getNumLeavingEdges() > 0) {
      removeFromNodes(pToRemove.getLeavingEdge(0));
    }
  }

  private static void removeFromNodes(CFAEdge pEdge) {
    pEdge.getPredecessor().removeLeavingEdge(pEdge);
    pEdge.getSuccessor().removeEnteringEdge(pEdge);
  }

  private static void addToNodes(CFAEdge pEdge) {
    pEdge.getPredecessor().addLeavingEdge(pEdge);
    pEdge.getSuccessor().addEnteringEdge(pEdge);
  }

  private static Set<CFANode> getNodesBetween(CFANode source, CFANode target, Set<CFANode> subgraph) {
    Set<CFANode> visited = new HashSet<>();
    Set<CFANode> result = new HashSet<>();
    Queue<List<CFANode>> waitlist = new ArrayDeque<>();
    waitlist.add(Collections.singletonList(source));
    while (!waitlist.isEmpty()) {
      List<CFANode> current = waitlist.poll();
      CFANode head = getLast(current);
      if (subgraph.contains(head) && visited.add(head)) {
        if (head.equals(target)) {
          result.addAll(current);
        } else {
          for (CFANode successor : getSuccessors(head)) {
            List<CFANode> succ = new ArrayList<>(current);
            succ.add(successor);
            waitlist.add(succ);
          }
        }
      }
    }
    return result;
  }

  private static void connectSubgraphLeavingNodesToLoopHead(CFANode pLoopHead,
      Map<Integer, CFANode> pNewPredecessorsToPC,
      CIdExpression pPCIdExpression,
      FileLocation pMainLocation) {
    for (Map.Entry<Integer, CFANode> newPredecessorToPC : pNewPredecessorsToPC.entrySet()) {
      int pcToSet = newPredecessorToPC.getKey();
      CFANode subgraphPredecessor = newPredecessorToPC.getValue();
      CStatement statement = new CExpressionAssignmentStatement(pMainLocation, pPCIdExpression,
          new CIntegerLiteralExpression(pMainLocation, CNumericTypes.INT, BigInteger.valueOf(pcToSet)));
      CFAEdge edgeToLoopHead = new CStatementEdge(String.format("%s = %d;", pPCIdExpression.getName(), pcToSet),
          statement, subgraphPredecessor.getLineNumber(), subgraphPredecessor, pLoopHead);
      edgeToLoopHead.getPredecessor().addLeavingEdge(edgeToLoopHead);
      edgeToLoopHead.getSuccessor().addEnteringEdge(edgeToLoopHead);
    }
  }

  /**
   * Connects subgraph entry nodes to the loop head via program counter value assume edges.
   *
   * @param pPcToNewSuccessorMapping the mapping of subgraph entry nodes to the
   *  corresponding program counter values.
   * @param pLoopHead the loop head.
   * @param pPCIdExpression the CIdExpression used for the program counter variable.
   * @param pMainLocation the location of the main function.
   * @param pExpressionBuilder the CExpressionBuilder used to build the assume edges.
   */
  private static void connectLoopHeadToSubgraphEntryNodes(CFANode pLoopHead,
      Map<Integer, CFANode> pPcToNewSuccessorMapping,
      CIdExpression pPCIdExpression,
      FileLocation pMainLocation,
      CBinaryExpressionBuilder pExpressionBuilder) {
    List<CAssumeEdge> toAdd = new ArrayList<>();
    CFANode decisionTreeNode = pLoopHead;
    for (Entry<Integer, CFANode> pcToNewSuccessorMapping : pPcToNewSuccessorMapping.entrySet()) {
      CFANode newSuccessor = pcToNewSuccessorMapping.getValue();
      int pcToSet = pcToNewSuccessorMapping.getKey();
      // Connect thKeyequence to the loop header assuming the program counter value
      CFANode newDecisionTreeNode = new CFANode(0, decisionTreeNode.getFunctionName());
      CExpression assumePCExpression = pExpressionBuilder.buildBinaryExpression(
          pPCIdExpression,
          new CIntegerLiteralExpression(pMainLocation, CNumericTypes.INT, BigInteger.valueOf(pcToSet)),
          BinaryOperator.EQUALS);
      CAssumeEdge toSequence = new CAssumeEdge(String.format("%s == %d",  pPCIdExpression.getName(), pcToSet), 0, decisionTreeNode,
          newSuccessor, assumePCExpression, true);
      CAssumeEdge toNewDecisionTreeNode = new CAssumeEdge(String.format("!(%s == %d)",  pPCIdExpression.getName(), pcToSet),
          0, decisionTreeNode, newDecisionTreeNode,
          assumePCExpression, false);
      toAdd.add(toSequence);
      toAdd.add(toNewDecisionTreeNode);
      decisionTreeNode = newDecisionTreeNode;
    }
    /*
     * At the end of the decision tree, there is now an unreachable dangling
     * assume edge node. This does not hurt, but can be optimized out:
     */
    if (!toAdd.isEmpty()) {
      // The last edge is superfluous
      removeLast(toAdd);
      /*
       * The last positive edge is thus the only relevant edge after the edge
       * leading to its predecessor
       */
      CFAEdge lastTrueEdge = removeLast(toAdd);
      /*
       * The successor of the edge leading to the predecessor of the last
       * positive edge can thus be set to the last relevant node
       */
      if (!toAdd.isEmpty()) {
        CAssumeEdge secondToLastFalseEdge = removeLast(toAdd);
        CAssumeEdge newLastEdge = new CAssumeEdge(secondToLastFalseEdge.getRawStatement(), 0, secondToLastFalseEdge.getPredecessor(), lastTrueEdge.getSuccessor(),
            secondToLastFalseEdge.getExpression(), false);
        toAdd.add(newLastEdge);
      } else {
        BlankEdge edge = new BlankEdge("", 0, pLoopHead, lastTrueEdge.getSuccessor(), "");
        edge.getPredecessor().addLeavingEdge(edge);
        edge.getSuccessor().addEnteringEdge(edge);
      }
    }
    // Add the edges connecting the real nodes with the loop head
    for (CFAEdge edge : toAdd) {
      addToNodes(edge);
    }

  }

  /**
   * Gets the last element of a list.
   *
   * @param pList the list to get the element from.
   * @return the last element of the list.
   *
   * @throws NoSuchElementException if the list is empty.
   */
  private static <T> T getLast(List<T> pList) {
    int index = pList.size() - 1;
    if (index < 0) {
      throw new NoSuchElementException();
    }
    return pList.get(index);
  }

  /**
   * Removes the last element of a list.
   *
   * @param pList the list to get the element from.
   * @return the last element of the list.
   *
   * @throws NoSuchElementException if the list is empty.
   */
  private static <T> T removeLast(List<T> pList) {
    int index = pList.size() - 1;
    if (index < 0) {
      throw new NoSuchElementException();
    }
    return pList.remove(index);
  }

  /**
   * Gets all nodes of the given control flow automaton in breath first search
   * order with the function entry nodes as start nodes for the search.
   *
   * @param pInputCFA the control flow automaton to extract the nodes from.
   * @return all nodes of the given control flow automaton.
   */
  private static Set<CFANode> getAllNodes(CFA pInputCFA) {

    // First, determine reachable functions
    final Set<String> functionNames = new HashSet<>();
    Set<CFANode> nodes = new LinkedHashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.add(pInputCFA.getMainFunction());
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (nodes.add(current)) {
        functionNames.add(current.getFunctionName());
        waitlist.addAll(FluentIterable.from(getSuccessors(current)).toList());
      }
    }

    // Now get all nodes of all reachable functions (even if potentially unreachable)
    nodes.clear();
    waitlist.clear();
    waitlist.add(pInputCFA.getMainFunction());
    waitlist.addAll(FluentIterable.from(pInputCFA.getAllFunctionHeads()).filter(new Predicate<CFANode>() {

      @Override
      public boolean apply(@Nullable CFANode pArg0) {
        return pArg0 != null && functionNames.contains(pArg0.getFunctionName());
      }

    }).toList());
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (nodes.add(current)) {
        waitlist.addAll(FluentIterable.from(getSuccessors(current)).toList());
      }
    }
    return nodes;
  }

  private static <T> boolean containsAny(Collection<T> pCollection, Iterable<T> pElements) {
    for (T element : pElements) {
      if (pCollection.contains(element)) {
        return true;
      }
    }
    return false;
  }

  private static Iterable<CFANode> getSuccessors(final CFANode pNode) {
    return new Iterable<CFANode>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new Iterator<CFANode>() {

          private int index = 0;

          @Override
          public boolean hasNext() {
            return index >= 0 && index < pNode.getNumLeavingEdges();
          }

          @Override
          public CFANode next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            CFANode next = pNode.getLeavingEdge(index).getSuccessor();
            ++index;
            return next;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

        };
      }
    };
  }

  private void replaceInStructure(CFANode pOldNode, CFANode pNewNode) {
    Map<CFANode, CFANode> newToOld = new HashMap<>();
    newToOld.put(pOldNode, pNewNode);
    List<CFAEdge> edgesToAdd = new ArrayList<>();
    List<CFAEdge> edgesToRemove = new ArrayList<>();
    for (int i = 0; i < pOldNode.getNumLeavingEdges(); ++i) {
      CFAEdge oldEdge = pOldNode.getLeavingEdge(i);
      newToOld.put(oldEdge.getSuccessor(), oldEdge.getSuccessor());
      CFAEdge newEdge = copyCFAEdgeWithNewNodes(oldEdge, newToOld);
      edgesToAdd.add(newEdge);
      edgesToRemove.add(oldEdge);
    }
    for (int i = 0; i < pOldNode.getNumEnteringEdges(); ++i) {
      CFAEdge oldEdge = pOldNode.getEnteringEdge(i);
      newToOld.put(oldEdge.getPredecessor(), oldEdge.getPredecessor());
      CFAEdge newEdge = copyCFAEdgeWithNewNodes(oldEdge, newToOld);
      edgesToAdd.add(newEdge);
      edgesToRemove.add(oldEdge);
    }
    for (CFAEdge edge : edgesToRemove) {
      edge.getPredecessor().removeLeavingEdge(edge);
      edge.getSuccessor().removeEnteringEdge(edge);
    }
    for (CFAEdge edge : edgesToAdd) {
      edge.getPredecessor().addLeavingEdge(edge);
      edge.getSuccessor().addEnteringEdge(edge);
    }
  }

  private CFANode getOrCreateNewFromOld(CFANode pNode, Map<CFANode, CFANode> pNewToOldMapping) {
    CFANode result = pNewToOldMapping.get(pNode);
    if (result != null) {
      return result;
    }
    int lineNumber = pNode.getLineNumber();
    String functionName = pNode.getFunctionName();
    if (pNode instanceof org.sosy_lab.cpachecker.cfa.model.c.CLabelNode) {
      result = new org.sosy_lab.cpachecker.cfa.model.c.CLabelNode(lineNumber, functionName, ((org.sosy_lab.cpachecker.cfa.model.c.CLabelNode) pNode).getLabel());
    } else if (pNode instanceof org.sosy_lab.cpachecker.cfa.model.CLabelNode) {
      result = new org.sosy_lab.cpachecker.cfa.model.CLabelNode(lineNumber, functionName, ((org.sosy_lab.cpachecker.cfa.model.CLabelNode) pNode).getLabel());
    } else if (pNode instanceof CFunctionEntryNode) {
      CFunctionEntryNode functionEntryNode = (CFunctionEntryNode) pNode;
      FunctionExitNode functionExitNode = (FunctionExitNode) getOrCreateNewFromOld(functionEntryNode.getExitNode(), pNewToOldMapping);
      if (functionExitNode.getEntryNode() == null) {
        functionExitNode.setEntryNode(functionEntryNode);
      }
      result = new CFunctionEntryNode(lineNumber, functionEntryNode.getFunctionDefinition(),
          functionExitNode, functionEntryNode.getFunctionParameterNames());
    } else if (pNode instanceof JMethodEntryNode) {
      JMethodEntryNode methodEntryNode = (JMethodEntryNode) pNode;
      FunctionExitNode functionExitNode = (FunctionExitNode) getOrCreateNewFromOld(methodEntryNode.getExitNode(), pNewToOldMapping);
      if (functionExitNode.getEntryNode() == null) {
        functionExitNode.setEntryNode(methodEntryNode);
      }
      result = new JMethodEntryNode(lineNumber, methodEntryNode.getFunctionDefinition(),
          functionExitNode, methodEntryNode.getFunctionParameterNames());
    } else if (pNode instanceof FunctionExitNode) {
      FunctionExitNode functionExitNode = new FunctionExitNode(lineNumber, functionName);
      pNewToOldMapping.put(pNode, functionExitNode);
      if (functionExitNode.getEntryNode() == null) {
        FunctionEntryNode functionEntryNode = (FunctionEntryNode) getOrCreateNewFromOld(((FunctionExitNode) pNode).getEntryNode(), pNewToOldMapping);
        if (functionExitNode.getEntryNode() == null) {
          functionExitNode.setEntryNode(functionEntryNode);
        }
      }
      result = functionExitNode;
    } else {
      result = new CFANode(lineNumber, functionName);
    }
    pNewToOldMapping.put(pNode, result);
    return result;
  }

  private CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, CFANode pNewPredecessor, CFANode pNewSuccessor, final Map<CFANode, CFANode> pNewToOldMapping) {
    String rawStatement = pEdge.getRawStatement();
    int lineNumber = pEdge.getLineNumber();
    switch (pEdge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      return new CAssumeEdge(rawStatement, lineNumber, pNewPredecessor, pNewSuccessor, assumeEdge.getExpression(), assumeEdge.getTruthAssumption());
    case BlankEdge:
      return new BlankEdge(rawStatement, lineNumber, pNewPredecessor, pNewSuccessor, pEdge.getDescription());
    case DeclarationEdge:
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      return new CDeclarationEdge(rawStatement, lineNumber, pNewPredecessor, pNewSuccessor, declarationEdge.getDeclaration());
    case FunctionCallEdge:
      if (!(pNewSuccessor instanceof FunctionEntryNode)) {
        throw new IllegalArgumentException("The successor of a function call edge must be a function entry node.");
      }
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pEdge;
      Optional<CFunctionCall> cFunctionCall = functionCallEdge.getRawAST();
      return new CFunctionCallEdge(rawStatement, lineNumber, pNewPredecessor, (CFunctionEntryNode) pNewSuccessor, cFunctionCall.orNull(), functionCallEdge.getSummaryEdge());
    case FunctionReturnEdge:
      if (!(pNewPredecessor instanceof FunctionExitNode)) {
        throw new IllegalArgumentException("The predecessor of a function return edge must be a function exit node.");
      }
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) pEdge;
      CFunctionSummaryEdge functionSummaryEdge = (CFunctionSummaryEdge) copyCFAEdgeWithNewNodes(functionReturnEdge.getSummaryEdge(), pNewToOldMapping);
      functionSummaryEdge.getPredecessor().addLeavingSummaryEdge(functionSummaryEdge);
      functionSummaryEdge.getSuccessor().addEnteringSummaryEdge(functionSummaryEdge);
      return new CFunctionReturnEdge(lineNumber, (FunctionExitNode) pNewPredecessor, pNewSuccessor, functionSummaryEdge);
    case MultiEdge:
      MultiEdge multiEdge = (MultiEdge) pEdge;
      return new MultiEdge(pNewPredecessor, pNewSuccessor, FluentIterable.from(multiEdge.getEdges()).transform(new Function<CFAEdge, CFAEdge>() {

        @Override
        @Nullable
        public CFAEdge apply(@Nullable CFAEdge pOldEdge) {
          if (pOldEdge == null) {
            return null;
          }
          return copyCFAEdgeWithNewNodes(pOldEdge, pNewToOldMapping);
        }


      }).toList());
    case ReturnStatementEdge:
      if (!(pNewSuccessor instanceof FunctionExitNode)) {
        throw new IllegalArgumentException("The successor of a return statement edge must be a function exit node.");
      }
      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pEdge;
      Optional<CReturnStatement> cReturnStatement = returnStatementEdge.getRawAST();
      return new CReturnStatementEdge(rawStatement, cReturnStatement.orNull(), lineNumber, pNewPredecessor, (FunctionExitNode) pNewSuccessor);
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      return new CStatementEdge(rawStatement, statementEdge.getStatement(), lineNumber, pNewPredecessor, pNewSuccessor);
    case CallToReturnEdge:
      CFunctionSummaryEdge cFunctionSummaryEdge = (CFunctionSummaryEdge) pEdge;
      return new CFunctionSummaryEdge(rawStatement, lineNumber, pNewPredecessor, pNewSuccessor, cFunctionSummaryEdge.getExpression());
    default:
      throw new IllegalArgumentException("Unsupported edge type: " + pEdge.getEdgeType());
    }
  }

  private CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, final Map<CFANode, CFANode> pNewToOldMapping) {
    CFANode newPredecessor = getOrCreateNewFromOld(pEdge.getPredecessor(), pNewToOldMapping);
    CFANode newSuccessor = getOrCreateNewFromOld(pEdge.getSuccessor(), pNewToOldMapping);
    return copyCFAEdgeWithNewNodes(pEdge, newPredecessor, newSuccessor, pNewToOldMapping);
  }

  private Optional<ImmutableMultimap<String, Loop>> getLoopStructure(String pMainFunctionName, SortedSet<CFANode> pAllNodes, Language pLanguage, LogManager pLogger) {
    try {
      ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
      loops.putAll(pMainFunctionName, findLoops(pAllNodes, pLanguage));
      return Optional.of(loops.build());

    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can continue
      pLogger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");

    } catch (OutOfMemoryError e) {
      pLogger.logUserException(Level.WARNING, e,
          "Could not analyze loop structure of program due to memory problems");
    }
    return Optional.absent();
  }

}
