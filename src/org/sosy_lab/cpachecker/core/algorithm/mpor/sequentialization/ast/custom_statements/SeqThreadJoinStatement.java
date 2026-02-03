// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public final class SeqThreadJoinStatement extends CSeqThreadStatement {

  private final Optional<CIdExpression> joinedThreadExitVariable;

  private final CBinaryExpression joinedThreadNotActive;

  SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      CBinaryExpression pJoinedThreadNotActive,
      CLeftHandSide pPcLeftHandSide) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadNotActive;
  }

  private SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CBinaryExpression pJoinedThreadActive,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadActive;
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    CFunctionCallStatement assumeCall =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(joinedThreadNotActive);

    if (joinedThreadExitVariable.isPresent()) {
      CExportStatement returnValueRead =
          new CStatementWrapper(
              buildReturnValueRead(joinedThreadExitVariable.orElseThrow(), substituteEdges));
      return buildExportStatements(new CStatementWrapper(assumeCall), returnValueRead);
    }

    return buildExportStatements(new CStatementWrapper(assumeCall));
  }

  private static CStatement buildReturnValueRead(
      CIdExpression pJoinedThreadExitVariable, ImmutableSet<SubstituteEdge> pSubstituteEdges) {

    SubstituteEdge substituteEdge = pSubstituteEdges.iterator().next();
    int returnValueIndex =
        PthreadFunctionType.PTHREAD_JOIN.getParameterIndex(PthreadObjectType.RETURN_VALUE);
    CFunctionCall functionCall =
        PthreadUtil.tryGetFunctionCallFromCfaEdge(substituteEdge.cfaEdge).orElseThrow();
    CExpression returnValueParameter =
        functionCall.getFunctionCallExpression().getParameterExpressions().get(returnValueIndex);
    if (returnValueParameter instanceof CUnaryExpression unaryExpression) {
      // extract retval from unary expression &retval
      if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
          return SeqStatementBuilder.buildExpressionAssignmentStatement(
              idExpression, pJoinedThreadExitVariable);
        }
      }
    }
    throw new AssertionError(
        "pthread_join retval could not be extracted from the following expression: "
            + returnValueParameter);
  }

  @Override
  public SeqThreadJoinStatement withTargetPc(int pTargetPc) {
    return new SeqThreadJoinStatement(
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqThreadJoinStatement(
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadJoinStatement(
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return true;
  }
}
