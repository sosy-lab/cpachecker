// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public class SeqThreadJoinStatement implements SeqThreadStatement {

  private final Optional<CIdExpression> joinedThreadExitVariable;

  private final CBinaryExpression joinedThreadNotActive;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockGotoLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      CBinaryExpression pJoinedThreadNotActive,
      CLeftHandSide pPcLeftHandSide) {

    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadNotActive;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CBinaryExpression pJoinedThreadActive,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockGotoLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    joinedThreadExitVariable = pJoinedThreadExitVariable;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    joinedThreadNotActive = pJoinedThreadActive;
    pcLeftHandSide = pPcLeftHandSide;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CFunctionCallStatement assumeCall = SeqStatementBuilder.buildAssumeCall(joinedThreadNotActive);
    Optional<String> returnValueRead =
        buildReturnValueRead(joinedThreadExitVariable, substituteEdges);
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return assumeCall.toASTString()
        + (returnValueRead.isPresent()
            ? SeqSyntax.SPACE + returnValueRead.orElseThrow()
            : SeqSyntax.EMPTY_STRING)
        + SeqSyntax.SPACE
        + targetStatements;
  }

  private static Optional<String> buildReturnValueRead(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges) {

    if (pJoinedThreadExitVariable.isPresent()) {
      SubstituteEdge substituteEdge = pSubstituteEdges.iterator().next();
      CExpression retvalParameter =
          CFAUtils.getParameterAtIndex(
              substituteEdge.cfaEdge, PthreadFunctionType.PTHREAD_JOIN.getReturnValueIndex());
      if (retvalParameter instanceof CUnaryExpression unaryExpression) {
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
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockGotoLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) {
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
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockGotoLabelStatement pLabel) {
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
  public SeqThreadStatement cloneWithInjectedStatements(
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

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
