// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.base.Preconditions;
import com.google.common.collect.TreeMultimap;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;

public class Mutator {
  private CFA cfa;
  private CFAEdge originalEdge;

  public Mutator(CFA pCfa, CFAEdge pOriginalEdge) {
    cfa = cloneCFA(pCfa);
    originalEdge = CorrespondingEdgeProvider.findCorrespondingEdge(pOriginalEdge, cfa);
  }

  /* TODO create deep copy */
  static MutableCFA cloneCFA(CFA cfa) {
    final TreeMultimap<String, CFANode> nodes = TreeMultimap.create();

    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(
          function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
    }

    MutableCFA clonedCFA =
        new MutableCFA(
            cfa.getMachineModel(),
            cfa.getAllFunctions(),
            nodes,
            cfa.getMainFunction(),
            cfa.getFileNames(),
            cfa.getLanguage());

    cfa.getLoopStructure().ifPresent(clonedCFA::setLoopStructure);
    cfa.getLiveVariables().ifPresent(clonedCFA::setLiveVariables);

    return clonedCFA;
  }

  public Set<? extends Mutation> calcPossibleMutations() {
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
        return Set.of();
    }
  }

  /* EDGES  */
  private Set<SimpleMutation> generateAssumeEdgeMutations(CAssumeEdge originalAssumeEdge) {

    return ExpressionMutator.calcMutationsFor(originalAssumeEdge.getExpression(), cfa).stream()
        .map(
            alternativeExpression ->
                new SimpleMutation(
                    originalAssumeEdge,
                    EdgeMutator.replaceExpression(originalAssumeEdge, alternativeExpression),
                    cfa))
        .collect(Collectors.toSet());
  }

  private Set<SimpleMutation> generateStatementEdgeMutations(CStatementEdge originalStatementEdge) {
    return StatementMutator.calcMutationsFor(originalStatementEdge.getStatement(), cfa).stream()
        .map(
            newStatement ->
                new SimpleMutation(
                    originalStatementEdge,
                    EdgeMutator.replaceExpression(originalStatementEdge, newStatement),
                    cfa))
        .collect(Collectors.toSet());
  }

  private Set<FunctionCallMutation> generateFunctionCallEdgeMutations(
      CFunctionCallEdge originalFunctionCallEdge) {
    CFunctionSummaryEdge summaryEdge = originalFunctionCallEdge.getSummaryEdge();

    return generateFunctionCallMutations(
        originalFunctionCallEdge,
        CorrespondingEdgeProvider.findCorrespondingFunctionReturnEdge(summaryEdge),
        summaryEdge);
  }

  private Set<FunctionCallMutation> generateFunctionReturnEdgeMutations(
      CFunctionReturnEdge originalReturnEdge) {
    CFunctionSummaryEdge summaryEdge = originalReturnEdge.getSummaryEdge();

    return generateFunctionCallMutations(
        CorrespondingEdgeProvider.findCorrespondingFunctionCallEdge(summaryEdge),
        originalReturnEdge,
        summaryEdge);
  }

  private Set<FunctionCallMutation> generateFunctionSummaryEdgeMutations(
      CFunctionSummaryEdge originalSummaryEdge) {
    return generateFunctionCallMutations(
        CorrespondingEdgeProvider.findCorrespondingFunctionCallEdge(originalSummaryEdge),
        CorrespondingEdgeProvider.findCorrespondingFunctionReturnEdge(originalSummaryEdge),
        originalSummaryEdge);
  }

  private Set<FunctionCallMutation> generateFunctionCallMutations(
      CFunctionCallEdge callEdge,
      CFunctionReturnEdge returnEdge,
      CFunctionSummaryEdge summaryEdge) {
    CStatement functionCall = summaryEdge.getExpression();

    return StatementMutator.calcMutationsFor(functionCall, cfa).stream()
        .map(
            newFunctionCallStatement -> {
              CFunctionCall newFunctionCall = (CFunctionCall) newFunctionCallStatement;
              CFunctionSummaryEdge newSummaryEdge =
                  EdgeMutator.replaceFunctionCall(summaryEdge, newFunctionCall);
              CFunctionCallEdge newFunctionCallEdge =
                  EdgeMutator.replaceFunctionCall(callEdge, newSummaryEdge, newFunctionCall);
              CFunctionReturnEdge newFunctionReturnEdge =
                  EdgeMutator.replaceFunctionCall(returnEdge, newSummaryEdge);

              return new FunctionCallMutation(
                  originalEdge, newSummaryEdge, newFunctionCallEdge, newFunctionReturnEdge, cfa);
            })
        .collect(Collectors.toSet());
  }

  private Set<SimpleMutation> generateReturnStatementEdgeMutations(
      CReturnStatementEdge originalReturnStatementEdge) {
    Preconditions.checkNotNull(originalReturnStatementEdge.getRawAST());

    CReturnStatement returnStatement = originalReturnStatementEdge.getRawAST().get();

    Preconditions.checkNotNull(returnStatement.getReturnValue());
    Preconditions.checkNotNull(returnStatement.asAssignment());

    CAssignment returnAssignment = returnStatement.asAssignment().get();

    return StatementMutator.calcMutationsFor(returnAssignment, cfa).stream()
        .filter(assignment -> assignment.getRightHandSide() instanceof CExpression)
        .map(
            assignment ->
                new SimpleMutation(
                    originalReturnStatementEdge,
                    EdgeMutator.replaceAssignment(
                        originalReturnStatementEdge, returnStatement, assignment),
                    cfa))
        .collect(Collectors.toSet());
  }
}
