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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqLocalVariableDeclarationWithInitializerStatement implements SeqCaseBlockStatement {

  private final CVariableDeclaration variableDeclaration;

  private final CLeftHandSide pcLeftHandSide;

  private final int targetPc;

  protected SeqLocalVariableDeclarationWithInitializerStatement(
      CVariableDeclaration pVariableDeclaration, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");
    checkArgument(
        pVariableDeclaration.getInitializer() != null,
        "pVariableDeclaration must have an initializer");
    variableDeclaration = pVariableDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqLocalVariableDeclarationWithInitializerStatement cloneWithTargetPc(
      int pTargetPc) throws UnrecognizedCodeException {

    return new SeqLocalVariableDeclarationWithInitializerStatement(
        variableDeclaration, pcLeftHandSide, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public String toASTString() {
    return variableDeclaration.toASTStringWithoutStorageClass();
  }
}
