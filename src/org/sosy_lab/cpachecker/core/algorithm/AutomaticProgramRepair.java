// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class AutomaticProgramRepair
    implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownNotifier shutdownNotifier;


  private final StatTimer totalTime = new StatTimer("Total time for bug repair");

  public AutomaticProgramRepair(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final Specification pSpecification,
      final ShutdownNotifier pShutdownNotifier
  )
      throws InvalidConfigurationException {

    config = pConfig;
    algorithm = pStoreAlgorithm;
    cfa = pCfa;
    logger = pLogger;
    specification = pSpecification;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);

    totalTime.start();

    try {
      logger.log(Level.INFO, "Starting bug repair...");
      logger.log(Level.INFO, "STATUS before: " + status.toString());

      runAlgorithm();

      logger.log(Level.INFO, "Stopping bug repair...");
    } catch (Exception e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } finally{
      totalTime.stop();
    }
    return status;
  }

  private void runAlgorithm()
      throws Exception {
    final MutableCFA clonedCFA = cloneCFA();
    final RepairCandidateCollector repairCandidatesCollector = new RepairCandidateCollector();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(clonedCFA.getMainFunction(), repairCandidatesCollector);

    final List<CFANode> repairCandidateNodes =  repairCandidatesCollector.getRepairCandidateNodes();

    logger.log(Level.INFO,  clonedCFA.getAllNodes().size());

    final MutableCFA mutatedCFA = mutateCFA(clonedCFA, repairCandidateNodes);
    final AlgorithmStatus newStatus = rerun(mutatedCFA);

    logger.log(Level.INFO, "STATUS after: " + newStatus.toString());
    logger.log(Level.INFO,  mutatedCFA.getAllNodes().size());
  }

  private AlgorithmStatus rerun(MutableCFA mutatedCFA)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    final ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    final CPABuilder builder = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
    final ConfigurableProgramAnalysis mutatedCPA = builder.buildCPAs(mutatedCFA, specification, new AggregatedReachedSets());
    final CPAAlgorithm algo = CPAAlgorithm.create(mutatedCPA, logger, config, shutdownNotifier);

    return algo.run(reachedSetFactory.create());
  }

  private MutableCFA cloneCFA() {

    final TreeMultimap<String, CFANode> nodes = TreeMultimap.create();

    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
    }

    return new MutableCFA(cfa.getMachineModel(),
        cfa.getAllFunctions(),
        nodes,
        cfa.getMainFunction(),
        cfa.getFileNames(),
        cfa.getLanguage());
  }

  private MutableCFA mutateCFA(MutableCFA currentCFA, List<CFANode> repairCandidateNodes)
      throws Exception {

    for (CFANode node : repairCandidateNodes) {
      if (shouldDelete()) {
        deleteNode(currentCFA, node);
      }

      if(shouldInsert()){
        insertNode(currentCFA, node);
      }
    }

    return currentCFA;
  }

  /*
  * Deletes a given node from a cfa, along with all nodes that can be reached from it */
  private void deleteNode(MutableCFA currentCFA, CFANode node){
    final Set<CFANode> reachableNodes = Sets.newHashSet();

    reachableNodes.addAll(CFATraversal.dfs().collectNodesReachableFrom(node));

    for (CFANode reachableNode : reachableNodes) {
      currentCFA.removeNode(reachableNode);
      logger.log(Level.INFO, "Deleted " + reachableNode.toString());
    }
  }

  /*
  * Inserts a random node from a given CFA after a given node. */
  private void insertNode(MutableCFA currentCFA, CFANode predecessorNode) throws Exception {
    final Collection<CFANode> originalNodes = cfa.getAllNodes();
    final int insertionNodeIndex = new Random().nextInt(originalNodes.size());
    final CFANode insertionNode = Iterables.get(originalNodes, insertionNodeIndex);

    CFANode successorNode = null;

    for(int i = 0; i <  predecessorNode.getNumLeavingEdges(); i++){
      final CFAEdge edge = predecessorNode.getLeavingEdge(i);
      predecessorNode.removeLeavingEdge(edge);

      final CFAEdge predecessorNodeNewLeavingEdge = alterEdge(edge, edge.getPredecessor(), insertionNode);
      predecessorNode.addLeavingEdge(predecessorNodeNewLeavingEdge);

      successorNode = edge.getSuccessor();
    }

    for(int a = 0; a <  insertionNode.getNumLeavingEdges(); a++){
      final CFAEdge edge = insertionNode.getLeavingEdge(a);
      logger.log(Level.INFO, "Edge " + edge.toString());

      insertionNode.removeLeavingEdge(edge);
      final CFAEdge predecessorNodeNewLeavingEdge = alterEdge(edge, insertionNode, successorNode);
      insertionNode.addLeavingEdge(predecessorNodeNewLeavingEdge);
    }

    currentCFA.addNode(insertionNode);
  }

  /* TODO:
      - it is assumed that the language is C - an error should be thrown if this is not the case
      - In cases where special conditions have to be met, the insertion should not take place if the conditions don't apply, instead of throwing an error
  */

  private CFAEdge alterEdge(CFAEdge edge, CFANode predecessor , CFANode successor)
      throws Exception {
    switch (edge.getEdgeType()) {

      case AssumeEdge:
        final CAssumeEdge assumeEdge = (CAssumeEdge) edge;
          return new CAssumeEdge(assumeEdge.getRawStatement(), assumeEdge.getFileLocation(), predecessor,
              successor, assumeEdge.getExpression(),assumeEdge.getTruthAssumption());

      case FunctionCallEdge:
        final CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) edge;
        final Optional<CFunctionCall> functionCall = functionCallEdge.getRawAST();

        if (functionCall.isPresent()){
          return new CFunctionCallEdge(functionCallEdge.getRawStatement(),
              functionCallEdge.getFileLocation(), predecessor, (CFunctionEntryNode) successor,
              functionCall.get(), functionCallEdge.getSummaryEdge());
        } else {
          /* TODO throw proper error */
          throw new Exception("Cannot extract functional call: " + successor.getClass());
        }

      case FunctionReturnEdge:
        final CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) edge;
        /* TODO reconsider casting predecessor */
        return new CFunctionReturnEdge(functionReturnEdge.getFileLocation(),
            (FunctionExitNode) predecessor, successor, functionReturnEdge.getSummaryEdge());

      case DeclarationEdge:
        final CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        return new CDeclarationEdge(declarationEdge.getRawStatement(), declarationEdge.getFileLocation(),
      declarationEdge.getPredecessor(), declarationEdge.getSuccessor(), declarationEdge.getDeclaration());

      case StatementEdge:
        final CStatementEdge statementEdge = (CStatementEdge) edge;
        return new CStatementEdge(statementEdge.getRawStatement(), statementEdge.getStatement(),
            statementEdge.getFileLocation(), predecessor, successor);

      case ReturnStatementEdge:
        final CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) edge;
        final Optional<CReturnStatement> optionalReturnStatement = returnStatementEdge.getRawAST();

        if(optionalReturnStatement.isPresent()){
          /* TODO reconsider casting successor */
          return new CReturnStatementEdge(returnStatementEdge.getRawStatement(), optionalReturnStatement.get(),
              returnStatementEdge.getFileLocation(), predecessor, (FunctionExitNode)  successor);
        } else {
          /* TODO throw proper error */
          throw new Exception("Cannot extract return statement");
        }

      case BlankEdge:
        final BlankEdge blankEdge = (BlankEdge) edge;
        return new BlankEdge(blankEdge.getRawStatement(), blankEdge.getFileLocation(),  predecessor,
          successor, blankEdge.getDescription());

      case CallToReturnEdge:
        final CFunctionSummaryEdge functionSummaryEdge = (CFunctionSummaryEdge) edge;

        return new CFunctionSummaryEdge(functionSummaryEdge.getRawStatement(), functionSummaryEdge.getFileLocation(),
            predecessor, successor, functionSummaryEdge.getExpression(),
            functionSummaryEdge.getFunctionEntry());
      default:
        throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private static class RepairCandidateCollector extends ForwardingCFAVisitor {
    private final List<CFANode> repairCandidateNodes = new ArrayList<>();

    public RepairCandidateCollector() {
      super(new EdgeCollectingCFAVisitor());
    }


    @Override
    public TraversalProcess visitNode(CFANode node) {
      if (isCandidate()) {
        repairCandidateNodes.add(node);
      }
      return super.visitNode(node);
    }

    private boolean isCandidate() {
      return Math.random() >= 0.7;
    }

    private List<CFANode> getRepairCandidateNodes() {
      return repairCandidateNodes;
    }
  }

  private boolean shouldDelete() {
    return Math.random() >= 0.8;
  }

  private boolean shouldInsert() {
    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

}
