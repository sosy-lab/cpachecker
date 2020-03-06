/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options
public class CFAMutator extends CFACreator {
  @Option(
      secure = true,
      name = "analysis.runMutationsCount",
      description =
          "if analysis.runMutations is true and this option is not 0, this option limits count of runs")
  private int runMutationsCount = 0;

  private ParseResult parseResult = null;
  private Set<CFANode> originalNodes = null;
  private Set<CFAEdge> originalEdges = null;

  private Set<CFANode> deletedNodes = new HashSet<>();
  private Set<Deque<CFANode>> deletedChains = new HashSet<>();
  private CFA lastCFA = null;
  private boolean doLastRun = false;
  private boolean wasRollback = false;
  private RandomEdgeRemovalStrategy strategy = new RandomEdgeRemovalStrategy();

  private static class CFAMutatorStatistics extends CFACreatorStatistics {
    private final StatTimer mutationTimer = new StatTimer("Time for mutations");
    private final StatTimer clearingTimer = new StatTimer("Time for clearing postprocessings");
    private final StatCounter mutationsAttempted = new StatCounter("Mutations attempted");
    private final StatCounter mutationsDone = new StatCounter("Mutations done");
    private final StatCounter rollbacksDone = new StatCounter("Rollbacks done");
    private long possibleMutations;

    private CFAMutatorStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      super.printStatistics(out, pResult, pReached);
      StatisticsWriter.writingStatisticsTo(out)
          .beginLevel()
          .put(mutationTimer)
          .put(clearingTimer)
          .put(mutationsAttempted)
          .put(mutationsDone)
          .put(rollbacksDone)
          .put("Mutations remained", possibleMutations - mutationsAttempted.getValue())
          .endLevel();
    }

    @Override
    public @Nullable String getName() {
      return "CFA mutation";
    }
  }

  public CFAMutator(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier);

    pConfig.inject(this, CFAMutator.class);
  }

  private void addNode(CFANode pNode) {
    logger.logf(Level.FINER, "adding node %s", pNode);
    assert parseResult.getCFANodes().put(pNode.getFunctionName(), pNode);
    originalNodes.add(pNode);
  }

  private void addEdge(CFAEdge pEdge) {
    logger.logf(Level.INFO, "adding edge %s", pEdge);
    CFANode pred = pEdge.getPredecessor();
    assert !pred.hasEdgeTo(pEdge.getSuccessor());
    assert !CFAUtils.enteringEdges(pred).contains(pEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(pEdge);
    originalEdges.add(pEdge);
  }

  private void addEdgeToNode(CFAEdge pEdge, CFANode pNode) {
    logger.logf(Level.INFO, "adding edge %s to node %s", pEdge, pNode);
    if (pEdge.getPredecessor() == pNode) {
      assert !pNode.hasEdgeTo(pEdge.getSuccessor());
      pNode.addLeavingEdge(pEdge);
    } else if (pEdge.getSuccessor() == pNode) {
      assert !CFAUtils.enteringEdges(pNode).contains(pEdge);
      pNode.addEnteringEdge(pEdge);
    } else {
      assert false : "Tried to add edge " + pEdge + " to node " + pNode;
    }
    originalEdges.add(pEdge);
  }

  private void removeEdge(CFAEdge pEdge) {
    logger.logf(Level.INFO, "removing edge %s", pEdge);
    CFACreationUtils.removeEdgeFromNodes(pEdge);
    originalEdges.remove(pEdge);
  }

  private void removeNode(CFANode pNode) {
    logger.logf(Level.INFO, "removing node %s", pNode);
    assert parseResult.getCFANodes().remove(pNode.getFunctionName(), pNode);
    originalNodes.remove(pNode);
  }

  private void removeEdgeFromNode(CFAEdge pEdge, CFANode pNode) {
    logger.logf(Level.FINER, "removing edge %s from node %s", pEdge, pNode);
    if (pEdge.getPredecessor() == pNode) {
      pNode.removeLeavingEdge(pEdge);
    } else if (pEdge.getSuccessor() == pNode) {
      pNode.removeEnteringEdge(pEdge);
    } else {
      assert false : "Tried to remove edge " + pEdge + " from node " + pNode;
    }
    originalEdges.remove(pEdge);
  }

  // kind of main function
  @Override
  protected ParseResult parseToCFAs(final List<String> sourceFiles)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    if (parseResult == null) { // do zero-th run, init

      parseResult = super.parseToCFAs(sourceFiles);
      originalNodes = new HashSet<>(parseResult.getCFANodes().values());

      final EdgeCollectingCFAVisitor visitor = new EdgeCollectingCFAVisitor();
      for (final FunctionEntryNode entryNode : parseResult.getFunctions().values()) {
        CFATraversal.dfs().traverseOnce(entryNode, visitor);
      }

      originalEdges = Sets.newIdentityHashSet();
      originalEdges.addAll(visitor.getVisitedEdges());

      ((CFAMutatorStatistics) stats).possibleMutations =
          strategy.countPossibleMutations(parseResult);
      return parseResult;

    } else if (!doLastRun) { // do non-last run
      doMutationAftermath();

      if (((CFAMutatorStatistics) stats).mutationsAttempted.getValue() == runMutationsCount) {
        doLastRun = true;
      } else {

        ((CFAMutatorStatistics) stats).mutationTimer.start();
        doLastRun = !mutate();
        ((CFAMutatorStatistics) stats).mutationTimer.stop();
      }
      // need to return after possible rollback
      return parseResult;

    } else { // do last run
      // need to clear out deleted in case we run out of times
      doMutationAftermath();

      exportCFAAsync(lastCFA);
      return null;
    }
  }

  private void doMutationAftermath() {
    if (((CFAMutatorStatistics) stats).mutationsAttempted.getValue() == 0) {
      clearParseResultAfterPostprocessings();
    } else if (wasRollback) {
      ((CFAMutatorStatistics) stats).rollbacksDone.inc();
      wasRollback = false;
    } else if (!doLastRun) {
      clearChainsRemains();
      clearParseResultAfterPostprocessings();
      ((CFAMutatorStatistics) stats).mutationsDone.inc();
    }

    deletedNodes.clear();
    deletedChains.clear();
  }

  private void clearChainsRemains() {
    for (Deque<CFANode> chain : deletedChains) {
      for (CFANode node : chain) {
        assert node.getNumLeavingEdges() == 1;
        originalEdges.remove(node.getLeavingEdge(0));
      } // actually last edge was already removed
    }
  }

  private boolean mutate() {
    ((CFAMutatorStatistics) stats).mutationsAttempted.inc();
    Collection<CFANode> chosenNodes = strategy.chooseNodesToRemove(parseResult);
    if (chosenNodes.isEmpty()) {
      return false;
    }
    Set<CFANode> nodesRemained = new HashSet<>(chosenNodes);

    for (CFANode node : chosenNodes) {
      if (!nodesRemained.contains(node)) {
        continue;
      }

      Deque<CFANode> chain = pollChainFrom(nodesRemained, node);
      if (chain.size() > 1) {
        removeChain(chain);
      } else {
        removeLeavingEdgeAndConnectEnteringEdgeAround(node);
      }
    }

    if (chosenNodes.equals(nodesRemained)) {
      return false;
    }

    return true;
  }

  // undo last mutation
  public void rollback() {
    wasRollback = true;
    clearParseResultAfterPostprocessings();
    for (CFANode node : deletedNodes) {
      returnNodeWithLeavingEdge(node);
    }
    for (final Deque<CFANode> chain : deletedChains) {
      returnChainWithLastEdge(chain);
    }
  }

  private void clearParseResultAfterPostprocessings() {
    ((CFAMutatorStatistics) stats).clearingTimer.start();
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    final NodeCollectingCFAVisitor nodeCollector = new NodeCollectingCFAVisitor();
    final CFATraversal.CompositeCFAVisitor visitor =
        new CompositeCFAVisitor(edgeCollector, nodeCollector);

    for (final FunctionEntryNode entryNode : parseResult.getFunctions().values()) {
      CFATraversal.dfs().traverse(entryNode, visitor);
    }

    Set<CFAEdge> tmpSet = new IdentityHashSet<>();
    tmpSet.addAll(edgeCollector.getVisitedEdges());
    final Set<CFAEdge> edgesToRemove =
        Sets.difference(tmpSet, originalEdges);
    final Set<CFAEdge> edgesToAdd = Sets.difference(originalEdges, tmpSet);
    final Set<CFANode> nodesToRemove =
        Sets.difference(new HashSet<>(nodeCollector.getVisitedNodes()), originalNodes);
    final Set<CFANode> nodesToAdd =
        Sets.difference(originalNodes, new HashSet<>(nodeCollector.getVisitedNodes()));

    // finally remove nodes and edges added as global decl. and interprocedural
    SortedSetMultimap<String, CFANode> nodes = parseResult.getCFANodes();
    for (CFANode n : nodesToRemove) {
      logger.logf(
          Level.FINEST,
          "clearing: removing node %s (was present: %s)",
          n,
          nodes.remove(n.getFunctionName(), n));
    }
    for (CFANode n : nodesToAdd) {
      logger.logf(
          Level.FINEST,
          "clearing: returning node %s:%s (inserted: %s)",
          n.getFunctionName(),
          n,
          nodes.put(n.getFunctionName(), n));
    }
    for (CFAEdge e : edgesToRemove) {
      logger.logf(Level.FINEST, "clearing: removing edge %s", e);
      CFACreationUtils.removeEdgeFromNodes(e);
    }
    for (CFAEdge e : edgesToAdd) {
      logger.logf(Level.FINEST, "clearing: returning edge %s", e);
      // have to check because chain removing does not remove edges from originalEdges right away
      if (!e.getPredecessor().hasEdgeTo(e.getSuccessor())) {
        CFACreationUtils.addEdgeUnconditionallyToCFA(e);
      }
    }
    ((CFAMutatorStatistics) stats).clearingTimer.stop();
  }

  private class NodePollingChainVisitor extends CFATraversal.DefaultCFAVisitor {
    private Deque<CFANode> chainNodes = new ArrayDeque<>();
    private final Collection<CFANode> nodes;
    public boolean forwards;

    public NodePollingChainVisitor(Collection<CFANode> pNodes, boolean pForwards) {
      nodes = pNodes;
      forwards = pForwards;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      assert !chainNodes.contains(pNode) : pNode.toString() + " is already in chain " + chainNodes;

      if (!nodes.remove(pNode)) {
        return TraversalProcess.SKIP;
      }

      assert pNode.getNumEnteringEdges() == 1 && pNode.getNumLeavingEdges() == 1
          : pNode.toString() + " is in nodes-to-delete but has ill count of edges";

      if (forwards) {
        chainNodes.addLast(pNode);
      } else {
        chainNodes.addFirst(pNode);
      }
      return TraversalProcess.CONTINUE;
    }

    public void changeDirection() {
      forwards = !forwards;
    }

    public Deque<CFANode> getChain() {
      return chainNodes;
    }
  }

  public Deque<CFANode> pollChainFrom(Set<CFANode> pNodes, CFANode pNode) {
    assert pNodes.contains(pNode);

    NodePollingChainVisitor oneWayChainVisitor = new NodePollingChainVisitor(pNodes, false);
    CFATraversal.dfs().backwards().traverse(pNode, oneWayChainVisitor);
    oneWayChainVisitor.changeDirection();
    CFATraversal.dfs().traverse(pNode.getLeavingEdge(0).getSuccessor(), oneWayChainVisitor);
    return oneWayChainVisitor.getChain();
  }

  private void removeChain(Deque<CFANode> pChain) {
    CFANode firstNode = pChain.getFirst();
    CFANode lastNode = pChain.getLast();

    assert lastNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = lastNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    assert successor.getNumEnteringEdges() == 1;
    removeEdgeFromNode(leavingEdge, successor);

    assert firstNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = firstNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    removeEdgeFromNode(enteringEdge, predecessor);

    for (CFANode node : pChain) {
      removeNode(node);
    }

    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    addEdge(newEdge);

    deletedChains.add(pChain);
    logger.logf(Level.INFO, "replacing chain %s with edge %s", pChain, newEdge);
  }

  private void returnChainWithLastEdge(final Deque<CFANode> pChain) {
    CFANode firstNode = pChain.getFirst();
    CFANode lastNode = pChain.getLast();

    assert firstNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = firstNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    CFAEdge insertedEdge = predecessor.getLeavingEdge(0);
    removeEdge(insertedEdge);
    addEdgeToNode(enteringEdge, predecessor);

    assert lastNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = lastNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    addEdgeToNode(leavingEdge, successor);

    for (CFANode node : pChain) {
      addNode(node);
    }

    logger.logf(Level.INFO, "returning chain %s with edge %s", pChain, leavingEdge);
  }

  // remove the node with its only leaving and entering edges
  // and insert new edge similar to entering edge.
  private void removeLeavingEdgeAndConnectEnteringEdgeAround(CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    assert successor.getNumEnteringEdges() == 1;
    removeEdgeFromNode(leavingEdge, successor);

    assert pNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = pNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    removeEdgeFromNode(enteringEdge, predecessor);

    removeNode(pNode);
    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    addEdge(newEdge);

    deletedNodes.add(pNode);
    logger.logf(Level.INFO, "removing node %s with edge %s", pNode, leavingEdge);
  }

  // undo removing a node with leaving edge:
  // insert node, delete inserted edge, reconnect edges
  private void returnNodeWithLeavingEdge(CFANode pNode) {
    assert pNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = pNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    CFAEdge insertedEdge = predecessor.getLeavingEdge(0);
    removeEdge(insertedEdge);
    addEdgeToNode(enteringEdge, predecessor);

    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    addEdgeToNode(leavingEdge, successor);
    addNode(pNode);

    logger.logf(Level.INFO, "returning node %s with edge %s", pNode, leavingEdge);
  }

  // return an edge with same "contents" but from pPredNode to pSuccNode
  private CFAEdge dupEdge(CFAEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {
    if (pPredecessor == null) {
      pPredecessor = pEdge.getPredecessor();
    }
    if (pSuccessor == null) {
      pSuccessor = pEdge.getSuccessor();
    }

    assert pPredecessor.getFunctionName().equals(pSuccessor.getFunctionName());

    CFAEdge newEdge = new DummyCFAEdge(pPredecessor, pSuccessor);

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        if (pEdge instanceof CAssumeEdge) {
          CAssumeEdge cAssumeEdge = (CAssumeEdge) pEdge;
          newEdge =
              new CAssumeEdge(
                  cAssumeEdge.getRawStatement(),
                  cAssumeEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  cAssumeEdge.getExpression(),
                  cAssumeEdge.getTruthAssumption(),
                  cAssumeEdge.isSwapped(),
                  cAssumeEdge.isArtificialIntermediate());
        } else if (pEdge instanceof JAssumeEdge) {
          JAssumeEdge jAssumeEdge = (JAssumeEdge) pEdge;
          newEdge =
              new JAssumeEdge(
                  jAssumeEdge.getRawStatement(),
                  jAssumeEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  jAssumeEdge.getExpression(),
                  jAssumeEdge.getTruthAssumption());
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case BlankEdge:
        BlankEdge blankEdge = (BlankEdge) pEdge;
        newEdge =
            new BlankEdge(
                blankEdge.getRawStatement(),
                blankEdge.getFileLocation(),
                pPredecessor,
                pSuccessor,
                blankEdge.getDescription());
        break;
      case DeclarationEdge:
        if (pEdge instanceof CDeclarationEdge) {
          CDeclarationEdge cDeclarationEdge = (CDeclarationEdge) pEdge;
          newEdge =
              new CDeclarationEdge(
                  cDeclarationEdge.getRawStatement(),
                  cDeclarationEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  cDeclarationEdge.getDeclaration());
        } else if (pEdge instanceof JDeclarationEdge) {
          JDeclarationEdge jDeclarationEdge = (JDeclarationEdge) pEdge;
          newEdge =
              new JDeclarationEdge(
                  jDeclarationEdge.getRawStatement(),
                  jDeclarationEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  jDeclarationEdge.getDeclaration());
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case StatementEdge:
        if (pEdge instanceof CStatementEdge) {
          CStatementEdge cStatementEdge = (CStatementEdge) pEdge;
          newEdge =
              new CStatementEdge(
                  cStatementEdge.getRawStatement(),
                  cStatementEdge.getStatement(),
                  cStatementEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor);
        } else if (pEdge instanceof JStatementEdge) {
          JStatementEdge jStatementEdge = (JStatementEdge) pEdge;
          newEdge =
              new JStatementEdge(
                  jStatementEdge.getRawStatement(),
                  jStatementEdge.getStatement(),
                  jStatementEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor);
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case FunctionCallEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
      case CallToReturnEdge:
      default:
        throw new UnsupportedOperationException(
            "Unsupported type of edge " + pEdge.getEdgeType() + " at edge " + pEdge);
    }

    logger.logf(Level.FINER, "duplicated edge %s as %s", pEdge, newEdge);

    return newEdge;
  }

  @Override
  protected void exportCFAAsync(final CFA cfa) {
    if (cfa == null) {
      return;
    }

    logger.logf(Level.FINE, "Count of CFA nodes: %d", cfa.getAllNodes().size());

    if (doLastRun) {
      super.exportCFAAsync(cfa);
    } else {
      lastCFA = cfa;
    }
  }

  @Override
  protected CFAMutatorStatistics createStatistics(LogManager pLogger) {
    return new CFAMutatorStatistics(pLogger);
  }
}
