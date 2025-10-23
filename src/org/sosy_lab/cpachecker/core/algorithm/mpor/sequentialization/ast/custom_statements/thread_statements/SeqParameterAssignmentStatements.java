// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents the assignment of a parameter given to a function to an injected parameter variable in
 * the sequentialization.
 */
public class SeqParameterAssignmentStatements implements SeqThreadStatement {

  private final MPOROptions options;

  private final ImmutableList<FunctionParameterAssignment> assignments;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqParameterAssignmentStatements(
      MPOROptions pOptions,
      ImmutableList<FunctionParameterAssignment> pAssignments,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    options = pOptions;
    assignments = pAssignments;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqParameterAssignmentStatements(
      MPOROptions pOptions,
      ImmutableList<FunctionParameterAssignment> pAssignments,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    assignments = pAssignments;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner rString = new StringJoiner(SeqSyntax.SPACE);
    for (FunctionParameterAssignment assignment : assignments) {
      rString.add(assignment.toExpressionAssignmentStatement().toASTString());
    }
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return rString.add(injected).toString();
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
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqParameterAssignmentStatements cloneWithTargetPc(int pTargetPc) {
    return new SeqParameterAssignmentStatements(
        options,
        assignments,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqParameterAssignmentStatements(
        options,
        assignments,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    return new SeqParameterAssignmentStatements(
        options,
        assignments,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return new SeqParameterAssignmentStatements(
        options,
        assignments,
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
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }

  public ImmutableList<FunctionParameterAssignment> getAssignments() {
    return assignments;
  }
}
