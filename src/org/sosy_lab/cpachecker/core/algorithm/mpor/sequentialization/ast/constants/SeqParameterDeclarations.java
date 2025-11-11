// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqParameterDeclarations {

  public static final CParameterDeclaration ASSERTION_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          CPointerType.POINTER_TO_CONST_CHAR,
          SeqToken.ASSERTION_KEYWORD_ASSERT_FAIL);

  public static final CParameterDeclaration COND_PARAMETER_ASSUME =
      new CParameterDeclaration(
          FileLocation.DUMMY, CNumericTypes.CONST_INT, SeqToken.COND_KEYWORD_ASSUME);

  public static final CParameterDeclaration FUNCTION_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          CPointerType.POINTER_TO_CONST_CHAR,
          SeqToken.FUNCTION_KEYWORD_ASSERT_FAIL);

  public static final CParameterDeclaration FILE_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          CPointerType.POINTER_TO_CONST_CHAR,
          SeqToken.FILE_KEYWORD_ASSERT_FAIL);

  public static final CParameterDeclaration LINE_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, SeqToken.LINE_KEYWORD_ASSERT_FAIL);

  public static final CParameterDeclaration SIZE_PARAMETER_ASSERT_FAIL =
      new CParameterDeclaration(
          FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, SeqToken.SIZE_KEYWORD_MALLOC);
}
