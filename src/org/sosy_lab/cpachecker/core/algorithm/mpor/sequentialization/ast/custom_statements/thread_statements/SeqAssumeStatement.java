// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.BranchType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a conditional case block statement with {@code if} and {@code else if} statements. */
public class SeqAssumeStatement extends CSeqThreadStatement {

  public final BranchType branchType;

  private final Optional<CExpression> ifExpression;

  SeqAssumeStatement(
      MPOROptions pOptions,
      BranchType pBranchType,
      Optional<CExpression> pIfExpression,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(
        pOptions,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of());

    checkArgument(
        pBranchType.equals(BranchType.IF) || pBranchType.equals(BranchType.ELSE),
        "pStatementType must be IF or ELSE");
    checkArgument(
        !pBranchType.equals(BranchType.IF) || pIfExpression.isPresent(),
        "if pStatementType is IF, then pIfExpression must be present");

    branchType = pBranchType;
    ifExpression = pIfExpression;
  }

  private SeqAssumeStatement(
      MPOROptions pOptions,
      BranchType pBranchType,
      Optional<CExpression> pIfExpression,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    branchType = pBranchType;
    ifExpression = pIfExpression;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // TODO use SeqBranchStatement here
    String controlFlowPrefix;
    if (branchType.equals(BranchType.IF)) {
      controlFlowPrefix =
          branchType.buildPrefix(ifExpression.orElseThrow().toASTString())
              + SeqSyntax.CURLY_BRACKET_LEFT;
    } else {
      controlFlowPrefix =
          SeqSyntax.CURLY_BRACKET_RIGHT + branchType.buildPrefix() + SeqSyntax.CURLY_BRACKET_LEFT;
    }
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    if (branchType.equals(BranchType.IF)) {
      return Joiner.on(SeqSyntax.NEWLINE).join(controlFlowPrefix, injected);
    } else {
      // need additional closing bracket for the "else" statement
      return Joiner.on(SeqSyntax.NEWLINE)
          .join(controlFlowPrefix, injected, SeqSyntax.CURLY_BRACKET_RIGHT);
    }
  }

  @Override
  public SeqAssumeStatement withTargetPc(int pTargetPc) {
    return new SeqAssumeStatement(
        options,
        branchType,
        ifExpression,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqAssumeStatement(
        options,
        branchType,
        ifExpression,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqAssumeStatement(
        options,
        branchType,
        ifExpression,
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
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
