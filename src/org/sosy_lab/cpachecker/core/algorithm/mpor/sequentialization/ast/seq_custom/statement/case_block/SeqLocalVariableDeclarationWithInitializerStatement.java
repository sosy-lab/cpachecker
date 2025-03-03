// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Standard C does not allow function declarations or definitions inside functions. Type
 * declarations are allowed, but CPAcheckers preprocessing puts them at the top, together with other
 * declarations. Thus, only (local) variable declarations inside functions have to handled
 * explicitly for the sequentialization.
 */
public class SeqLocalVariableDeclarationWithInitializerStatement implements SeqCaseBlockStatement {

  private final CVariableDeclaration variableDeclaration;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  private void checkArguments(CVariableDeclaration pVariableDeclaration) {
    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");
    checkArgument(
        pVariableDeclaration.getInitializer() != null,
        "pVariableDeclaration must have an initializer");
  }

  SeqLocalVariableDeclarationWithInitializerStatement(
      CVariableDeclaration pVariableDeclaration, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    checkArguments(pVariableDeclaration);
    variableDeclaration = pVariableDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
  }

  private SeqLocalVariableDeclarationWithInitializerStatement(
      CVariableDeclaration pVariableDeclaration,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    checkArguments(pVariableDeclaration);
    variableDeclaration = pVariableDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
  }

  private SeqLocalVariableDeclarationWithInitializerStatement(
      CVariableDeclaration pVariableDeclaration,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    checkArguments(pVariableDeclaration);
    variableDeclaration = pVariableDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
  }

  @Override
  public String toASTString() {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetPcExpression, concatenatedStatements);
    return variableDeclaration.toASTStringWithOnlyNameAndInitializer()
        + SeqSyntax.SPACE
        + targetStatements;
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
  public SeqLocalVariableDeclarationWithInitializerStatement cloneWithTargetPc(
      CExpression pTargetPc) {

    return new SeqLocalVariableDeclarationWithInitializerStatement(
        variableDeclaration, pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    return new SeqLocalVariableDeclarationWithInitializerStatement(
        variableDeclaration, pcLeftHandSide, pConcatenatedStatements);
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
