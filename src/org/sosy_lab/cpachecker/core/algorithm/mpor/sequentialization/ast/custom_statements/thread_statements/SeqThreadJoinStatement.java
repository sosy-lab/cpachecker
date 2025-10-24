// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public class SeqThreadJoinStatement extends ASeqThreadStatement {

  private final Optional<CIdExpression> joinedThreadExitVariable;

  private final CBinaryExpression joinedThreadNotActive;

  SeqThreadJoinStatement(
      MPOROptions pOptions,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      CBinaryExpression pJoinedThreadNotActive,
      CLeftHandSide pPcLeftHandSide) {

    super(
        pOptions,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of());
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadNotActive;
  }

  private SeqThreadJoinStatement(
      MPOROptions pOptions,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CBinaryExpression pJoinedThreadActive,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadActive;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CFunctionCallStatement assumeCall = SeqAssumptionBuilder.buildAssumption(joinedThreadNotActive);
    String returnValueRead =
        buildReturnValueRead(joinedThreadExitVariable, substituteEdges)
            .orElse(SeqSyntax.EMPTY_STRING);
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return Joiner.on(SeqSyntax.SPACE).join(assumeCall.toASTString(), returnValueRead, injected);
  }

  private static Optional<String> buildReturnValueRead(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges) {

    if (pJoinedThreadExitVariable.isPresent()) {
      SubstituteEdge substituteEdge = pSubstituteEdges.iterator().next();
      int index =
          PthreadFunctionType.PTHREAD_JOIN.getParameterIndex(PthreadObjectType.RETURN_VALUE);
      CExpression returnValueParameter =
          CFAUtils.getParameterAtIndex(substituteEdge.cfaEdge, index);
      if (returnValueParameter instanceof CUnaryExpression unaryExpression) {
        // extract retval from unary expression &retval
        if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
          if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
            CExpressionAssignmentStatement assignment =
                SeqStatementBuilder.buildExpressionAssignmentStatement(
                    idExpression, pJoinedThreadExitVariable.orElseThrow());
            return Optional.of(assignment.toASTString());
          } else {
            // just in case
            throw new IllegalArgumentException("pthread_join retval must be CIdExpression");
          }
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public SeqThreadJoinStatement withTargetPc(int pTargetPc) {
    return new SeqThreadJoinStatement(
        options,
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqThreadJoinStatement(
        options,
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadJoinStatement(
        options,
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

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
