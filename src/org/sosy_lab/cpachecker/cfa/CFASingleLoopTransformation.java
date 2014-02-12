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
package org.sosy_lab.cpachecker.cfa;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
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
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Instances of this class are used to apply single loop transformation to
 * control flow automata.
 */
@Options
public class CFASingleLoopTransformation {

  public static final String ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME = " ";

  /**
   * The name of the program counter variable introduced by the transformation.
   */
  public static final String PROGRAM_COUNTER_VAR_NAME = "___pc";

  /**
   * The description of the dummy edges used.
   */
  private static final String DUMMY_EDGE = "DummyEdge";

  private static final Predicate<CFAEdge> DUMMY_EDGE_PREDICATE = new Predicate<CFAEdge>() {

      @Override
      public boolean apply(@Nullable CFAEdge pArg0) {
        return isDummyEdge(pArg0);
      }};

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

  @Option(name="cfa.transformIntoSingleLoop.programCounterValueProviderFactory",
      description="This option controls what program counter values are used" +
          ". Possible values are INCREMENTAL and NODE_NUMBER.")
  private ProgramCounterValueProviderFactories programCounterValueProviderFactory = ProgramCounterValueProviderFactories.INCREMENTAL;

  @Option(name="cfa.transformIntoSingleLoop.subgraphGrowthStrategy",
      description="This option controls the size of the subgraphs referred" +
          " to by program counter values. The larger the subgraphs, the" +
          " fewer program counter values are required. Possible values are " +
          " MULTIPLE_PATHS, SINGLE_PATH and SINGLE_EDGE, where" +
          " MULTIPLE_PATHS has the largest subgraphs (and fewest program" +
          " counter values) and SINGLE_EDGE has the smallest subgraphs (and" +
          " most program counter values). The larger the subgraphs, the" +
          " closer the resulting graph will look like the original CFA.")
  private AcyclicGraph.AcyclicGrowthStrategies subgraphGrowthStrategy = org.sosy_lab.cpachecker.cfa.CFASingleLoopTransformation.AcyclicGraph.AcyclicGrowthStrategies.MULTIPLE_PATHS;

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
   * @throws InvalidConfigurationException if the configuration this transformer was created with is invalid.
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  public ImmutableCFA apply(CFA pInputCFA) throws InvalidConfigurationException, InterruptedException {
    // Create new main function entry and initialize the program counter there
    FunctionEntryNode oldMainFunctionEntryNode = pInputCFA.getMainFunction();
    AFunctionDeclaration mainFunctionDeclaration = oldMainFunctionEntryNode.getFunctionDefinition();
    FileLocation mainLocation = mainFunctionDeclaration.getFileLocation();
    FunctionEntryNode start = oldMainFunctionEntryNode instanceof CFunctionEntryNode ?
        new CFunctionEntryNode(0, (CFunctionDeclaration) mainFunctionDeclaration, oldMainFunctionEntryNode.getExitNode(), oldMainFunctionEntryNode.getFunctionParameterNames()) :
        new JMethodEntryNode(0, (JMethodDeclaration) mainFunctionDeclaration, oldMainFunctionEntryNode.getExitNode(), oldMainFunctionEntryNode.getFunctionParameterNames());
    CFANode loopHead = new CFANode(0, ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);

    Queue<CFANode> nodes = new ArrayDeque<>(getAllNodes(pInputCFA));

    // Eliminate self loops
    eliminateSelfLoops(nodes);

    Multimap<Integer, CFANode> newPredecessorsToPC = LinkedHashMultimap.create();
    BiMap<Integer, CFANode> newSuccessorsToPC = HashBiMap.create();
    SimpleMap<CFANode, CFANode> globalNewToOld = SimpleMapAdapter.createSimpleHashMap();
    globalNewToOld.put(oldMainFunctionEntryNode, start);

    // Create new nodes and assume edges based on program counter values leading to the new nodes
    int pcValueOfStart =
        buildProgramCounterValueMaps(oldMainFunctionEntryNode, nodes,
            newPredecessorsToPC, newSuccessorsToPC, globalNewToOld);

    // Remove trivial dummy subgraphs and other dummy edges etc.
    simplify(start, newPredecessorsToPC, newSuccessorsToPC, globalNewToOld);

    // Declare program counter and initialize it to 0
    String pcVarName = PROGRAM_COUNTER_VAR_NAME;
    CDeclaration pcDeclaration = new CVariableDeclaration(mainLocation, true, CStorageClass.AUTO, CNumericTypes.INT, pcVarName, pcVarName, pcVarName,
        new CInitializerExpression(mainLocation, new CIntegerLiteralExpression(mainLocation, CNumericTypes.INT, BigInteger.valueOf(pcValueOfStart))));
    CIdExpression pcIdExpression = new CIdExpression(mainLocation, CNumericTypes.INT, pcVarName, pcDeclaration);
    CFAEdge pcDeclarationEdge = new CDeclarationEdge(String.format("int %s = %d;", pcVarName, pcValueOfStart), 0, start, loopHead, pcDeclaration);
    start.addLeavingEdge(pcDeclarationEdge);
    loopHead.addEnteringEdge(pcDeclarationEdge);

    CFANode firstSubgraphStart = newSuccessorsToPC.get(pcValueOfStart);

    if (initialSubgraphAsPrefix) {
      newSuccessorsToPC.remove(pcValueOfStart);
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

    /*
     * Fix the summary edges broken by the indirection introduced by the
     * artificial loop.
     */
    fixSummaryEdges(start, newSuccessorsToPC, globalNewToOld);

    // Skip the program counter initialization if it is never used
    if (newPredecessorsToPC.size() <= 1) {
      removeFromNodes(pcDeclarationEdge);
      replaceInStructure(firstSubgraphStart, start);
    }

    // If there is only one pc value, then the loop head can be skipped
    if (newPredecessorsToPC.size() == 1) {
      Entry<Integer, CFANode> entry = newPredecessorsToPC.entries().iterator().next();
      CFANode pred = entry.getValue();
      int pcValue = entry.getKey();
      CFANode succ = newSuccessorsToPC.get(pcValue);
      removeFromGraph(loopHead);
      replaceInStructure(pred, succ);
      loopHead = succ;
    }

    // Build the CFA from the syntactically reachable nodes
    ImmutableCFA result = buildCFA(start, loopHead, pInputCFA.getMachineModel(), pInputCFA.getLanguage());
    return result;
  }

  /**
   * Simplify the new graph by removing empty subgraphs and dummy edges.
   *
   * @param pStartNode the start node of the new control flow automaton.
   * @param pNewPredecessorsToPC the mapping of program counter value assignment predecessors to program counter values. Must be mutable.
   * @param pNewSuccessorsToPC the mapping of program counter value assumption successors to program counter values. Must be mutable.
   * @param pGlobalNewToOld
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  private void simplify(CFANode pStartNode,
      Multimap<Integer, CFANode> pNewPredecessorsToPC,
      BiMap<Integer, CFANode> pNewSuccessorsToPC,
      SimpleMap<CFANode, CFANode> pGlobalNewToOld) throws InterruptedException {
    Map<CFANode, Integer> pcToNewSuccessors = pNewSuccessorsToPC.inverse();
    for (int replaceablePCValue : new ArrayList<>(pNewPredecessorsToPC.keySet())) {
      this.shutdownNotifier.shutdownIfNecessary();
      CFANode newSuccessor = pNewSuccessorsToPC.get(replaceablePCValue);
      List<CFANode> tailsOfRedundantSubgraph = new ArrayList<>(pNewPredecessorsToPC.get(replaceablePCValue));
      for (CFANode tailOfRedundantSubgraph : tailsOfRedundantSubgraph) {
        Integer precedingPCValue;
        CFAEdge dummyEdge;
        // If a subgraph consists only of a dummy edge, eliminate it completely
        if (tailOfRedundantSubgraph.getNumEnteringEdges() == 1
            && isDummyEdge(dummyEdge = tailOfRedundantSubgraph.getEnteringEdge(0))
            && dummyEdge.getPredecessor().getNumEnteringEdges() == 0
            && (precedingPCValue = pcToNewSuccessors.get(dummyEdge.getPredecessor())) != null) {
          Integer predToRemove = pcToNewSuccessors.remove(newSuccessor);
          for (CFANode removedPredecessor : pNewPredecessorsToPC.removeAll(predToRemove)) {
            pNewPredecessorsToPC.put(precedingPCValue, removedPredecessor);
          }
          pNewPredecessorsToPC.remove(precedingPCValue, tailOfRedundantSubgraph);
          pNewSuccessorsToPC.remove(precedingPCValue);
          pNewSuccessorsToPC.put(precedingPCValue, newSuccessor);
        }
      }
    }
    for (CFAEdge oldDummyEdge : findEdges(DUMMY_EDGE_PREDICATE, pStartNode)) {
      this.shutdownNotifier.shutdownIfNecessary();
      CFANode successor = pGlobalNewToOld.get(oldDummyEdge.getSuccessor());
      for (CFAEdge edge : CFAUtils.enteringEdges(successor).toList()) {
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
            pNewSuccessorsToPC.remove(precedingPCValue);
            pNewSuccessorsToPC.put(precedingPCValue, edge.getSuccessor());
          } else {
            /*
             * If the dummy edge is somewhere in between, replace its
             * predecessor by its successor in the graph.
             */
            for (CFAEdge edgeEnteringPredecessor : CFAUtils.enteringEdges(predecessor).toList()) {
              removeFromNodes(edgeEnteringPredecessor);
              edgeEnteringPredecessor =
                  copyCFAEdgeWithNewNodes(edgeEnteringPredecessor, edgeEnteringPredecessor.getPredecessor(), successor, pGlobalNewToOld);
              addToNodes(edgeEnteringPredecessor);
            }
          }
        }
      }
    }
  }

  /**
   * All function call edges calling functions where the summary edge
   * successor, i.e. the node succeeding the function call predecessor in the
   * caller function, is now a successor of the artificial decision tree need
   * to be fixed in that their summary edge must now point to a different
   * successor: The predecessor of the assignment edge assigning the program
   * counter value that leads to the old successor.
   *
   * @param pStartNode the start node of the new graph.
   * @param pNewSuccessorsToPC the successor nodes of program counter value
   * assume edges mapped to their respective program counter value.
   * @param pGlobalNewToOld the mapping of new control flow nodes to old control
   * flow nodes.
   */
  private void fixSummaryEdges(FunctionEntryNode pStartNode,
      BiMap<Integer, CFANode> pNewSuccessorsToPC,
      SimpleMap<CFANode, CFANode> pGlobalNewToOld) {
    for (FunctionCallEdge fce : findEdges(FunctionCallEdge.class, pStartNode)) {
      FunctionEntryNode entryNode = fce.getSuccessor();
      FunctionExitNode exitNode = entryNode.getExitNode();
      FunctionSummaryEdge oldSummaryEdge = fce.getSummaryEdge();
      CFANode oldSummarySuccessor = fce.getSummaryEdge().getSuccessor();
      Integer pcValue = pNewSuccessorsToPC.inverse().get(oldSummarySuccessor);
      if (pcValue != null) {
        // Find the correct new successor
        for (CFANode potentialNewSummarySuccessor : CFAUtils.successorsOf(exitNode)) {
          if (potentialNewSummarySuccessor.getNumLeavingEdges() == 1) {
            CFAEdge potentialPCValueAssignmentEdge = potentialNewSummarySuccessor.getLeavingEdge(0);
            if (potentialPCValueAssignmentEdge instanceof ProgramCounterValueAssignmentEdge) {
              ProgramCounterValueAssignmentEdge programCounterValueAssignmentEdge =
                  (ProgramCounterValueAssignmentEdge) potentialPCValueAssignmentEdge;
              if (programCounterValueAssignmentEdge.getProgramCounterValue() == pcValue) {
                // Fix the function summary edge
                FunctionSummaryEdge newSummaryEdge = (FunctionSummaryEdge) copyCFAEdgeWithNewNodes(oldSummaryEdge, oldSummaryEdge.getPredecessor(), potentialNewSummarySuccessor, pGlobalNewToOld);
                removeSummaryEdgeFromNodes(oldSummaryEdge);
                oldSummaryEdge = newSummaryEdge.getPredecessor().getLeavingSummaryEdge();
                if (oldSummaryEdge != null) {
                  removeSummaryEdgeFromNodes(oldSummaryEdge);
                }
                newSummaryEdge.getPredecessor().addLeavingSummaryEdge(newSummaryEdge);
                oldSummaryEdge = newSummaryEdge.getSuccessor().getEnteringSummaryEdge();
                if (oldSummaryEdge != null) {
                  removeSummaryEdgeFromNodes(oldSummaryEdge);
                }
                newSummaryEdge.getSuccessor().addEnteringSummaryEdge(newSummaryEdge);
                // Fix function summary statement edges
                for (CFunctionSummaryStatementEdge edge : CFAUtils.leavingEdges(oldSummaryEdge.getPredecessor()).filter(CFunctionSummaryStatementEdge.class)) {
                  if (edge.getPredecessor() != newSummaryEdge.getPredecessor() || edge.getSuccessor() != newSummaryEdge.getSuccessor()) {
                    removeFromNodes(edge);
                    CFAEdge newEdge = copyCFAEdgeWithNewNodes(edge, newSummaryEdge.getPredecessor(), newSummaryEdge.getSuccessor(), pGlobalNewToOld);
                    addToNodes(newEdge);
                  }
                }
                break;
              }
            }
          }
        }
      }
    }
  }

  /**
   * Collects all matching edges reachable from the given start node.
   *
   * @param pTypeToken a token specifying the type of the edges.
   * @param pStartNode the node to start searching from.
   * @return all found matching edges.
   */
  private <T extends CFAEdge> Iterable<T> findEdges(Class<T> pTypeToken, CFANode pStartNode) {
    return FluentIterable.from(findEdges(Predicates.instanceOf(pTypeToken), pStartNode)).filter(pTypeToken);
  }

  /**
   * Collects all matching edges reachable from the given start node.
   *
   * @param pPredicate a predicate specifying the edges.
   * @param pStartNode the node to start searching from.
   * @return all found matching edges.
   */
  private Iterable<CFAEdge> findEdges(Predicate<? super CFAEdge> pPredicate, CFANode pStartNode) {
    Collection<CFAEdge> result = new ArrayList<>();
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.add(pStartNode);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (visited.add(current)) {
        for (CFAEdge edge : CFAUtils.leavingEdges(current)) {
          if (pPredicate.apply(edge)) {
            result.add(edge);
          }
          waitlist.add(edge.getSuccessor());
        }
      }
    }
    return result;
  }

  /**
   * Builds the program counter value maps.
   *
   * @param pOldMainFunctionEntryNode the old main function entry node.
   * @param pNodes the nodes of the original graph.
   * @param pNewPredecessorsToPC the mapping of program counter value assignment predecessors to program counter values. Must be mutable.
   * @param pNewSuccessorsToPC the mapping of program counter value assumption successors to program counter values. Must be mutable.
   * @param pGlobalNewToOld the mapping of new control flow nodes to old control flow nodes.
   *
   * @return the initial program counter value.
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  private int buildProgramCounterValueMaps(FunctionEntryNode pOldMainFunctionEntryNode,
      Iterable<CFANode> pNodes,
      Multimap<Integer, CFANode> pNewPredecessorsToPC,
      BiMap<Integer, CFANode> pNewSuccessorsToPC,
      SimpleMap<CFANode, CFANode> pGlobalNewToOld) throws InterruptedException {
    Set<CFANode> visited = new HashSet<>();
    SimpleMap<CFANode, CFANode> tmpMap = SimpleMapAdapter.createSimpleHashMap();
    AcyclicGraph subgraph = null;
    int pcValueOfStart = -1;
    ProgramCounterValueProvider programCounterValueProvider = this.programCounterValueProviderFactory.newOrImmutableProgramCounterValueProvider();

    Set<CFANode> newWaitlistNodes = new HashSet<>();
    List<Pair<CFANode, CFANode>> predecessorsAndSuccessors = new ArrayList<>();
    for (CFANode subgraphRoot : pNodes) {
      // Mark an unvisited node as visited or discard a visited node
      if (!visited.add(subgraphRoot)) {
        continue;
      }
      this.shutdownNotifier.shutdownIfNecessary();

      /*
       * Handle the old main entry node: There is a new main entry node and
       * there must only be one main entry node, so while the old node must be
       * represented in the transformed graph, it must no longer be a main
       * entry node.
       */
      boolean isOldMainEntryNode = subgraphRoot.equals(pOldMainFunctionEntryNode);
      if (isOldMainEntryNode) {
        subgraphRoot = new CFANode(subgraphRoot.getLineNumber(), subgraphRoot.getFunctionName());
        replaceInStructure(pOldMainFunctionEntryNode, subgraphRoot);
        CFANode newSubgraphRoot = getOrCreateNewFromOld(subgraphRoot, pGlobalNewToOld);
        pcValueOfStart = programCounterValueProvider.getPCValueFor(newSubgraphRoot);
        pNewSuccessorsToPC.put(pcValueOfStart, newSubgraphRoot);
        visited.add(subgraphRoot);
      }

      // Get an acyclic sub graph
      subgraph = subgraph == null ? new AcyclicGraph(subgraphRoot, subgraphGrowthStrategy) : subgraph.reset(subgraphRoot);

      Deque<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(subgraphRoot);
      SimpleTransactionMap<CFANode, CFANode> newToOld = new SimpleTransactionMap<>(pGlobalNewToOld);
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        assert subgraph.containsNode(current) && visited.contains(current);

        newWaitlistNodes.clear();
        predecessorsAndSuccessors.clear();

        Set<CFAEdge> edgesToRemove = new HashSet<>();
        Set<CFAEdge> edgesToAdd = new HashSet<>();

        for (CFAEdge edge : CFAUtils.leavingEdges(current).toList()) {
          CFANode next = edge.getSuccessor();

          assert current != next : "Self-loops must be eliminated previously";

          // Add the edge to the subgraph if no cycle is introduced by it
          if ((!visited.contains(next) || subgraph.containsNode(next))
              && (edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
                || next instanceof CFATerminationNode
                || subgraph.isFurtherGrowthDesired())
              && subgraph.offerEdge(edge)) {
            if (visited.add(next)) {
              newWaitlistNodes.add(next);
            }
          } else {
            assert tmpMap.isEmpty();

            /*
             * Function call edges should stay with their successor, thus a
             * dummy predecessor is introduced between the original predecessor
             * and the edge. Also, all other edges of the old predecessor must
             * be moved to the new predecessor.
             */
            if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
              newWaitlistNodes.clear();
              predecessorsAndSuccessors.clear();
              subgraph.abort();
              newToOld.abort();
              edgesToAdd.clear();
              edgesToRemove.clear();

              FunctionCallEdge fce = (FunctionCallEdge) edge;
              tmpMap.put(next, next);
              tmpMap.put(fce.getSummaryEdge().getSuccessor(), fce.getSummaryEdge().getSuccessor());

              // Replace the edge in the old graph and create a copy in the new graph
              CFAEdge newEdge = replaceAndCopy(edge, tmpMap, newToOld, edgesToRemove, edgesToAdd);

              // Compute the program counter for the replaced edge and map the nodes to it
              CFANode newPredecessor = getOrCreateNewFromOld(current, newToOld);
              CFANode newSuccessor = newEdge.getPredecessor();
              predecessorsAndSuccessors.add(Pair.of(newPredecessor, newSuccessor));

              // Move the other edges over
              for (CFAEdge otherEdge : CFAUtils.leavingEdges(current).toList()) {
                if (otherEdge != edge) {
                  /*
                   * Replace the edge in the old graph, as there is a new
                   * predecessor, and create its equivalent in the new graph.
                   */
                  replaceAndCopy(otherEdge, tmpMap.get(current), otherEdge.getSuccessor(), tmpMap, newToOld, edgesToRemove, edgesToAdd);
                }
              }
              tmpMap.clear();
              // Skip the other edges, as they have already been dealt with
              break;
            } else {
              /*
               * Other edges should stay with their original predecessor, thus a
               * dummy successor is introduced between the edge and the original
               * successor
               */
              edgesToRemove.add(edge);
              tmpMap.put(current, current);
              CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(edge, tmpMap);
              // The replacement edge is added in place of the old edge
              edgesToAdd.add(replacementEdge);
              subgraph.addEdge(replacementEdge);

              CFANode dummy = replacementEdge.getSuccessor();

              // Compute the program counter for the replaced edge and map the nodes to it
              CFANode newPredecessor = getOrCreateNewFromOld(dummy, newToOld);
              CFANode newSuccessor = getOrCreateNewFromOld(next, newToOld);
              predecessorsAndSuccessors.add(Pair.of(newPredecessor, newSuccessor));
            }
            tmpMap.clear();
          }
        }
        newToOld.commit();
        subgraph.commit();
        waitlist.addAll(newWaitlistNodes);
        for (Pair<CFANode, CFANode> predecessorAndSuccessor : predecessorsAndSuccessors) {
          CFANode predecessor = predecessorAndSuccessor.getFirst();
          CFANode successor = predecessorAndSuccessor.getSecond();
          registerTransitionThroughLoopHead(programCounterValueProvider, predecessor, successor, pNewPredecessorsToPC, pNewSuccessorsToPC);
        }
        for (CFAEdge edgeToRemove : edgesToRemove) {
          removeFromNodes(edgeToRemove);
        }
        for (CFAEdge edgeToAdd : edgesToAdd) {
          addToNodes(edgeToAdd);
        }
      }

      // Copy the subgraph
      for (CFAEdge oldEdge : subgraph.getEdges()) {
        addToNodes(copyCFAEdgeWithNewNodes(oldEdge, pGlobalNewToOld));
      }
    }
    return pcValueOfStart;
  }

  private void registerTransitionThroughLoopHead(ProgramCounterValueProvider pProgramCounterValueProvider,
      CFANode pPredecessor,
      CFANode pSuccessor,
      Multimap<Integer, CFANode> pPredecessorsToPC,
      BiMap<Integer, CFANode> pSuccessorsToPC) {
    if (!(pPredecessor instanceof CFATerminationNode)) {
      int pcToSuccessor = pProgramCounterValueProvider.getPCValueFor(pSuccessor);
      pPredecessorsToPC.put(pcToSuccessor, pPredecessor);
      pSuccessorsToPC.put(pcToSuccessor, pSuccessor);
    }
  }

  private CFAEdge replaceAndCopy(CFAEdge pOriginalEdge, @Nullable SimpleMap<CFANode, CFANode> pOldToOld, SimpleMap<CFANode, CFANode> pGlobalNewToOld, Set<CFAEdge> pEdgesToRemove, Set<CFAEdge> pEdgesToAdd) {
      CFANode replacementPredecessor = getOrCreateNewFromOld(pOriginalEdge.getPredecessor(), pOldToOld);
      CFANode replacementSuccessor = getOrCreateNewFromOld(pOriginalEdge.getSuccessor(), pOldToOld);
      return replaceAndCopy(pOriginalEdge, replacementPredecessor,  replacementSuccessor, pOldToOld, pGlobalNewToOld, pEdgesToRemove, pEdgesToAdd);
  }

  private CFAEdge replaceAndCopy(CFAEdge pOriginalEdge, CFANode pReplacementPredecessor, CFANode pReplacementSuccessor, @Nullable SimpleMap<CFANode, CFANode> pOldToOld, SimpleMap<CFANode, CFANode> pGlobalNewToOld, Set<CFAEdge> pEdgesToRemove, Set<CFAEdge> pEdgesToAdd) {
    pEdgesToRemove.add(pOriginalEdge);

    // Replace the edge in the old graph, as there is a new predecessor
    CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(pOriginalEdge, pReplacementPredecessor, pReplacementSuccessor, pOldToOld);

    // The replacement edge is added in place of the old edge
    pEdgesToAdd.add(replacementEdge);

    // Create the actual edge in the new graph
    CFAEdge newEdge = copyCFAEdgeWithNewNodes(replacementEdge, pGlobalNewToOld);
    pEdgesToAdd.add(newEdge);
    return newEdge;
  }

  /**
   * Eliminates all self loops of the given nodes by introducing dummy nodes
   * and edges, so that at least two nodes are involved in any loop afterwards.
   *
   * @param pNodes the nodes to check for self loops.
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  private void eliminateSelfLoops(Collection<CFANode> pNodes) throws InterruptedException {
    List<CFANode> toAdd = new ArrayList<>();
    for (CFANode node : pNodes) {
      this.shutdownNotifier.shutdownIfNecessary();
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        CFANode successor = edge.getSuccessor();
        // Eliminate a direct self edge by introducing a dummy node in between
        if (successor == node) {
          removeFromNodes(edge);

          SimpleMap<CFANode, CFANode> tmpMap = SimpleMapAdapter.createSimpleHashMap();
          CFANode dummy = getOrCreateNewFromOld(successor, tmpMap);

          tmpMap.put(node, node);
          CFAEdge replacementEdge = copyCFAEdgeWithNewNodes(edge, node, dummy, tmpMap);
          addToNodes(replacementEdge);

          BlankEdge dummyEdge = new BlankEdge("", edge.getLineNumber(), dummy, successor, DUMMY_EDGE);
          addToNodes(dummyEdge);

          toAdd.add(dummy);
        }
      }
    }
    pNodes.addAll(toAdd);
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
    Map<String, FunctionEntryNode> functions = new HashMap<>();
    SortedSetMultimap<String, CFANode> allNodes = TreeMultimap.create();
    FunctionExitNode artificialFunctionExitNode = new FunctionExitNode(0, ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
    FunctionEntryNode artificialFunctionEntryNode =
        new FunctionEntryNode(0, ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME, artificialFunctionExitNode, null, Collections.<String>emptyList());
    boolean changed = true;
    while (changed) {
      this.shutdownNotifier.shutdownIfNecessary();
      changed = false;
      functions.clear();
      allNodes.clear();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      waitlist.add(pStartNode);
      while (!waitlist.isEmpty()) {
        this.shutdownNotifier.shutdownIfNecessary();
        CFANode current = waitlist.poll();
        String functionName = current.getFunctionName();
        if (allNodes.put(functionName, current)) {
          waitlist.addAll(CFAUtils.successorsOf(current).toList());
          waitlist.addAll(CFAUtils.predecessorsOf(current).toList());
          if (current instanceof FunctionEntryNode) {
            functions.put(functionName, (FunctionEntryNode) current);
          } else if (current == pLoopHead && functionName.equals(ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME)) {
            functions.put(functionName, artificialFunctionEntryNode);
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
      removeSummaryEdgeFromNodes(pToRemove.getEnteringSummaryEdge());
    }
    while (pToRemove.getNumLeavingEdges() > 0) {
      removeFromNodes(pToRemove.getLeavingEdge(0));
    }
    if (pToRemove.getLeavingSummaryEdge() != null) {
      removeSummaryEdgeFromNodes(pToRemove.getLeavingSummaryEdge());
    }
  }

  /**
   * Removes the given edge from its nodes.
   *
   * @param pEdge the edge to remove.
   */
  private static void removeFromNodes(CFAEdge pEdge) {
    if (pEdge instanceof FunctionSummaryEdge) {
      removeSummaryEdgeFromNodes((FunctionSummaryEdge) pEdge);
    } else {
      pEdge.getPredecessor().removeLeavingEdge(pEdge);
      pEdge.getSuccessor().removeEnteringEdge(pEdge);
      if (pEdge instanceof FunctionCallEdge) {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) pEdge;
        FunctionSummaryEdge summaryEdge = functionCallEdge.getSummaryEdge();
        if (summaryEdge != null) {
          removeSummaryEdgeFromNodes(summaryEdge);
        }
      }
    }
  }

  /**
   * Removes the given summary edge from its nodes.
   *
   * @param pEdge the edge to remove.
   */
  private static void removeSummaryEdgeFromNodes(FunctionSummaryEdge pEdge) {
    CFANode predecessor = pEdge.getPredecessor();
    if (predecessor.getLeavingSummaryEdge() == pEdge) {
      predecessor.removeLeavingSummaryEdge(pEdge);
    }
    CFANode successor = pEdge.getSuccessor();
    if (successor.getEnteringSummaryEdge() == pEdge) {
      successor.removeEnteringSummaryEdge(pEdge);
    }
  }

  /**
   * Adds the given edge as a leaving edge to its predecessor and as an
   * entering edge to its successor.
   *
   * @param pEdge the edge to add.
   */
  private static void addToNodes(CFAEdge pEdge) {
    CFANode predecessor = pEdge.getPredecessor();
    if (pEdge instanceof FunctionSummaryEdge) {
      FunctionSummaryEdge summaryEdge = (FunctionSummaryEdge) pEdge;
      FunctionSummaryEdge oldSummaryEdge = pEdge.getPredecessor().getLeavingSummaryEdge();
      if (oldSummaryEdge != null) {
        removeSummaryEdgeFromNodes(oldSummaryEdge);
      }
      oldSummaryEdge = pEdge.getSuccessor().getEnteringSummaryEdge();
      if (oldSummaryEdge != null) {
        removeSummaryEdgeFromNodes(oldSummaryEdge);
      }
      predecessor.addLeavingSummaryEdge(summaryEdge);
      pEdge.getSuccessor().addEnteringSummaryEdge(summaryEdge);
    } else {
      assert predecessor.getNumLeavingEdges() == 0
          || predecessor.getNumLeavingEdges() <= 1 && pEdge.getEdgeType() == CFAEdgeType.AssumeEdge
          || predecessor instanceof FunctionExitNode && pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge
          || predecessor.getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge && pEdge.getEdgeType() == CFAEdgeType.StatementEdge
          || predecessor.getLeavingEdge(0).getEdgeType() == CFAEdgeType.StatementEdge && pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge;
      predecessor.addLeavingEdge(pEdge);
      pEdge.getSuccessor().addEnteringEdge(pEdge);
    }
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
      Multimap<Integer, CFANode> pNewPredecessorsToPC,
      CIdExpression pPCIdExpression,
      FileLocation pMainLocation) {
    for (Map.Entry<Integer, CFANode> newPredecessorToPC : pNewPredecessorsToPC.entries()) {
      int pcToSet = newPredecessorToPC.getKey();
      CFANode subgraphPredecessor = newPredecessorToPC.getValue();
      CFAEdge edgeToLoopHead = createProgramCounterAssumeEdge(pMainLocation, subgraphPredecessor, pLoopHead, pPCIdExpression, pcToSet);
      addToNodes(edgeToLoopHead);
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
      // Connect the subgraph entry nodes to the loop header by assuming the program counter value
      CFANode newDecisionTreeNode = new CFANode(0, decisionTreeNode.getFunctionName());
      CAssumeEdge toSequence = createProgramCounterAssumeEdge(pMainLocation, pExpressionBuilder, decisionTreeNode, newSuccessor, pPCIdExpression, pcToSet, true);
      CAssumeEdge toNewDecisionTreeNode = createProgramCounterAssumeEdge(pMainLocation, pExpressionBuilder, decisionTreeNode, newDecisionTreeNode, pPCIdExpression, pcToSet, false);
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
   * Gets all nodes of the given control flow automaton in breadth first search
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
        waitlist.addAll(CFAUtils.predecessorsOf(current).toList());
      }
    }

    // Now get all nodes of all syntactically reachable functions
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
        waitlist.addAll(CFAUtils.predecessorsOf(current).toList());
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
  private void replaceInStructure(CFANode pOldNode, CFANode pNewNode) {
    SimpleMap<CFANode, CFANode> newToOld = SimpleMapAdapter.createSimpleHashMap();
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
   * If the provided mapping is {@code null}, no copy but the given node itself
   * is returned.
   *
   * @param pNode the node to get or create a partner for in the second graph.
   * @param pNewToOldMapping the mapping between the first graph to the second
   * graph. If {@code null}, the identity is returned.
   *
   * @return a copy of the given node, possibly reused from the provided
   * mapping or the identity of the given node if the provided mapping is
   * {@code null}.
   */
  private CFANode getOrCreateNewFromOld(CFANode pNode, @Nullable SimpleMap<CFANode, CFANode> pNewToOldMapping) {
    if (pNewToOldMapping == null) {
      return pNode;
    }
    CFANode result = pNewToOldMapping.get(pNode);
    if (result != null) {
      return result;
    }
    int lineNumber = pNode.getLineNumber();
    String functionName = pNode.getFunctionName();
    if (pNode instanceof CLabelNode) {
      result = new CLabelNode(lineNumber, functionName, ((CLabelNode) pNode).getLabel());
    } else if (pNode instanceof CFunctionEntryNode) {
      CFunctionEntryNode functionEntryNode = (CFunctionEntryNode) pNode;
      FunctionExitNode functionExitNode = (FunctionExitNode) getOrCreateNewFromOld(functionEntryNode.getExitNode(), pNewToOldMapping);
      if (functionExitNode.getEntryNode() == null) {
        result = new CFunctionEntryNode(lineNumber, functionEntryNode.getFunctionDefinition(),
            functionExitNode, functionEntryNode.getFunctionParameterNames());
        functionExitNode.setEntryNode((FunctionEntryNode) result);
      } else {
        result = functionExitNode.getEntryNode();
      }
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
    } else if (pNode instanceof CFATerminationNode) {
      result = new CFATerminationNode(lineNumber, functionName);
    } else if (pNode.getClass() != CFANode.class) {
      Class<? extends CFANode> clazz = pNode.getClass();
      Class<?>[] requiredParameterTypes = new Class<?>[] { int.class, String.class };
      for (Constructor<?> cons : clazz.getConstructors()) {
        if (cons.isAccessible() && Arrays.equals(cons.getParameterTypes(), requiredParameterTypes)) {
          try {
            result = (CFANode) cons.newInstance(lineNumber, functionName);
            break;
          } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
              | InvocationTargetException e) {
            result = null;
          }
        }
      }
      if (result == null) {
        result = new CFANode(lineNumber, functionName);
        this.logger.log(Level.WARNING, "Unknown node type " + clazz + "; created copy as instance of base type CFANode.");
      } else {
        this.logger.log(Level.WARNING, "Unknown node type " + clazz + "; created copy by reflection.");
      }
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
  private CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, CFANode pNewPredecessor, CFANode pNewSuccessor, final SimpleMap<CFANode, CFANode> pNewToOldMapping) {
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
    case FunctionCallEdge: {
      if (!(pNewSuccessor instanceof FunctionEntryNode)) {
        throw new IllegalArgumentException("The successor of a function call edge must be a function entry node.");
      }
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pEdge;
      FunctionSummaryEdge oldSummaryEdge = functionCallEdge.getSummaryEdge();
      CFunctionSummaryEdge functionSummaryEdge =
          (CFunctionSummaryEdge) copyCFAEdgeWithNewNodes(
              oldSummaryEdge,
              pNewPredecessor,
              getOrCreateNewFromOld(oldSummaryEdge.getSuccessor(), pNewToOldMapping),
              pNewToOldMapping);
      addToNodes(functionSummaryEdge);
      Optional<CFunctionCall> cFunctionCall = functionCallEdge.getRawAST();
      return new CFunctionCallEdge(rawStatement, lineNumber, pNewPredecessor, (CFunctionEntryNode) pNewSuccessor, cFunctionCall.orNull(), functionSummaryEdge);
    }
    case FunctionReturnEdge:
      if (!(pNewPredecessor instanceof FunctionExitNode)) {
        throw new IllegalArgumentException("The predecessor of a function return edge must be a function exit node.");
      }
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) pEdge;
      CFunctionSummaryEdge oldSummaryEdge = functionReturnEdge.getSummaryEdge();
      CFANode functionCallPred = oldSummaryEdge.getPredecessor();
      CFANode functionSummarySucc = oldSummaryEdge.getSuccessor();
      // If there is a conflicting summary edge, never use the one stored with the function return edge
      if (oldSummaryEdge != functionCallPred.getLeavingSummaryEdge() && functionCallPred.getLeavingSummaryEdge() != null) {
        oldSummaryEdge = (CFunctionSummaryEdge) functionCallPred.getLeavingSummaryEdge();
      } else if (oldSummaryEdge != functionSummarySucc.getEnteringSummaryEdge() && functionSummarySucc.getEnteringSummaryEdge() != null) {
        oldSummaryEdge = (CFunctionSummaryEdge) functionSummarySucc.getEnteringSummaryEdge();
      }
      CFunctionSummaryEdge functionSummaryEdge = (CFunctionSummaryEdge) copyCFAEdgeWithNewNodes(oldSummaryEdge, pNewToOldMapping);
      addToNodes(functionSummaryEdge);
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
      if (statementEdge instanceof CFunctionSummaryStatementEdge) {
        CFunctionSummaryStatementEdge functionStatementEdge = (CFunctionSummaryStatementEdge) pEdge;
        return new CFunctionSummaryStatementEdge(rawStatement, statementEdge.getStatement(), lineNumber, pNewPredecessor, pNewSuccessor, functionStatementEdge.getFunctionCall(), functionStatementEdge.getFunctionName());
      }
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
  private CFAEdge copyCFAEdgeWithNewNodes(CFAEdge pEdge, final SimpleMap<CFANode, CFANode> pNewToOldMapping) {
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
   * @throws InterruptedException if a shutdown has been requested by the registered shutdown notifier.
   */
  private Optional<ImmutableMultimap<String, Loop>> getLoopStructure(CFANode pSingleLoopHead) throws InterruptedException {
    // First, find all nodes reachable via the loop head
    Queue<CFANode> waitlist = new ArrayDeque<>();
    Deque<FunctionSummaryEdge> emptyStack = new ArrayDeque<>();
    Queue<Deque<FunctionSummaryEdge>> callstacks = new ArrayDeque<>();
    Set<CFANode> reachableSuccessors = new HashSet<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.offer(pSingleLoopHead);
    callstacks.offer(emptyStack);
    boolean firstIteration = true;
    while (!waitlist.isEmpty()){
      shutdownNotifier.shutdownIfNecessary();
      CFANode current = waitlist.poll();
      Deque<FunctionSummaryEdge> callstack = callstacks.poll();
      if (firstIteration) {
        firstIteration = false;
      } else {
        reachableSuccessors.add(current);
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
        Deque<FunctionSummaryEdge> newCallstack = callstack;
        if (leavingEdge instanceof FunctionCallEdge) {
          newCallstack = new ArrayDeque<>(newCallstack);
          newCallstack.push(((FunctionCallEdge) leavingEdge).getSummaryEdge());
        } else if (leavingEdge instanceof FunctionReturnEdge) {
          if (newCallstack.isEmpty()) {
            continue;
          }
          newCallstack = new ArrayDeque<>(newCallstack);
          FunctionSummaryEdge summaryEdge = newCallstack.pop();
          if (!summaryEdge.equals(((FunctionReturnEdge) leavingEdge).getSummaryEdge())) {
            continue;
          }
        }
        CFANode successor = leavingEdge.getSuccessor();
        if (visited.add(successor)) {
          waitlist.offer(successor);
          callstacks.offer(newCallstack);
        }
      }
    }

    // If the loop head cannot reach itself, there is no loop
    if (!reachableSuccessors.contains(pSingleLoopHead)) {
      return Optional.of(ImmutableMultimap.<String, Loop>of());
    }

    /*
     * Now, Find all loop nodes by checking which of the nodes reachable via
     * the loop head, the loop head itself is reachable from.
     */
    visited.clear();
    waitlist.offer(pSingleLoopHead);
    callstacks.offer(emptyStack);
    Set<CFANode> loopNodes = new HashSet<>();
    while (!waitlist.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      CFANode current = waitlist.poll();
      Deque<FunctionSummaryEdge> callstack = callstacks.poll();
      if (reachableSuccessors.contains(current)) {
        loopNodes.add(current);
        for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
          Deque<FunctionSummaryEdge> newCallstack = callstack;
          if (enteringEdge instanceof FunctionReturnEdge) {
            newCallstack = new ArrayDeque<>(newCallstack);
            newCallstack.push(((FunctionReturnEdge) enteringEdge).getSummaryEdge());
          } else if (enteringEdge instanceof FunctionCallEdge) {
            if (newCallstack.isEmpty()) {
              continue;
            }
            newCallstack = new ArrayDeque<>(newCallstack);
            FunctionSummaryEdge summaryEdge = newCallstack.pop();
            if (!summaryEdge.equals(((FunctionCallEdge) enteringEdge).getSummaryEdge())) {
              continue;
            }
          }
          CFANode predecessor = enteringEdge.getPredecessor();
          if (visited.add(predecessor)) {
            waitlist.offer(predecessor);
            callstacks.offer(newCallstack);
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
   * Checks if the given edge is a dummy edge.
   *
   * @param pEdge the edge to check.
   * @return <code>true</code> if the edge is a dummy edge, <code>false</code> otherwise.
   */
  private static boolean isDummyEdge(CFAEdge pEdge) {
    return pEdge != null && pEdge.getEdgeType() == CFAEdgeType.BlankEdge && pEdge.getDescription().equals(DUMMY_EDGE);
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

  private static interface ProgramCounterValueProvider {

    int getPCValueFor(CFANode pCFANode);

  }

  private static interface AbstractProgramCounterValueProviderFactory {

    ProgramCounterValueProvider newOrImmutableProgramCounterValueProvider();

  }

  private static enum ProgramCounterValueProviderFactories implements AbstractProgramCounterValueProviderFactory {

    NODE_NUMBER {

      @Override
      public ProgramCounterValueProvider newOrImmutableProgramCounterValueProvider() {
        return NodeNumberProgramCounterValueProvider.INSTANCE;
      }

    },

    INCREMENTAL {

      class IncrementalProgramCounterValueProvider implements ProgramCounterValueProvider {

        private final Map<CFANode, Integer> providedPCValues = new HashMap<>();

        private int lastPCValue = -1;

        @Override
        public int getPCValueFor(CFANode pCFANode) {
          Integer storedPCValue = providedPCValues.get(pCFANode);
          if (storedPCValue == null) {
            storedPCValue = ++lastPCValue;
            providedPCValues.put(pCFANode, storedPCValue);
          }
          return storedPCValue;
        }

      }

      @Override
      public ProgramCounterValueProvider newOrImmutableProgramCounterValueProvider() {
        return new IncrementalProgramCounterValueProvider();
      }

    };

  }

  private static enum NodeNumberProgramCounterValueProvider implements ProgramCounterValueProvider {

    INSTANCE;

    @Override
    public int getPCValueFor(CFANode pCFANode) {
      return pCFANode.getNodeNumber();
    }

  }

  public static interface ProgramCounterValueAssignmentEdge extends CFAEdge {

    public int getProgramCounterValue();

  }

  private static class CProgramCounterValueAssignmentEdge extends CStatementEdge implements ProgramCounterValueAssignmentEdge {

    private int pcValue;

    public CProgramCounterValueAssignmentEdge(String pRawStatement, CStatement pStatement, int pLineNumber,
        CFANode pPredecessor, CFANode pSuccessor, int pPCValue) {
      super(pRawStatement, pStatement, pLineNumber, pPredecessor, pSuccessor);
      this.pcValue = pPCValue;
    }

    @Override
    public int getProgramCounterValue() {
      return this.pcValue;
    }

  }

  private static CProgramCounterValueAssignmentEdge createProgramCounterAssumeEdge(FileLocation pLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      CIdExpression pPCIdExpression,
      int pPCValue) {
    String rawStatement = String.format("%s = %d",  pPCIdExpression.getName(), pPCValue);
    CExpression assignmentExpression = new CIntegerLiteralExpression(pLocation, CNumericTypes.INT, BigInteger.valueOf(pPCValue));
    CStatement statement = new CExpressionAssignmentStatement(pLocation, pPCIdExpression, assignmentExpression);
    return new CProgramCounterValueAssignmentEdge(rawStatement, statement , 0, pPredecessor, pSuccessor, pPCValue);
  }

  /**
   * Edges of this interface are CFA assume edges used in the single loop
   * transformation. They are artificial edges used to encode the control flow
   * through the single loop head into the correct subgraph based on program
   * counter values.
   */
  public static interface ProgramCounterValueAssumeEdge extends CFAEdge {

    /**
     * Gets the program counter value.
     *
     * @return the program counter value.
     */
    public int getProgramCounterValue();

  }

  private static class CProgramCounterValueAssumeEdge extends CAssumeEdge implements ProgramCounterValueAssumeEdge {

    private final int pcValue;

    public CProgramCounterValueAssumeEdge(String pRawStatement, CFANode pPredecessor, CFANode pSuccessor,
        CExpression pExpression, boolean pTruthAssumption, int pPCValue) {
      super(pRawStatement, 0, pPredecessor, pSuccessor, pExpression, pTruthAssumption);
      this.pcValue = pPCValue;
    }

    @Override
    public int getProgramCounterValue() {
      return pcValue;
    }

  }

  private static CProgramCounterValueAssumeEdge createProgramCounterAssumeEdge(FileLocation pLocation,
      CBinaryExpressionBuilder pExpressionBuilder,
      CFANode pPredecessor,
      CFANode pSuccessor,
      CIdExpression pPCIdExpression,
      int pPCValue,
      boolean pTruthAssumption) {
    CExpression assumePCExpression = pExpressionBuilder.buildBinaryExpression(
        pPCIdExpression,
        new CIntegerLiteralExpression(pLocation, CNumericTypes.INT, BigInteger.valueOf(pPCValue)),
        BinaryOperator.EQUALS);
    String rawStatement = String.format("%s == %d",  pPCIdExpression.getName(), pPCValue);
    if (!pTruthAssumption) {
      rawStatement = String.format("!(%s)", rawStatement);
    }
    return new CProgramCounterValueAssumeEdge(rawStatement, pPredecessor, pSuccessor, assumePCExpression, pTruthAssumption, pPCValue);
  }

  /**
   * Instances of this class are acyclic graphs.
   */
  private static class AcyclicGraph {

    /**
     * The set of nodes.
     */
    private final Set<CFANode> nodes = new LinkedHashSet<>();

    /**
     * The set of edges.
     */
    private final Multimap<CFANode, CFAEdge> edges = LinkedHashMultimap.create();

    /**
     * The set of uncommitted nodes.
     */
    private final Set<CFANode> uncommittedNodes = new LinkedHashSet<>();

    /**
     * The set of uncommitted edges.
     */
    private final Multimap<CFANode, CFAEdge> uncommittedEdges = LinkedHashMultimap.create();

    /**
     * The growth strategy.
     */
    private final AcyclicGrowthStrategy growthStrategy;

    /**
     * Creates a new acyclic graph with the given root node and default growth
     * strategy.
     *
     * @param pRoot the root node.
     * @param pGrowthStrategy the growth strategy.
     */
    public AcyclicGraph(CFANode pRoot, AcyclicGrowthStrategy pGrowthStrategy) {
      this.nodes.add(pRoot);
      this.growthStrategy = pGrowthStrategy;
    }

    /**
     * Gets the nodes of the graph as an unmodifiable set.
     *
     * @return the nodes of the graph as an unmodifiable set.
     */
    public Iterable<CFANode> getNodes() {
      return Iterables.concat(Collections.unmodifiableSet(this.nodes), Collections.unmodifiableSet(this.uncommittedNodes));
    }

    /**
     * Gets the edges of the graph as an unmodifiable set.
     *
     * @return the edges of the graph as an unmodifiable set.
     */
    public Iterable<CFAEdge> getEdges() {
      return Iterables.concat(Collections.unmodifiableCollection(this.edges.values()), Collections.unmodifiableCollection(this.uncommittedEdges.values()));
    }

    /**
     * Checks if the given node is contained in this graph.
     *
     * @param pNode the node to look for.
     * @return @{code true} if the node is contained in the graph,
     * @{code false} otherwise.
     */
    public boolean containsNode(CFANode pNode) {
      return this.nodes.contains(pNode) || this.uncommittedNodes.contains(pNode);
    }

    /**
     * Checks if the given edge is contained in this graph.
     *
     * @param pEdge the edge to look for.
     * @return @{code true} if the edge is contained in the graph,
     * @{code false} otherwise.
     */
    public boolean containsEdge(CFAEdge pEdge) {
      return this.edges.containsValue(pEdge) || this.uncommittedEdges.containsValue(pEdge);
    }

    /**
     * Adds the given edge to the graph but does not commit the change.
     *
     * @param pEdge the edge to be added.
     *
     * @throws IllegalArgumentException if the edge cannot be added according
     * to the employed growth strategy.
     */
    public void addEdge(CFAEdge pEdge) {
      Preconditions.checkArgument(offerEdge(pEdge));
    }

    /**
     * If the given edge may be added to the graph according to the growth
     * strategy, it is added but not committed.
     *
     * @param pEdge the candidate edge.
     *
     * @return {@code true} if the edge was added, {@code false} otherwise.
     */
    public boolean offerEdge(CFAEdge pEdge) {
      if (!containsNode(pEdge.getPredecessor()) || introducesLoop(pEdge)) {
        return false;
      }
      this.uncommittedEdges.put(pEdge.getPredecessor(), pEdge);
      this.uncommittedNodes.add(pEdge.getSuccessor());
      return true;
    }

    /**
     * Checks if further growth of the graph is desirable.
     *
     * @return {@code true} if further growth is desirable, {@code false}
     * otherwise.
     */
    public boolean isFurtherGrowthDesired() {
      return this.growthStrategy.isFurtherGrowthDesired(this);
    }

    /**
     * Commits all changes.
     */
    public void commit() {
      this.nodes.addAll(uncommittedNodes);
      this.edges.putAll(uncommittedEdges);
      abort();
    }

    /**
     * Aborts all changes.
     */
    public void abort() {
      this.uncommittedNodes.clear();
      this.uncommittedEdges.clear();
    }

    @Override
    public String toString() {
      return Iterables.toString(getEdges());
    }

    /**
     * Checks if the given control flow edge would introduce a loop to the
     * graph if it was added.
     *
     * @param pEdge the edge to check.
     * @return {@code true} if adding the edge would introduce a loop to the
     * graph, {@code false} otherwise.
     */
    public boolean introducesLoop(CFAEdge pEdge) {
      Set<CFANode> visited = new HashSet<>();
      Queue<CFANode> waitlist = new ArrayDeque<>();
      Queue<Deque<FunctionSummaryEdge>> callstacks = new ArrayDeque<>();
      callstacks.offer(new ArrayDeque<FunctionSummaryEdge>());
      waitlist.offer(pEdge.getSuccessor());
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        if (current.equals(pEdge.getPredecessor())) {
          return true;
        }
        Deque<FunctionSummaryEdge> callstack = callstacks.poll();
        if (visited.add(current)) {
          for (CFAEdge leavingEdge : getLeavingEdges(current)) {
            if (containsEdge(leavingEdge)) {
              Deque<FunctionSummaryEdge> newCallstack = callstack;
              if (leavingEdge instanceof FunctionCallEdge) {
                newCallstack = new ArrayDeque<>(newCallstack);
                newCallstack.push(((FunctionCallEdge) leavingEdge).getSummaryEdge());
              } else if (leavingEdge instanceof FunctionReturnEdge) {
                if (newCallstack.isEmpty()) {
                  continue;
                }
                newCallstack = new ArrayDeque<>(newCallstack);
                FunctionSummaryEdge summaryEdge = newCallstack.pop();
                if (!summaryEdge.equals(((FunctionReturnEdge) leavingEdge).getSummaryEdge())) {
                  continue;
                }
              }
              CFANode succ = leavingEdge.getSuccessor();
              if (containsNode(succ)) {
                waitlist.offer(succ);
                callstacks.offer(newCallstack);
              }
            }
          }
        }
      }
      return false;
    }

    private Iterable<CFAEdge> getLeavingEdges(CFANode pNode) {
      return Iterables.concat(this.edges.get(pNode), this.uncommittedEdges.get(pNode));
    }

    /**
     * Resets the graph and aborts all changes.
     *
     * @param pNewRootNode the new root node.
     *
     * @return this graph with all nodes and edges removed.
     */
    public AcyclicGraph reset(CFANode pNewRootNode) {
      abort();
      this.edges.clear();
      this.nodes.clear();
      this.nodes.add(pNewRootNode);
      return this;
    }

    /**
     * Instances of implementing classes are used as growth strategies for
     * acyclic graphs.
     */
    public static interface AcyclicGrowthStrategy {

      /**
       * Decides whether or not further growth is desired for the given graph.
       *
       * @param pGraph the current graph.
       *
       * @return {@code true} if further growth of the graph is desired,
       * @{code false} otherwise.
       */
      boolean isFurtherGrowthDesired(AcyclicGraph pGraph);

    }

    /**
     * This enum contains different acyclic growth strategies.
     */
    public static enum AcyclicGrowthStrategies implements AcyclicGrowthStrategy {

      /**
       * This growth strategy allows for infinite growth.
       */
      MULTIPLE_PATHS {

        @Override
        public boolean isFurtherGrowthDesired(AcyclicGraph pGraph) {
          return true;
        }

      },

      /**
       * This growth strategy allows for growth along an arbitrary single
       * finite path.
       */
      SINGLE_PATH {

        @Override
        public boolean isFurtherGrowthDesired(AcyclicGraph pGraph) {
          for (CFANode node : pGraph.getNodes()) {
            if (node.getNumLeavingEdges() > 1) {
              return false;
            }
          }
          return true;
        }

      },

      /**
       * This growth strategy advises against any kind of growth.
       */
      SINGLE_EDGE {

        @Override
        public boolean isFurtherGrowthDesired(AcyclicGraph pGraph) {
          return false;
        }

      };

    }

  }

  private static interface SimpleMap<S, T> {

    T get(Object pKey);

    boolean isEmpty();

    T put(S pKey, T pValue);

    boolean containsKey(S pKey);

    void clear();

    public void putAllInto(SimpleMap<S, T> pTarget);

  }

  private static class SimpleMapAdapter<S, T> implements SimpleMap<S, T> {

    private final Map<S, T> adaptee;

    private SimpleMapAdapter(Map<S, T> pAdaptee) {
      Preconditions.checkNotNull(pAdaptee);
      adaptee = pAdaptee;
    }

    @Override
    public T get(Object pKey) {
      return adaptee.get(pKey);
    }

    @Override
    public T put(S pKey, T pValue) {
      return adaptee.put(pKey, pValue);
    }

    @Override
    public boolean containsKey(S pKey) {
      return adaptee.containsKey(pKey);
    }

    @Override
    public void clear() {
      adaptee.clear();
    }

    @Override
    public void putAllInto(SimpleMap<S, T> pTarget) {
      for (Map.Entry<S, T> entry : adaptee.entrySet()) {
        pTarget.put(entry.getKey(), entry.getValue());
      }
    }

    public static <S, T> SimpleMap<S, T> adapt(Map<S, T> pAdaptee) {
      return new SimpleMapAdapter<>(pAdaptee);
    }

    public static <S, T> SimpleMap<S, T> createSimpleHashMap() {
      return adapt(new HashMap<S, T>());
    }

    @Override
    public boolean isEmpty() {
      return adaptee.isEmpty();
    }

    @Override
    public String toString() {
      return adaptee.toString();
    }

  }

  private static class SimpleTransactionMap<S, T> implements SimpleMap<S, T> {

    private final SimpleMap<S, T> persistentWrappee;

    private final SimpleMap<S, T> transactionalWrappee;

    public SimpleTransactionMap(SimpleMap<S, T> pPersistentWrappee, SimpleMap<S, T> pTransactionalWrappee) {
      persistentWrappee = pPersistentWrappee;
      transactionalWrappee = pTransactionalWrappee;
    }

    public SimpleTransactionMap(SimpleMap<S, T> pPersistentWrappee) {
      this(pPersistentWrappee, SimpleMapAdapter.<S, T>createSimpleHashMap());
    }

    @Override
    public T get(Object pKey) {
      T result = persistentWrappee.get(pKey);
      if (result != null) {
        return result;
      }
      return transactionalWrappee.get(pKey);
    }

    @Override
    public T put(S pKey, T pValue) {
      T previous = get(pKey);
      if (previous == null) {
        transactionalWrappee.put(pKey, pValue);
      }
      return previous;
    }

    @Override
    public boolean containsKey(S pKey) {
      return persistentWrappee.containsKey(pKey) || transactionalWrappee.containsKey(pKey);
    }

    public void commit() {
      transactionalWrappee.putAllInto(persistentWrappee);
      abort();
    }

    public void abort() {
      transactionalWrappee.clear();
    }

    @Override
    public void clear() {
      persistentWrappee.clear();
      transactionalWrappee.clear();
    }

    @Override
    public boolean isEmpty() {
      return persistentWrappee.isEmpty() && transactionalWrappee.isEmpty();
    }

    @Override
    public void putAllInto(SimpleMap<S, T> pTarget) {
      persistentWrappee.putAllInto(pTarget);
      transactionalWrappee.putAllInto(pTarget);
    }

    @Override
    public String toString() {
      return String.format("%s; (%s)", persistentWrappee, transactionalWrappee);
    }

  }

}