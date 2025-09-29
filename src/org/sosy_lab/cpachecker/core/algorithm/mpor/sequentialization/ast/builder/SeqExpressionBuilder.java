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
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressionBuilder {

  // CArraySubscriptExpression =====================================================================

  public static CArraySubscriptExpression buildPcSubscriptExpression(CExpression pSubscriptExpr) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY,
        SeqArrayType.UNSIGNED_INT_ARRAY,
        SeqIdExpression.DUMMY_PC,
        pSubscriptExpr);
  }

  // CBinaryExpression =============================================================================

  public static ImmutableList<CBinaryExpression> buildThreadNotActiveExpressions(
      ImmutableList<CLeftHandSide> pPcLeftHandSides,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CBinaryExpression> rExpressions = ImmutableList.builder();
    for (CLeftHandSide pcLeftHandSide : pPcLeftHandSides) {
      rExpressions.add(buildPcEqualExitPc(pcLeftHandSide, pBinaryExpressionBuilder));
    }
    return rExpressions.build();
  }

  /**
   * Returns {@code pc[pThreadId] != -1} for array and {@code pc{pThreadId} != -1} for scalar {@code
   * pc}.
   */
  public static CBinaryExpression buildPcUnequalExitPc(
      CLeftHandSide pPcLeftHandSide, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        pPcLeftHandSide, SeqIntegerLiteralExpression.INT_EXIT_PC, BinaryOperator.NOT_EQUALS);
  }

  /**
   * Returns {@code pc[pThreadId] == -1} for array and {@code pc{pThreadId} == -1} for scalar {@code
   * pc}.
   */
  public static CBinaryExpression buildPcEqualExitPc(
      CLeftHandSide pPcLeftHandSide, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        pPcLeftHandSide, SeqIntegerLiteralExpression.INT_EXIT_PC, BinaryOperator.EQUALS);
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
        CVoidType.VOID,
        SeqIdExpression.REACH_ERROR,
        ImmutableList.of(file, line, function),
        SeqFunctionDeclaration.REACH_ERROR);
  }

  public static Optional<CFunctionCallExpression> buildVerifierNondetByType(CType pType) {
    for (VerifierNondetFunctionType nondetType : VerifierNondetFunctionType.values()) {
      if (nondetType.getReturnType().equals(pType)) {
        return Optional.of(nondetType.getFunctionCallExpression());
      }
    }
    return Optional.empty();
  }

  public static CFunctionCallExpression buildFunctionCallExpression(
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
        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(pValue));
  }

  // CIdExpression =================================================================================

  /**
   * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVariableName} =
   * {pInitializer};}.
   */
  public static CIdExpression buildIdExpressionWithIntegerInitializer(
      boolean pIsGlobal, CSimpleType pType, String pVariableName, CInitializer pInitializer) {

    CVariableDeclaration variableDeclaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            pIsGlobal, pType, pVariableName, pInitializer);
    return new CIdExpression(FileLocation.DUMMY, variableDeclaration);
  }

  public static CIdExpression buildIdExpression(CSimpleDeclaration pDeclaration) {
    return new CIdExpression(FileLocation.DUMMY, pDeclaration);
  }

  public static CIdExpression buildNumThreadsIdExpression(int pNumThreads) {
    return SeqExpressionBuilder.buildIdExpression(
        SeqDeclarationBuilder.buildVariableDeclaration(
            false,
            CNumericTypes.CONST_INT,
            SeqToken.NUM_THREADS,
            SeqInitializerBuilder.buildInitializerExpression(
                SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads))));
  }

  // CStringLiteralExpression ======================================================================

  public static CStringLiteralExpression buildStringLiteralExpression(String pValue) {
    return new CStringLiteralExpression(FileLocation.DUMMY, pValue);
  }

  // Helper ========================================================================================

  public static CExpression nestBinaryExpressions(
      ImmutableCollection<CExpression> pAllExpressions,
      BinaryOperator pBinaryOperator,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pAllExpressions.iterator().next();
    for (CExpression next : pAllExpressions) {
      if (!next.equals(rNested)) {
        rNested = pBinaryExpressionBuilder.buildBinaryExpression(rNested, next, pBinaryOperator);
      }
    }
    return rNested;
  }
}
