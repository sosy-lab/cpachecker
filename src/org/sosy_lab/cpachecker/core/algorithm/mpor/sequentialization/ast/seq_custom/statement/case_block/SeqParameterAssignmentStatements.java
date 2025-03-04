// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents the assignment of a parameter given to a function to an injected parameter variable in
 * the sequentialization.
 *
 * <p>E.g. {@code __MPOR_SEQ__THREAD0_PARAM_q = GLOBAL_queue; }
 */
public class SeqParameterAssignmentStatements implements SeqCaseBlockStatement {

  private final ImmutableList<FunctionParameterAssignment> assignments;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  SeqParameterAssignmentStatements(
      ImmutableList<FunctionParameterAssignment> pAssignments,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    assignments = pAssignments;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
  }

  private SeqParameterAssignmentStatements(
      ImmutableList<FunctionParameterAssignment> pAssignments,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    assignments = pAssignments;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
  }

  private SeqParameterAssignmentStatements(
      ImmutableList<FunctionParameterAssignment> pAssignments,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    assignments = pAssignments;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
  }

  @Override
  public String toASTString() {
    StringBuilder rString = new StringBuilder();
    for (FunctionParameterAssignment assignment : assignments) {
      rString.append(assignment.statement.toASTString()).append(SeqSyntax.SPACE);
    }
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetPcExpression, concatenatedStatements);
    rString.append(targetStatements);
    return rString.toString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqParameterAssignmentStatements cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqParameterAssignmentStatements(assignments, pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqParameterAssignmentStatements(
        assignments, pcLeftHandSide, pConcatenatedStatements);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
