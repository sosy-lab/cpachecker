// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public class SeqThreadJoinStatement implements SeqCaseBlockStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final Optional<CIdExpression> joinedThreadExitVariable;

  public final CIdExpression threadJoinsThreadVariable;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqThreadJoinStatement(
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CIdExpression pThreadJoinsThreadVariable,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide) {

    loopHeadLabel = Optional.empty();
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    threadJoinsThreadVariable = pThreadJoinsThreadVariable;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqThreadJoinStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CIdExpression pThreadJoinsThreadVariable,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    threadJoinsThreadVariable = pThreadJoinsThreadVariable;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    pcLeftHandSide = pPcLeftHandSide;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setJoinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoinsThreadVariable, SeqIntegerLiteralExpression.INT_0);

    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);

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

    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + setJoinsFalse.toASTString()
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
  public Optional<SeqLoopHeadLabelStatement> getLoopHeadLabel() {
    return loopHeadLabel;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqThreadJoinStatement(
        loopHeadLabel,
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    return new SeqThreadJoinStatement(
        loopHeadLabel,
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadJoinStatement(
        loopHeadLabel,
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqThreadJoinStatement(
        Optional.of(pLoopHeadLabel),
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqThreadJoinStatement(
        loopHeadLabel,
        joinedThreadExitVariable,
        threadJoinsThreadVariable,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.empty(),
        injectedStatements,
        pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
