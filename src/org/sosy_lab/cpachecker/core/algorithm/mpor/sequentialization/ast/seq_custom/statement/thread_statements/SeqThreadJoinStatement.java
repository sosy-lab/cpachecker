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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
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

  public final CIdExpression threadJoinsThreadVariable;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockGotoLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CIdExpression pThreadJoinsThreadVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide) {

    joinedThreadExitVariable = pJoinedThreadExitVariable;
    threadJoinsThreadVariable = pThreadJoinsThreadVariable;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CIdExpression pThreadJoinsThreadVariable,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockGotoLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    joinedThreadExitVariable = pJoinedThreadExitVariable;
    threadJoinsThreadVariable = pThreadJoinsThreadVariable;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    pcLeftHandSide = pPcLeftHandSide;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement setJoinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoinsThreadVariable, SeqIntegerLiteralExpression.INT_0);

    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    Optional<String> returnValueRead = Optional.empty();
    if (joinedThreadExitVariable.isPresent()) {
      SubstituteEdge substituteEdge = substituteEdges.iterator().next();
      CExpression retvalParameter =
          CFAUtils.getParameterAtIndex(
              substituteEdge.cfaEdge, PthreadFunctionType.PTHREAD_JOIN.getReturnValueIndex());
      if (retvalParameter instanceof CUnaryExpression unaryExpression) {
        // extract retval from unary expression &retval
        if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
          if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
            CExpressionAssignmentStatement assignment =
                SeqStatementBuilder.buildExpressionAssignmentStatement(
                    idExpression, joinedThreadExitVariable.orElseThrow());
            returnValueRead = Optional.of(assignment.toASTString());
          } else {
            // just in case
            throw new IllegalArgumentException("pthread_join retval must be CIdExpression");
          }
        }
      }
    }

    return setJoinsFalse.toASTString()
        + (returnValueRead.isPresent()
            ? SeqSyntax.SPACE + returnValueRead.orElseThrow()
            : SeqSyntax.EMPTY_STRING)
        + SeqSyntax.SPACE
        + targetStatements;
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
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        SeqThreadStatementClauseUtil.replaceTargetGotoLabel(injectedStatements, pTargetPc));
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockGotoLabelStatement pLabel) {
    return new SeqThreadJoinStatement(
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
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
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
