// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class AutomaticProgramRepair
    implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final CFA cfa;


  private final StatTimer totalTime = new StatTimer("Total time for bug repair");

  public AutomaticProgramRepair(
      final Algorithm pStoreAlgorithm,
      final LogManager pLogger,
      final CFA pCfa){

    algorithm = pStoreAlgorithm;
    cfa = pCfa;
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);

    totalTime.start();

    try {
      logger.log(Level.INFO, "Starting bug repair...");

      runAlgorithm();

      logger.log(Level.INFO, "Stopping bug repair...");
    } finally{
      totalTime.stop();
    }
    return status;
  }

  private void runAlgorithm(){
    Iterable<? extends AAstNode> repairCandidates = calcRepairCandidates();

    for (AAstNode node : repairCandidates) {
      logger.log(Level.INFO,  node.toASTString());

      if (node instanceof CBinaryExpression){
        CBinaryExpression binNode = (CBinaryExpression) node;

        if (binNode.getOperator().isLogicalOperator()) {
          CBinaryExpression patchCandidate = calcFlippedExpression(binNode);

          logger.log(Level.INFO, "This might be the cause: " + node.toASTString());
          logger.log(Level.INFO, "This might fix it: " + patchCandidate.toASTString());
        }
      }
    }
  }

  private CBinaryExpression calcFlippedExpression(CBinaryExpression binExp){
    return new CBinaryExpression(binExp.getFileLocation(),
        binExp.getExpressionType(),
        binExp.getCalculationType(),
        binExp.getOperand1(),
        binExp.getOperand2(),
        binExp.getOperator().getOppositLogicalOperator());
  }

  private Iterable<? extends AAstNode>  calcRepairCandidates(){
    RepairCandidatesCollector repairCandidatesCollector = new RepairCandidatesCollector();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(), repairCandidatesCollector);

    return repairCandidatesCollector.getRepairCandidates();
  }

  private static class RepairCandidatesCollector extends ForwardingCFAVisitor {
    Iterable<? extends AAstNode> repairCandidates = FluentIterable.of() ;

    public RepairCandidatesCollector() {
      super(new NodeCollectingCFAVisitor());
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      Iterable<? extends AAstNode> candidates =
          FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(edge))
              .transformAndConcat(CFAUtils::traverseRecursively)
              .filter(predicate);

      repairCandidates =
          Iterables.concat(repairCandidates, candidates);

      return super.visitEdge(edge);
    }

    Predicate<AAstNode> predicate = input -> Math.random() >= 0.7;

    private Iterable<? extends AAstNode> getRepairCandidates() {
      return repairCandidates;
    }

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
