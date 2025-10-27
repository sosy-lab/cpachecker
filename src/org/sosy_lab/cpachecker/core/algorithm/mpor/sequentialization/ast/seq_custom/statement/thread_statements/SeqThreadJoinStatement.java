// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CFAEdgeSubstitute;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public class SeqThreadJoinStatement implements SeqThreadStatement {

  private final MPOROptions options;

  private final Optional<CIdExpression> joinedThreadExitVariable;

  private final CBinaryExpression joinedThreadNotActive;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<CFAEdgeSubstitute> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqThreadJoinStatement(
      MPOROptions pOptions,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      int pTargetPc,
      CBinaryExpression pJoinedThreadNotActive,
      CLeftHandSide pPcLeftHandSide) {

    options = pOptions;
    joinedThreadExitVariable = pJoinedThreadExitVariable;
    joinedThreadNotActive = pJoinedThreadNotActive;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqThreadJoinStatement(
      MPOROptions pOptions,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CBinaryExpression pJoinedThreadActive,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
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
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges) {

    if (pJoinedThreadExitVariable.isPresent()) {
      CFAEdgeSubstitute substituteEdge = pSubstituteEdges.iterator().next();
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
  public ImmutableSet<CFAEdgeSubstitute> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) {
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
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
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
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    return new SeqThreadJoinStatement(
        options,
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return new SeqThreadJoinStatement(
        options,
        joinedThreadExitVariable,
        joinedThreadNotActive,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        SeqThreadStatementUtil.appendInjectedStatements(this, pAppendedInjectedStatements));
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
