// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Standard C does not allow function declarations or definitions inside functions. Type
 * declarations are allowed, but CPAcheckers preprocessing puts them at the top, together with other
 * declarations. Thus, only (local) variable declarations inside functions have to handled
 * explicitly for the sequentialization.
 */
public class SeqLocalVariableDeclarationWithInitializerStatement extends ASeqThreadStatement {

  private final CVariableDeclaration variableDeclaration;

  private void checkArguments(CVariableDeclaration pVariableDeclaration) {
    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");
    checkArgument(
        pVariableDeclaration.getInitializer() != null,
        "pVariableDeclaration must have an initializer");
  }

  SeqLocalVariableDeclarationWithInitializerStatement(
      MPOROptions pOptions,
      CVariableDeclaration pVariableDeclaration,
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
    checkArguments(pVariableDeclaration);
    variableDeclaration = pVariableDeclaration;
  }

  private SeqLocalVariableDeclarationWithInitializerStatement(
      MPOROptions pOptions,
      CVariableDeclaration pVariableDeclaration,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    checkArguments(pVariableDeclaration);
    variableDeclaration = pVariableDeclaration;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return variableDeclaration.toASTStringWithoutStorageClassAndType(AAstNodeRepresentation.DEFAULT)
        + SeqSyntax.SPACE
        + injected;
  }

  @Override
  public SeqLocalVariableDeclarationWithInitializerStatement withTargetPc(int pTargetPc) {
    return new SeqLocalVariableDeclarationWithInitializerStatement(
        options,
        variableDeclaration,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqLocalVariableDeclarationWithInitializerStatement(
        options,
        variableDeclaration,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqLocalVariableDeclarationWithInitializerStatement(
        options,
        variableDeclaration,
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

  public CVariableDeclaration getVariableDeclaration() {
    return variableDeclaration;
  }
}
