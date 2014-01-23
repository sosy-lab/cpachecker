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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Instances of this class are used to apply single loop transformation to
 * control flow automata.
 */
@Options
public class CFASingleLoopTransformation {

  /**
   * The name of the program counter variable introduced by the transformation.
   */
  public static final String PROGRAM_COUNTER_VAR_NAME = "___pc";

  /**
   * The description of the dummy edges used.
   */
  private static final String DUMMY_EDGE = "DummyEdge";

  /**
   * The log manager.
   */
  private final LogManager logger;

  /**
   * The configuration used.
   */
  private final Configuration config;

  /**
   * The shutdown notifier used.
   */
  private final OptionalShutdownNotifier shutdownNotifier;

  @Option(name="cfa.transformIntoSingleLoop.omitExplicitLastProgramCounterAssumption",
      description="Single loop transformation builds a decision tree based on" +
    		" the program counter values. This option causes the last program" +
    		" counter value not to be explicitly assumed in the decision tree," +
    		" so that it is only indirectly represented by the assumption of" +
    		" falsehood for all other assumptions in the decision tree.")
  private boolean omitExplicitLastProgramCounterAssumption = true;

  @Option(name="cfa.transformIntoSingleLoop.initialSubgraphAsPrefix",
      description="Setting this option prefixes the loop with the initial" +
      		"subgraph instead of treating it like the other subgraphs as a " +
      		"leaf of the program counter value decision tree.")
  private boolean initialSubgraphAsPrefix = true;

  /**
   * Creates a new single loop transformer.
   *
   * @param pLogger the log manager to be used.
   * @param pConfig the configuration used.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  private CFASingleLoopTransformation(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    this.logger = pLogger;
    this.config = pConfig;
    config.inject(this);
    this.shutdownNotifier = new OptionalShutdownNotifier(pShutdownNotifier);
  }

  /**
   * Gets a single loop transformation strategy using the given log manager and configuration.
   *
   * @param pLogger the log manager used.
   * @param pConfig the configuration used.
   *
   * @return a single loop transformation strategy.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  static CFASingleLoopTransformation getSingleLoopTransformation(LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException {
    return new CFASingleLoopTransformation(pLogger, pConfig, null);
  }

  /**
   * Gets a single loop transformation strategy using the given log manager, configuration and optional shutdown notifier.
   *
   * @param pLogger the log manager used.
   * @param pConfig the configuration used.
   * @param pShutdownNotifier the optional shutdown notifier.
   *
   * @return a single loop transformation strategy.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  static CFASingleLoopTransformation getSingleLoopTransformation(LogManager pLogger, Configuration pConfig, @Nullable ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    return new CFASingleLoopTransformation(pLogger, pConfig, pShutdownNotifier);
  }

  /**
   * Applies the single loop transformation to the given CFA.
   *
   * @param pInputCFA the control flow automaton to be transformed.
   *
   * @return a new CFA with at most one loop.
   * @throws InvalidConfigurationException
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  public ImmutableCFA apply(CFA pInputCFA) throws InvalidConfigurationException, InterruptedException {
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
    String pcVarName = PROGRAM_COUNTER_VAR_NAME;
    int pc = 0;
    CDeclaration pcDeclaration = new CVariableDeclaration(mainLocation, true, CStorageClass.AUTO, CNumericTypes.INT, pcVarName, pcVarName, pcVarName,
        new CInitializerExpression(mainLocation, new CIntegerLiteralExpression(mainLocation, CNumericTypes.INT, BigInteger.valueOf(pc))));
    CIdExpression pcIdExpression = new CIdExpression(mainLocation, CNumericTypes.INT, pcVarName, pcDeclaration);
    CFAEdge pcDeclarationEdge = new CDeclarationEdge(String.format("int %s = %d;", pcVarName, pc), 0, start, loopHead, pcDeclaration);
    start.addLeavingEdge(pcDeclarationEdge);
    loopHead.addEnteringEdge(pcDeclarationEdge);

    Queue<CFANode> nodes = new ArrayDeque<>(getAllNodes(pInputCFA));
    Set<CFAEdge> dummyEdges = new HashSet<>();

    // Eliminate self loops
    List<CFANode> toAdd = new ArrayList<>();
    for (CFANode node : nodes) {
      this.shutdownNotifier.shutdownIfNecessary();
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        CFANode successor = edge.getSuccessor();
        // Eliminate a direct self edge by introducing a dummy node in between
        if (successor == node) {
          removeFromNodes(edge);

          Map<CFANode, CFANode> tmpMap = new HashMap<>();
          CFANode dummy = getOrCreateNewFromOld(successor, tmpMap);

          tmpMap.put(successor, successor);
          CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(edge, dummy, successor, tmpMap);
          addToNodes(replacementEdge);

          BlankEdge dummyEdge = new BlankEdge("", edge.getLineNumber(), node, dummy, DUMMY_EDGE);
          addToNodes(dummyEdge);
          dummyEdges.add(dummyEdge);

          successor = dummy;
          toAdd.add(dummy);
        }
      }
    }
    nodes.addAll(toAdd);

    Map<Integer, CFANode> newPredecessorsToPC = new LinkedHashMap<>();
    BiMap<Integer, CFANode> newSuccessorsToPC = HashBiMap.create();
    Map<CFANode, CFANode> globalNewToOld = new HashMap<>();
    Map<CFANode, CFANode> entryNodeConnectors = new HashMap<>();
    globalNewToOld.put(oldMainFunctionEntryNode, start);

    // Create new nodes and assume edges based on program counter values leading to the new nodes
    Set<CFANode> visited = new HashSet<>();
    while (!nodes.isEmpty()) {
      this.shutdownNotifier.shutdownIfNecessary();
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
      final Set<CFANode> subgraph = new LinkedHashSet<>();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(subgraphRoot);
      subgraph.add(subgraphRoot);
      Set<Integer> pcValuesOutOfSubgraph = new HashSet<>();
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        for (CFAEdge edge : CFAUtils.leavingEdges(current).toList()) {
          CFANode next = edge.getSuccessor();

          assert current != next : "Self-loops must be eliminated previously";

          // Add a node to the subgraph if no cycle is introduced by it
          if (!becomesLoop(subgraph, next) && visited.add(next)) {
            subgraph.add(next);
            waitlist.add(next);
          } else if (!subgraph.contains(next)) {
            // Cut off the edge leaving the subgraph
            removeFromNodes(edge);

            Map<CFANode, CFANode> tmpMap = new HashMap<>();

            /*
             * Assume edges should stay with their original predecessor, thus a
             * dummy successor is introduced between the edge and the original
             * successor
             */
            if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {

              tmpMap.put(current, current);
              CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(edge, tmpMap);
              addToNodes(replacementEdge);

              // Create the dummy edge, but do not add it to the nodes!
              CFANode dummy = replacementEdge.getSuccessor();
              CFAEdge dummyEdge = new BlankEdge("", edge.getLineNumber(), dummy, next, DUMMY_EDGE);
              dummyEdges.add(dummyEdge);

              /*
               * Adjust the values: Current becomes the dummy, which is also
               * part of the subgraph now and the edge that is cut off is now
               * the dummy edge.
               */
              current = dummy;
              subgraph.add(dummy);
              edge = dummyEdge;
              tmpMap.clear();
            }

            /*
             * Copy the old edge but connect it to a new predecessor which will
             * become part of the linked subgraph
             */
            tmpMap.put(next, next);
            CFAEdge connectionEdge = copyCFAEdgeWithNewNodes(edge, tmpMap);
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
            pcValuesOutOfSubgraph.add(pcToSuccessor);
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

    // Remove trivial dummy subgraphs and other dummy edges
    Map<CFANode, Integer> pcToNewSuccessors = newSuccessorsToPC.inverse();
    for (int replaceablePCValue : new ArrayList<>(newPredecessorsToPC.keySet())) {
      this.shutdownNotifier.shutdownIfNecessary();
      CFANode newSuccessor = newSuccessorsToPC.get(replaceablePCValue);
      CFANode tailOfRedundantSubgraph = newPredecessorsToPC.get(replaceablePCValue);
      Integer precedingPCValue;
      CFAEdge dummyEdge;
      // If a subgraph consists only of a dummy edge, eliminate it completely
      if (tailOfRedundantSubgraph.getNumEnteringEdges() == 1
          && isDummyEdge(dummyEdge = tailOfRedundantSubgraph.getEnteringEdge(0))
          && dummyEdge.getPredecessor().getNumEnteringEdges() == 0
          && (precedingPCValue = pcToNewSuccessors.get(dummyEdge.getPredecessor())) != null) {
        Integer predToRemove = pcToNewSuccessors.remove(newSuccessor);
        if (predToRemove != null) {
          newPredecessorsToPC.remove(predToRemove);
        }
        newSuccessorsToPC.remove(precedingPCValue);
        newSuccessorsToPC.put(precedingPCValue, newSuccessor);
      }
    }
    for (CFAEdge oldDummyEdge : dummyEdges) {
      this.shutdownNotifier.shutdownIfNecessary();
      CFANode successor = globalNewToOld.get(oldDummyEdge.getSuccessor());
      for (CFAEdge edge : CFAUtils.enteringEdges(successor)) {
        if (isDummyEdge(edge)) {
          removeFromNodes(edge);
          CFANode predecessor = edge.getPredecessor();
          /*
           * If the subgraph is entered by a dummy edge adjust the program
           * counter successor.
           */
          Integer precedingPCValue;
          if (predecessor.getNumEnteringEdges() == 0
              && (precedingPCValue = pcToNewSuccessors.get(predecessor)) != null) {
            pcToNewSuccessors.remove(predecessor);
            newSuccessorsToPC.remove(precedingPCValue);
            newSuccessorsToPC.put(precedingPCValue, edge.getSuccessor());
          } else {
            /*
             * If the dummy edge is somewhere in between replace its
             * predecessor by its successor in the graph.
             */
            for (CFAEdge edgeEnteringPredecessor : CFAUtils.enteringEdges(predecessor)) {
              removeFromNodes(edgeEnteringPredecessor);
              edgeEnteringPredecessor =
                  copyCFAEdgeWithNewNodes(edgeEnteringPredecessor, edgeEnteringPredecessor.getPredecessor(), successor, globalNewToOld);
              addToNodes(edgeEnteringPredecessor);
            }
          }
        }
      }
    }

    CFANode firstSubgraphStart = newSuccessorsToPC.get(0);

    if (initialSubgraphAsPrefix) {
      newSuccessorsToPC.remove(0);
      // Connect the first subgraph directly to the declaration
      removeFromNodes(pcDeclarationEdge);
      pcDeclarationEdge = copyCFAEdgeWithNewNodes(pcDeclarationEdge, start, firstSubgraphStart, globalNewToOld);
      addToNodes(pcDeclarationEdge);
    }

    /*
     * Connect the subgraph tails to their successors via the loop head by
     * setting the corresponding program counter values.
     */
    connectSubgraphLeavingNodesToLoopHead(loopHead, newPredecessorsToPC, pcIdExpression, mainLocation);

    // Connect the subgraph entry nodes by assuming the program counter values
    connectLoopHeadToSubgraphEntryNodes(loopHead, newSuccessorsToPC, pcIdExpression, mainLocation,
        new CBinaryExpressionBuilder(pInputCFA.getMachineModel(), logger));

    // Skip the program counter initialization if it is never used
    if (newPredecessorsToPC.size() <= 1) {
      removeFromNodes(pcDeclarationEdge);
      replaceInStructure(firstSubgraphStart, start);
    }

    // If there is only one pc value (besides 0), then the loop head can be skipped
    if (newPredecessorsToPC.size() == 1) {
      Entry<Integer, CFANode> entry = newPredecessorsToPC.entrySet().iterator().next();
      CFANode pred = entry.getValue();
      int pcValue = entry.getKey();
      CFANode succ = newSuccessorsToPC.get(pcValue);
      removeFromGraph(loopHead);
      replaceInStructure(succ, pred);
      loopHead = pred;
    }

    // Build the CFA from the syntactically reachable nodes
    return buildCFA(start, loopHead, pInputCFA.getMachineModel(), pInputCFA.getLanguage());
  }

  /**
   * Checks if the given edge is a dummy edge.
   *
   * @param pEdge the edge to check.
   * @return <code>true</code> if the edge is a dummy edge, <code>false</code> otherwise.
   */
  private boolean isDummyEdge(CFAEdge pEdge) {
    return pEdge.getEdgeType() == CFAEdgeType.BlankEdge && pEdge.getDescription().equals(DUMMY_EDGE);
  }

  /**
   * Checks if the given subgraph will contain a loop if the given node is
   * added to the subgraph. It is assumed that the given subgraph does not yet
   * contain a loop.
   *
   * The result of the function is undefined if the given subgraph already
   * contains a loop - this may or may not be detected leading to arbitrary
   * results.
   *
   * @param pSubgraph the subgraph that would potentially contain the loop.
   * @param pSuccessor the node that would potentially introduce the loop.
   * @return <code>true</code> if the given node would introduce a loop if
   * added to the subgraph, <code>false</code> otherwise.
   */
  private boolean becomesLoop(Set<CFANode> pSubgraph, CFANode pSuccessor) {
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(pSuccessor);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (visited.add(current)) {
        for (CFANode succ : CFAUtils.successorsOf(current)) {
          if (succ.equals(pSuccessor)) {
            return true;
          } else if (pSubgraph.contains(succ)) {
            waitlist.add(succ);
          }
        }
      }
    }
    return false;
  }

  /**
   * Builds a CFA by collecting the nodes syntactically reachable from the
   * start node. Any nodes belonging to functions with unreachable entry nodes
   * are also omitted.
   *
   * @param pStartNode the start node.
   * @param pLoopHead the single loop head.
   * @param pMachineModel the machine model.
   * @param pLanguage the programming language.
   *
   * @return the CFA represented by the nodes reachable from the start node.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  private ImmutableCFA buildCFA(FunctionEntryNode pStartNode, CFANode pLoopHead, MachineModel pMachineModel, Language pLanguage) throws InvalidConfigurationException, InterruptedException {
    Map<String, FunctionEntryNode> functions = null;
    SortedSetMultimap<String, CFANode> allNodes = null;
    boolean changed = true;
    while (changed) {
      this.shutdownNotifier.shutdownIfNecessary();
      changed = false;
      functions = new HashMap<>();
      allNodes = TreeMultimap.create();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(pStartNode);
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        String functionName = current.getFunctionName();
        if (allNodes.put(functionName, current)) {
          waitlist.addAll(CFAUtils.successorsOf(current).toList());
          if (current instanceof FunctionEntryNode) {
            functions.put(functionName, (FunctionEntryNode) current);
          }
        }
      }

      // Remove nodes belonging to unreachable functions
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
      this.shutdownNotifier.shutdownIfNecessary();
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
        getLoopStructure(pLoopHead);

    // Get information about variables, required by some analyses
    final Optional<VariableClassification> varClassification
        = loopStructure.isPresent()
        ? Optional.of(new VariableClassification(cfa, config, logger, loopStructure.get()))
        : Optional.<VariableClassification>absent();

    // Finalize the transformed CFA
    return cfa.makeImmutableCFA(loopStructure, varClassification);
  }

  /**
   * Removes the given node from its graph by removing all its entering edges
   * from its predecessors and all its leaving edges from its successors.
   *
   * All these edges are also removed from the node itself, of course.
   *
   * @param pToRemove the node to be removed.
   */
  private void removeFromGraph(CFANode pToRemove) {
    while (pToRemove.getNumEnteringEdges() > 0) {
      removeFromNodes(pToRemove.getEnteringEdge(0));
    }
    if (pToRemove.getEnteringSummaryEdge() != null) {
      pToRemove.removeEnteringSummaryEdge(pToRemove.getEnteringSummaryEdge());
    }
    while (pToRemove.getNumLeavingEdges() > 0) {
      removeFromNodes(pToRemove.getLeavingEdge(0));
    }
    if (pToRemove.getLeavingSummaryEdge() != null) {
      pToRemove.removeLeavingSummaryEdge(pToRemove.getLeavingSummaryEdge());
    }
  }

  /**
   * Removes the given edge from its nodes.
   *
   * @param pEdge the edge to remove.
   */
  private static void removeFromNodes(CFAEdge pEdge) {
    pEdge.getPredecessor().removeLeavingEdge(pEdge);
    pEdge.getSuccessor().removeEnteringEdge(pEdge);
  }

  /**
   * Adds the given edge as a leaving edge to its predecessor and as an
   * entering edge to its successor.
   *
   * @param pEdge the edge to add.
   */
  private static void addToNodes(CFAEdge pEdge) {
    pEdge.getPredecessor().addLeavingEdge(pEdge);
    pEdge.getSuccessor().addEnteringEdge(pEdge);
  }

  /**
   * Connects the nodes leaving a subgraph to the loop head using assignment
   * edges setting the program counter to the value required for reaching the
   * correct successor in the next loop iteration.
   *
   * @param pLoopHead the loop head.
   * @param pNewPredecessorsToPC the nodes that represent gates for leaving
   * their subgraph mapped to the program counter values corresponding to the
   * correct successor states.
   * @param pPCIdExpression the CIdExpression used for the program counter variable.
   * @param pMainLocation the location of the main function.
   */
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
   * @param pLoopHead the loop head.
   * @param newSuccessorToPCMapping the mapping of subgraph entry nodes to the
   * corresponding program counter values.
   * @param pPCIdExpression the CIdExpression used for the program counter variable.
   * @param pMainLocation the location of the main function.
   * @param pExpressionBuilder the CExpressionBuilder used to build the assume edges.
   */
   private void connectLoopHeadToSubgraphEntryNodes(CFANode pLoopHead,
      Map<Integer, CFANode> newSuccessorToPCMapping,
      CIdExpression pPCIdExpression,
      FileLocation pMainLocation,
      CBinaryExpressionBuilder pExpressionBuilder) {
    List<CAssumeEdge> toAdd = new ArrayList<>();
    CFANode decisionTreeNode = pLoopHead;
    for (Entry<Integer, CFANode> pcToNewSuccessorMapping : newSuccessorToPCMapping.entrySet()) {
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
      if (omitExplicitLastProgramCounterAssumption) {
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
          addToNodes(edge);
        }
      } else {
        BlankEdge defaultBackEdge = new BlankEdge("", 0, decisionTreeNode, pLoopHead, "Illegal program counter value");
        addToNodes(defaultBackEdge);
      }
    }
    // Add the edges connecting the real nodes with the loop head
    for (CFAEdge edge : toAdd) {
      addToNodes(edge);
    }

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
        waitlist.addAll(CFAUtils.successorsOf(current).toList());
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
        waitlist.addAll(CFAUtils.successorsOf(current).toList());
      }
    }
    return nodes;
  }

  /**
   * Replaces the given old node by the given new node in its structure by
   * removing all edges leading to and from the old node and copying them so
   * that the leave or lead to the new node.
   *
   * @param pOldNode the node to remove the edges from.
   * @param pNewNode the node to add the edges to.
   */
  private static void replaceInStructure(CFANode pOldNode, CFANode pNewNode) {
    Map<CFANode, CFANode> newToOld = new HashMap<>();
    newToOld.put(pOldNode, pNewNode);
    CFAEdge oldEdge;
    while ((oldEdge = removeNextEnteringEdge(pOldNode)) != null && constantTrue(newToOld.put(oldEdge.getPredecessor(), oldEdge.getPredecessor()))
        || (oldEdge = removeNextLeavingEdge(pOldNode)) != null && constantTrue(newToOld.put(oldEdge.getSuccessor(), oldEdge.getSuccessor()))) {
      CFAEdge newEdge = copyCFAEdgeWithNewNodes(oldEdge, newToOld);
      addToNodes(newEdge);
    }
  }

  /**
   * Returns <code>true</code>, no matter what is passed.
   *
   * @param pParam this argument is ignored.
   * @return <code>true</code>
   */
  private static boolean constantTrue(Object pParam) {
    return true;
  }

  /**
   * Removes the next entering edge from the given node and the predecessor of
   * the edge.
   *
   * @param pCfaNode the node to remove an edge from.
   *
   * @return the removed edge.
   */
  @Nullable
  private static CFAEdge removeNextEnteringEdge(CFANode pCfaNode) {
    int numberOfEnteringEdges = pCfaNode.getNumEnteringEdges();
    CFAEdge result = null;
    if (numberOfEnteringEdges > 0) {
      result = pCfaNode.getEnteringEdge(numberOfEnteringEdges - 1);
    }
    if (result == null) {
      result = pCfaNode.getEnteringSummaryEdge();
    }
    if (result != null) {
      removeFromNodes(result);
    }
    return result;
  }

  /**
   * Removes the next leaving edge from the given node and the successor of
   * the edge.
   *
   * @param pCfaNode the node to remove an edge from.
   *
   * @return the removed edge.
   */
  @Nullable
  private static CFAEdge removeNextLeavingEdge(CFANode pCfaNode) {
    int numberOfLeavingEdges = pCfaNode.getNumLeavingEdges();
    CFAEdge result = null;
    if (numberOfLeavingEdges > 0) {
      result = pCfaNode.getLeavingEdge(numberOfLeavingEdges - 1);
    }
    if (result == null) {
      result = pCfaNode.getLeavingSummaryEdge();
    }
    if (result != null) {
      removeFromNodes(result);
    }
    return result;
  }

  /**
   * Given a node and a mapping of nodes of one graph to nodes of a different
   * graph, assumes that the given node belongs to the first graph and checks
   * if a node of the second graph is mapped to it. If so, the node of the
   * second graph mapped to the given node is returned. Otherwise, the given
   * node is copied <strong>without edges</strong> and recorded in the mapping
   * before it is returned.
   *
   * @param pNode the node to get or create a partner for in the second graph.
   * @param pNewToOldMapping the mapping between the first graph to the second
   * graph.
   *
   * @return a copy of the given node, possibly reused from the provided
   * mapping.
   */
  private static CFANode getOrCreateNewFromOld(CFANode pNode, Map<CFANode, CFANode> pNewToOldMapping) {
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

  /**
   * Copies the given control flow edge using the given new predecessor and
   * successor. Any additionally required nodes are taken from the given
   * mapping by using the corresponding node of the old edge as a key or, if
   * no node is mapped to this key, by copying the key and recording the result
   * in the mapping.
   *
   * @param pEdge the edge to copy.
   * @param pNewPredecessor the new predecessor.
   * @param pNewSuccessor the new successor.
   * @param pNewToOldMapping a mapping of old nodes to new nodes.
   *
   * @return a new edge with the given predecessor and successor.
   */
  private static CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, CFANode pNewPredecessor, CFANode pNewSuccessor, final Map<CFANode, CFANode> pNewToOldMapping) {
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
      CFANode summaryEdgePredecessor = functionSummaryEdge.getPredecessor();
      CFANode summaryEdgeSuccessor = functionSummaryEdge.getSuccessor();
      if (summaryEdgePredecessor.getLeavingSummaryEdge() != null) {
        summaryEdgePredecessor.removeLeavingSummaryEdge(summaryEdgePredecessor.getLeavingSummaryEdge());
      }
      if (summaryEdgeSuccessor.getEnteringSummaryEdge() != null) {
        summaryEdgeSuccessor.removeEnteringSummaryEdge(summaryEdgeSuccessor.getEnteringSummaryEdge());
      }
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

  /**
   * Copies the given control flow edge Predecessor, successor and any
   * additionally required nodes are taken from the given mapping by using the
   * corresponding node of the old edge as a key or, if no node is mapped to
   * this key, by copying the key and recording the result in the mapping.
   *
   * @param pEdge the edge to copy.
   * @param pNewToOldMapping a mapping of old nodes to new nodes.
   *
   * @return a new edge with the given predecessor and successor.
   */
  private static CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, final Map<CFANode, CFANode> pNewToOldMapping) {
    CFANode newPredecessor = getOrCreateNewFromOld(pEdge.getPredecessor(), pNewToOldMapping);
    CFANode newSuccessor = getOrCreateNewFromOld(pEdge.getSuccessor(), pNewToOldMapping);
    return copyCFAEdgeWithNewNodes(pEdge, newPredecessor, newSuccessor, pNewToOldMapping);
  }

  /**
   * Gets the loop structure of a control flow automaton with one single loop.
   *
   * @param pSingleLoopHead the loop head of the single loop.
   *
   * @return the loop structure of the control flow automaton.
   */
  private Optional<ImmutableMultimap<String, Loop>> getLoopStructure(CFANode pSingleLoopHead) {
    Queue<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> reachableSuccessors = new HashSet<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.offer(pSingleLoopHead);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      reachableSuccessors.add(current);
      for (CFANode successor : CFAUtils.successorsOf(current)) {
        if (visited.add(successor)) {
          waitlist.add(successor);
        }
      }
    }
    visited.clear();
    waitlist.offer(pSingleLoopHead);
    Set<CFANode> loopNodes = new HashSet<>();
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (reachableSuccessors.contains(current)) {
        loopNodes.add(current);
        for (CFANode predecessor : CFAUtils.predecessorsOf(current)) {
          if (visited.add(predecessor)) {
            waitlist.add(predecessor);
          }
        }
      }
    }
    String loopFunction = pSingleLoopHead.getFunctionName();
    // A size of one means only the loop head is contained
    return Optional.of(loopNodes.isEmpty() || loopNodes.size() == 1 && !pSingleLoopHead.hasEdgeTo(pSingleLoopHead)
        ? ImmutableMultimap.<String, Loop>of()
        : ImmutableMultimap.<String, Loop>builder().put(loopFunction, new Loop(pSingleLoopHead, loopNodes)).build());
  }

  /**
   * Instances of this class are proxies for ShutdownNotifier instances so that
   * calls to {@link ShutdownNotifier#shutdownIfNecessary} are delegated to the
   * wrapped instance or, if the wrapped instance is <code>null</code>, do
   * nothing.
   */
  private static class OptionalShutdownNotifier {

    private final ShutdownNotifier shutdownNotifier;

    public OptionalShutdownNotifier(@Nullable ShutdownNotifier pShutdownNotifier) {
      this.shutdownNotifier = pShutdownNotifier;
    }

    /**
     * @see ShutdownNotifier#shutdownIfNecessary
     *
     * @throws InterruptedException if a shutdown was requested.
     */
    public void shutdownIfNecessary() throws InterruptedException {
      if (shutdownNotifier != null) {
        shutdownNotifier.shutdownIfNecessary();
      }
    }

  }

}
