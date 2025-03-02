// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqLocalVariableDeclarationWithInitializerStatement implements SeqCaseBlockStatement {

  private final CDeclaration declaration;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private void checkArguments(CDeclaration pDeclaration) {
    checkArgument(
        pDeclaration instanceof CVariableDeclaration || pDeclaration instanceof CTypeDeclaration,
        "pDeclaration must be variable or type declaration");
    checkArgument(!pDeclaration.isGlobal(), "pDeclaration must be local");
    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      checkArgument(
          variableDeclaration.getInitializer() != null, "pDeclaration must have an initializer");
    }
  }

  SeqLocalVariableDeclarationWithInitializerStatement(
      CDeclaration pDeclaration, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    checkArguments(pDeclaration);
    declaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  private SeqLocalVariableDeclarationWithInitializerStatement(
      CDeclaration pDeclaration, CLeftHandSide pPcLeftHandSide, CExpression pTargetPc) {

    checkArguments(pDeclaration);
    declaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    if (declaration instanceof CVariableDeclaration variableDeclaration) {
      return variableDeclaration.toASTStringWithOnlyNameAndInitializer()
          + SeqSyntax.SPACE
          + pcWrite.toASTString();
    } else {
      // TODO test this
      throw new AssertionError(
          "type def declarations inside functions are currently not supported");
      /*Verify.verify(declaration instanceof CTypeDeclaration);
      CTypeDeclaration typeDeclaration = (CTypeDeclaration) declaration;
      return typeDeclaration.toASTString()
          + SeqSyntax.SPACE
          + pcWrite.toASTString();*/
    }
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
  @NonNull
  public SeqLocalVariableDeclarationWithInitializerStatement cloneWithTargetPc(
      CExpression pTargetPc) {

    return new SeqLocalVariableDeclarationWithInitializerStatement(
        declaration, pcLeftHandSide, pTargetPc);
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
