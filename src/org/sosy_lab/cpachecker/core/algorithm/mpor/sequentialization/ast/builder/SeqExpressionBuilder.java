// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorGlobalVariable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressionBuilder {

  // CArraySubscriptExpression =====================================================================

  public static CArraySubscriptExpression buildPcSubscriptExpression(CExpression pSubscriptExpr) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY, SeqArrayType.INT_ARRAY, SeqIdExpression.DUMMY_PC, pSubscriptExpr);
  }

  static ImmutableList<CArraySubscriptExpression> buildArrayPcExpressions(int pNumThreads) {
    Builder<CArraySubscriptExpression> rArrayPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rArrayPc.add(buildPcSubscriptExpression(buildIntegerLiteralExpression(i)));
    }
    return rArrayPc.build();
  }

  // CBinaryExpression =============================================================================

  /**
   * Returns {@code pc[pThreadId] != -1} for array and {@code pc{pThreadId} != -1} for scalar {@code
   * pc}.
   */
  public static CBinaryExpression buildPcUnequalExitPc(
      PcVariables pPcVariables, int pThreadId, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        pPcVariables.get(pThreadId),
        SeqIntegerLiteralExpression.INT_EXIT_PC,
        BinaryOperator.NOT_EQUALS);
  }

  /** Returns {@code next_thread != pThreadId}. */
  public static CBinaryExpression buildNextThreadUnequal(
      int pThreadId, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        SeqIdExpression.NEXT_THREAD,
        buildIntegerLiteralExpression(pThreadId),
        BinaryOperator.NOT_EQUALS);
  }

  public static CBinaryExpression buildBitVectorEvaluationByEncoding(
      BitVectorEncoding pEncoding,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<BitVectorGlobalVariable> pBitVectorGlobalVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, HEXADECIMAL -> {
        CIdExpression bitVector = pBitVectorVariables.get(pActiveThread);
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.bitVectors.values().stream()
                .filter(b -> !b.equals(bitVector))
                .collect(ImmutableSet.toImmutableSet());
        yield SeqExpressionBuilder.buildBitVectorEvaluation(
            bitVector, otherBitVectors, pBinaryExpressionBuilder);
      }
      case SCALAR ->
          buildScalarBitVectorEvaluation(
              pActiveThread, pBitVectorGlobalVariables, pBinaryExpressionBuilder);
    };
  }

  private static CBinaryExpression buildBitVectorEvaluation(
      CIdExpression pActiveBitVector,
      // TODO make list
      ImmutableSet<CExpression> pOtherBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOtherBitVectors.isEmpty());
    checkArgument(!pOtherBitVectors.contains(pActiveBitVector));

    CExpression rightHandSide =
        nestBinaryExpressions(pOtherBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pActiveBitVector, rightHandSide, BinaryOperator.BINARY_AND);
  }

  private static CBinaryExpression buildScalarBitVectorEvaluation(
      MPORThread pActiveThread,
      ImmutableList<BitVectorGlobalVariable> pAllGlobalVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Builder<CExpression> orVariableExpression = ImmutableList.builder();

    for (BitVectorGlobalVariable bitVectorGlobalVariable : pAllGlobalVariables) {
      assert bitVectorGlobalVariable.accessVariables.isPresent() : "no access variables present";
      ImmutableMap<MPORThread, CIdExpression> accessVariables =
          bitVectorGlobalVariable.accessVariables.orElseThrow();
      assert accessVariables.containsKey(pActiveThread) : "no variable found for active thread";
      CIdExpression activeVariable = accessVariables.get(pActiveThread);
      assert activeVariable != null;
      ImmutableList<CExpression> otherVariables =
          accessVariables.values().stream()
              .filter(v -> !v.equals(activeVariable))
              .collect(ImmutableList.toImmutableList());
      CExpression rightHandSide =
          nestBinaryExpressions(otherVariables, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
      CBinaryExpression andExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              activeVariable, rightHandSide, BinaryOperator.BINARY_AND);
      orVariableExpression.add(andExpression);
    }
    CExpression rNested =
        nestBinaryExpressions(
            orVariableExpression.build(), BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    assert rNested instanceof CBinaryExpression : "nested expression must be binary expression";
    return (CBinaryExpression) rNested;
  }

  private static CExpression nestBinaryExpressions(
      ImmutableCollection<CExpression> pAllExpressions,
      BinaryOperator pBinaryOperator,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {
    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pAllExpressions.iterator().next();
    for (CExpression nextExpression : pAllExpressions) {
      if (!nextExpression.equals(rNested)) {
        rNested =
            pBinaryExpressionBuilder.buildBinaryExpression(
                rNested, nextExpression, pBinaryOperator);
      }
    }
    return rNested;
  }

  // CFunctionCallExpression =======================================================================

  /**
   * Returns the {@link CFunctionCallExpression} of {@code reach_error("{pFile}", {pLine},
   * "{pFunction}")}
   */
  public static CFunctionCallExpression buildReachError(String pFile, int pLine, String pFunction) {

    CStringLiteralExpression file =
        buildStringLiteralExpression(SeqStringUtil.wrapInQuotationMarks(pFile));
    CIntegerLiteralExpression line = buildIntegerLiteralExpression(pLine);
    CStringLiteralExpression function =
        buildStringLiteralExpression(SeqStringUtil.wrapInQuotationMarks(pFunction));

    return buildFunctionCallExpression(
        SeqVoidType.VOID,
        SeqIdExpression.REACH_ERROR,
        ImmutableList.of(file, line, function),
        SeqFunctionDeclaration.REACH_ERROR);
  }

  // TODO add function that takes MPOROptions and returns (u)int
  public static CFunctionCallExpression buildVerifierNondetInt() {
    return buildFunctionCallExpression(
        SeqSimpleType.INT,
        SeqIdExpression.VERIFIER_NONDET_INT,
        ImmutableList.of(),
        SeqFunctionDeclaration.VERIFIER_NONDET_INT);
  }

  public static CFunctionCallExpression buildVerifierNondetUint() {
    return buildFunctionCallExpression(
        SeqSimpleType.UNSIGNED_INT,
        SeqIdExpression.VERIFIER_NONDET_UINT,
        ImmutableList.of(),
        SeqFunctionDeclaration.VERIFIER_NONDET_UINT);
  }

  private static CFunctionCallExpression buildFunctionCallExpression(
      CType pType,
      CExpression pFunctionName,
      List<CExpression> pParameters,
      CFunctionDeclaration pDeclaration) {

    return new CFunctionCallExpression(
        FileLocation.DUMMY, pType, pFunctionName, pParameters, pDeclaration);
  }

  // CIntegerLiteralExpression =====================================================================

  public static CIntegerLiteralExpression buildIntegerLiteralExpression(int pValue) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, SeqSimpleType.INT, BigInteger.valueOf(pValue));
  }

  // CIdExpression =================================================================================

  /**
   * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} =
   * {pInitializer};}.
   */
  public static CIdExpression buildIdExpressionWithIntegerInitializer(
      String pVarName, CInitializer pInitializer) {

    CVariableDeclaration varDec =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, SeqSimpleType.INT, pVarName, pInitializer);
    return new CIdExpression(FileLocation.DUMMY, varDec);
  }

  public static CIdExpression buildIdExpression(CSimpleDeclaration pDeclaration) {
    return new CIdExpression(FileLocation.DUMMY, pDeclaration);
  }

  static ImmutableList<CIdExpression> buildScalarPcExpressions(int pNumThreads) {
    Builder<CIdExpression> rScalarPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      CInitializer initializer = i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1;
      rScalarPc.add(
          new CIdExpression(
              FileLocation.DUMMY,
              SeqDeclarationBuilder.buildVariableDeclaration(
                  false, SeqSimpleType.INT, SeqToken.pc + i, initializer)));
    }
    return rScalarPc.build();
  }

  // CStringLiteralExpression ======================================================================

  public static CStringLiteralExpression buildStringLiteralExpression(String pValue) {
    return new CStringLiteralExpression(FileLocation.DUMMY, pValue);
  }
}
