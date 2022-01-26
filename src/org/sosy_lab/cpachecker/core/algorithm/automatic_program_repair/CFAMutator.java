// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.base.Preconditions;
import java.util.stream.Stream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * This class mutates a given edge in a CFA. It clones the original CFA and then generates new edges
 * to replace the given edge.
 */
public class CFAMutator {
  private final CFA cfa;
  private final CFAEdge originalEdge;
  private final Configuration config;
  private final LogManager logger;

  public CFAMutator(CFA pCfa, CFAEdge pOriginalEdge, Configuration pConfig, LogManager pLogger) {
    cfa = pCfa;
    originalEdge = pOriginalEdge;
    config = pConfig;
    logger = pLogger;
  }

  /** Returns a set of possible mutations for the given edge based on the edge type. */
  public Stream<? extends Mutation> calcPossibleMutations() {
    switch (originalEdge.getEdgeType()) {
      case AssumeEdge:
        return generateAssumeEdgeMutations((CAssumeEdge) originalEdge);

      case StatementEdge:
        return generateStatementEdgeMutations((CStatementEdge) originalEdge);

      case FunctionCallEdge:
        return generateFunctionCallEdgeMutations((CFunctionCallEdge) originalEdge);

      case FunctionReturnEdge:
        return generateFunctionReturnEdgeMutations((CFunctionReturnEdge) originalEdge);

      case CallToReturnEdge:
        return generateFunctionSummaryEdgeMutations((CFunctionSummaryEdge) originalEdge);

      case ReturnStatementEdge:
        return generateReturnStatementEdgeMutations((CReturnStatementEdge) originalEdge);

      default:
        return Stream.empty();
    }
  }

  /* EDGES  */
  private Stream<Mutation> generateAssumeEdgeMutations(CAssumeEdge originalAssumeEdge) {
    return ExpressionMutator.calcMutationsFor(originalAssumeEdge.getExpression(), cfa)
        .map(
            alternativeExpression -> {
              final AssumeEdgeMutator edgeMutator =
                  new AssumeEdgeMutator(cfa, config, logger, originalAssumeEdge);
              return new AssumeEdgeMutation(
                  originalAssumeEdge,
                  edgeMutator.replaceExpressionInAssumeEdgeAggregate(alternativeExpression),
                  edgeMutator.getClonedCFA());
            });
  }

  private Stream<SimpleMutation> generateStatementEdgeMutations(
      CStatementEdge originalStatementEdge) {
    return StatementMutator.calcMutationsFor(originalStatementEdge.getStatement(), cfa)
        .map(
            newStatement -> {
              final SimpleEdgeMutator edgeMutator =
                  new SimpleEdgeMutator(cfa, config, logger, originalStatementEdge);

              return new SimpleMutation(
                  originalStatementEdge,
                  edgeMutator.replaceStatementInStatementEdge(newStatement),
                  edgeMutator.getClonedCFA());
            });
  }

  private Stream<FunctionCallMutation> generateFunctionCallEdgeMutations(
      CFunctionCallEdge originalFunctionCallEdge) {
    return generateFunctionCallMutations(new FunctionCallEdgeAggregate(originalFunctionCallEdge));
  }

  private Stream<FunctionCallMutation> generateFunctionReturnEdgeMutations(
      CFunctionReturnEdge originalReturnEdge) {
    return generateFunctionCallMutations(new FunctionCallEdgeAggregate(originalReturnEdge));
  }

  private Stream<FunctionCallMutation> generateFunctionSummaryEdgeMutations(
      CFunctionSummaryEdge originalSummaryEdge) {
    return generateFunctionCallMutations(new FunctionCallEdgeAggregate(originalSummaryEdge));
  }

  private Stream<FunctionCallMutation> generateFunctionCallMutations(
      FunctionCallEdgeAggregate originalFunctionCallAggregate) {
    CStatement functionCall = originalFunctionCallAggregate.getFunctionCall();

    return StatementMutator.calcMutationsFor(functionCall, cfa)
        .map(
            newFunctionCallStatement -> {
              CFunctionCall newFunctionCall = (CFunctionCall) newFunctionCallStatement;
              FunctionCallMutator functionCallMutator =
                  new FunctionCallMutator(cfa, config, logger, originalFunctionCallAggregate);
              FunctionCallEdgeAggregate newFunctionCallEdgeAggregate =
                  functionCallMutator.replaceFunctionCall(newFunctionCall);

              return new FunctionCallMutation(
                  originalEdge,
                  newFunctionCallEdgeAggregate,
                  originalFunctionCallAggregate,
                  functionCallMutator.getClonedCFA());
            });
  }

  private Stream<SimpleMutation> generateReturnStatementEdgeMutations(
      CReturnStatementEdge originalReturnStatementEdge) {
    Preconditions.checkNotNull(originalReturnStatementEdge.getRawAST());

    CReturnStatement returnStatement =
        (CReturnStatement) originalReturnStatementEdge.getRawAST().orElseThrow();

    Preconditions.checkNotNull(returnStatement.getReturnValue());
    Preconditions.checkNotNull(returnStatement.asAssignment());

    CAssignment returnAssignment = returnStatement.asAssignment().orElseThrow();

    return StatementMutator.calcMutationsFor(returnAssignment, cfa)
        .filter(assignment -> assignment.getRightHandSide() instanceof CExpression)
        .map(
            assignment -> {
              final SimpleEdgeMutator edgeMutator =
                  new SimpleEdgeMutator(cfa, config, logger, originalReturnStatementEdge);

              return new SimpleMutation(
                  originalReturnStatementEdge,
                  edgeMutator.replaceReturnExpressionInReturnStatementEdge(
                      returnStatement, assignment),
                  edgeMutator.getClonedCFA());
            });
  }
}
