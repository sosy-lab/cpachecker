// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;

public final class SeqReachErrorFunction extends SeqFunction {

  public static final String NAME = "reach_error";

  // CParameterDeclarations

  private static final CParameterDeclaration FUNCTION_PARAMETER_REACH_ERROR =
      new CParameterDeclaration(
          FileLocation.DUMMY, CPointerType.POINTER_TO_CONST_CHAR, "__function");

  private static final CParameterDeclaration FILE_PARAMETER_REACH_ERROR =
      new CParameterDeclaration(FileLocation.DUMMY, CPointerType.POINTER_TO_CONST_CHAR, "__file");

  private static final CParameterDeclaration LINE_PARAMETER_REACH_ERROR =
      new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, "__line");

  private static final ImmutableList<CParameterDeclaration> REACH_ERROR_PARAMETERS =
      ImmutableList.of(
          FILE_PARAMETER_REACH_ERROR, LINE_PARAMETER_REACH_ERROR, FUNCTION_PARAMETER_REACH_ERROR);

  private static final CParameterDeclaration ASSERTION_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY, CPointerType.POINTER_TO_CONST_CHAR, "__assertion");

  private static final ImmutableList<CParameterDeclaration> ASSERT_FAIL_PARAMETERS =
      ImmutableList.<CParameterDeclaration>builder()
          .add(ASSERTION_PARAMETER_ASSERT_FAIL)
          .addAll(REACH_ERROR_PARAMETERS)
          .build();

  // CFunctionTypes

  private static final CFunctionTypeWithNames REACH_ERROR_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, REACH_ERROR_PARAMETERS, false);

  private static final CFunctionTypeWithNames ASSERT_FAIL_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ASSERT_FAIL_PARAMETERS, false);

  // CFunctionDeclaration

  public static final CFunctionDeclaration REACH_ERROR_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          REACH_ERROR_FUNCTION_TYPE,
          NAME,
          REACH_ERROR_PARAMETERS,
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration ASSERT_FAIL_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          ASSERT_FAIL_FUNCTION_TYPE,
          "__assert_fail",
          ASSERT_FAIL_PARAMETERS,
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  // CIdExpression

  private static final CIdExpression REACH_ERROR_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, REACH_ERROR_FUNCTION_DECLARATION);

  // CFunctionCallExpression

  private static final CFunctionCallExpression ASSERT_FAIL_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          new CIdExpression(FileLocation.DUMMY, ASSERT_FAIL_FUNCTION_DECLARATION),
          ImmutableList.of(
              new CStringLiteralExpression(FileLocation.DUMMY, "\"0\""),
              // reuse parameter declarations from reach_error since they are passed on
              new CIdExpression(FileLocation.DUMMY, FILE_PARAMETER_REACH_ERROR),
              new CIdExpression(FileLocation.DUMMY, LINE_PARAMETER_REACH_ERROR),
              new CIdExpression(FileLocation.DUMMY, LINE_PARAMETER_REACH_ERROR)),
          ASSERT_FAIL_FUNCTION_DECLARATION);

  // CFunctionCallStatement

  private static final CFunctionCallStatement ASSERT_FAIL_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, ASSERT_FAIL_FUNCTION_CALL_EXPRESSION);

  public SeqReachErrorFunction() {
    super(REACH_ERROR_FUNCTION_DECLARATION, ASSERT_FAIL_FUNCTION_CALL_STATEMENT.toASTString());
  }

  /**
   * Returns the {@link CFunctionCallStatement} of {@code reach_error("{pFile}", {pLine},
   * "{pFunction}")}
   */
  public static CFunctionCallStatement buildReachErrorFunctionCallStatement(
      String pFile, int pLine, String pFunction) {
    CStringLiteralExpression file =
        new CStringLiteralExpression(FileLocation.DUMMY, SeqStringUtil.wrapInQuotationMarks(pFile));
    CIntegerLiteralExpression line = SeqExpressionBuilder.buildIntegerLiteralExpression(pLine);
    CStringLiteralExpression function =
        new CStringLiteralExpression(
            FileLocation.DUMMY, SeqStringUtil.wrapInQuotationMarks(pFunction));

    CFunctionCallExpression functionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            CVoidType.VOID,
            REACH_ERROR_ID_EXPRESSION,
            ImmutableList.of(file, line, function),
            REACH_ERROR_FUNCTION_DECLARATION);
    return new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
  }
}
