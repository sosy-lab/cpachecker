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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public final class SeqReachErrorStatement extends CSeqThreadStatement {

  public static final String REACH_ERROR_FUNCTION_NAME = "reach_error";

  private static final CFunctionTypeWithNames REACH_ERROR_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(), false);

  public static final CFunctionDeclaration REACH_ERROR_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          REACH_ERROR_FUNCTION_TYPE,
          REACH_ERROR_FUNCTION_NAME,
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  private static final CIdExpression REACH_ERROR_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallExpression REACH_ERROR_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          REACH_ERROR_ID_EXPRESSION,
          ImmutableList.of(),
          REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallStatement REACH_ERROR_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, REACH_ERROR_FUNCTION_CALL_EXPRESSION);

  SeqReachErrorStatement(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc);
  }

  private SeqReachErrorStatement(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(
        pOptions,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        pInjectedStatements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return REACH_ERROR_FUNCTION_CALL_STATEMENT.toASTString() + SeqSyntax.SPACE + injected;
  }

  @Override
  public SeqReachErrorStatement withTargetPc(int pTargetPc) {
    checkArgument(
        pTargetPc == ProgramCounterVariables.EXIT_PC,
        this.getClass().getSimpleName() + " should only be cloned with exit pc %s",
        ProgramCounterVariables.EXIT_PC);
    return new SeqReachErrorStatement(options, pcLeftHandSide, substituteEdges, pTargetPc);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have target goto");
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqReachErrorStatement(
        options, pcLeftHandSide, substituteEdges, targetPc.orElseThrow(), pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return false;
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
