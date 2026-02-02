// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CFunctionCallStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CFunctionDefinitionStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public final class SeqAssumeFunction extends SeqFunction {

  // CParameterDeclaration

  private static final CParameterDeclaration COND_PARAMETER_ASSUME =
      new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.CONST_INT, "cond");

  // CFunctionType

  private static final CFunctionTypeWithNames ASSUME_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(COND_PARAMETER_ASSUME), false);

  private static final CFunctionType ABORT_FUNCTION_TYPE =
      new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);

  // CFunctionDeclaration

  public static final CFunctionDeclaration ASSUME_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          ASSUME_FUNCTION_TYPE,
          "__MPOR__assume",
          ImmutableList.of(COND_PARAMETER_ASSUME),
          ImmutableSet.of());

  public static final CFunctionDeclaration ABORT_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          ABORT_FUNCTION_TYPE,
          "abort",
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  // CIdExpression

  public static final CIdExpression COND_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, COND_PARAMETER_ASSUME);

  private static final CIdExpression ASSUME_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, ASSUME_FUNCTION_DECLARATION);

  // CFunctionCallExpression

  private static final CFunctionCallExpression ABORT_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          new CIdExpression(FileLocation.DUMMY, ABORT_FUNCTION_DECLARATION),
          ImmutableList.of(),
          ABORT_FUNCTION_DECLARATION);

  private static final CFunctionCallExpression ASSUME_FUNCTION_CALL_EXPRESSION_DUMMY =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          ASSUME_ID_EXPRESSION,
          ImmutableList.of(COND_ID_EXPRESSION),
          ASSUME_FUNCTION_DECLARATION);

  // CFunctionCallStatement

  public static final CFunctionCallStatement ABORT_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, ABORT_FUNCTION_CALL_EXPRESSION);

  private static final CFunctionCallStatement ASSUME_FUNCTION_CALL_STATEMENT_DUMMY =
      new CFunctionCallStatement(FileLocation.DUMMY, ASSUME_FUNCTION_CALL_EXPRESSION_DUMMY);

  public SeqAssumeFunction(CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    super(
        new CFunctionDefinitionStatement(
            ASSUME_FUNCTION_DECLARATION, buildBody(pBinaryExpressionBuilder)));
  }

  private static CCompoundStatement buildBody(CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // build the 'cond == 0' expression
    CBinaryExpression condEqualsZeroExpression =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqAssumeFunction.COND_ID_EXPRESSION,
            SeqIntegerLiteralExpressions.INT_0,
            BinaryOperator.EQUALS);
    CExpressionWrapper ifCondition = new CExpressionWrapper(condEqualsZeroExpression);
    // build the 'if (cond == 0) { abort(); }' statement
    ImmutableList<CExportStatement> ifBlock =
        ImmutableList.of(new CStatementWrapper(ABORT_FUNCTION_CALL_STATEMENT));
    return new CCompoundStatement(new CIfStatement(ifCondition, new CCompoundStatement(ifBlock)));
  }

  /**
   * Returns a {@link CFunctionCallStatement} to the assume function i.e. {@code
   * assume(pCondition);}.
   */
  public static CFunctionCallStatement buildAssumeFunctionCallStatement(CExpression pCondition) {
    CFunctionCallExpression assumeFunctionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            CVoidType.VOID,
            ASSUME_ID_EXPRESSION,
            ImmutableList.of(pCondition),
            ASSUME_FUNCTION_DECLARATION);
    return new CFunctionCallStatement(FileLocation.DUMMY, assumeFunctionCallExpression);
  }

  /**
   * Returns a {@link CFunctionCallStatementWrapper} of an assume function call i.e. {@code
   * assume(pCondition);}.
   */
  public static CFunctionCallStatementWrapper buildAssumeFunctionCallStatement(
      CExportExpression pCondition) {

    ImmutableList<CExportExpression> parameter = ImmutableList.of(pCondition);
    return new CFunctionCallStatementWrapper(ASSUME_FUNCTION_CALL_STATEMENT_DUMMY, parameter);
  }

  /**
   * Returns the function call to assume as a {@link String} that restricts the nondeterministic
   * {@code next_thread} variable to an appropriate value, i.e. {@code assume(0 <= next_thread &&
   * next_thread < NUM_THREADS)} for a signed variable, {@code assume(next_thread < NUM_THREADS)}
   * for an unsigned variable.
   */
  public static CExportStatement buildNextThreadAssumeCallFunctionCallStatement(
      boolean pIsSigned, int pNumThreads, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression numThreadsExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads);
    // ensure that next_thread < NUM_THREADS
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.NEXT_THREAD, numThreadsExpression, BinaryOperator.LESS_THAN);

    // if next_thread is signed, then we also need to ensure that 0 <= next_thread
    if (pIsSigned) {
      CBinaryExpression nextThreadGreaterOrEqualZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIntegerLiteralExpressions.INT_0,
              SeqIdExpressions.NEXT_THREAD,
              BinaryOperator.LESS_EQUAL);
      return buildAssumeFunctionCallStatement(
          CLogicalAndExpression.of(nextThreadLessThanNumThreads, nextThreadGreaterOrEqualZero));
    }
    return new CStatementWrapper(buildAssumeFunctionCallStatement(nextThreadLessThanNumThreads));
  }
}
