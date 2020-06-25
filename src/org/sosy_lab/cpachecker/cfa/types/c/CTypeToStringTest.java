/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
    value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    justification = "Fields are filled by parameterization of JUnit"
  )
public class CTypeToStringTest {

  private static final String VAR = "var";

  private static final CType CONST_VOLATILE_INT =
      new CSimpleType(true, true, CBasicType.INT, false, false, false, false, false, false, false);

  @Parameters(name = "{0} [{1}]")
  public static Object[][] types() {
    return new Object[][] {
      { // declare var as pointer to int
        "int *var", new CPointerType(false, false, CNumericTypes.INT),
      },
      { // declare var as const volatile pointer to int
        "int * const volatile var", new CPointerType(true, true, CNumericTypes.INT),
      },
      { // declare var as pointer to const volatile int
        "const volatile int *var", new CPointerType(false, false, CONST_VOLATILE_INT),
      },
      { // declare var as const volatile pointer to const volatile int
        "const volatile int * const volatile var", new CPointerType(true, true, CONST_VOLATILE_INT),
      },
      { // declare var as array 1 of int
        "int var[1]",
        new CArrayType(false, false, CNumericTypes.INT, CIntegerLiteralExpression.ONE),
      },
      { // not possible to specify directly, but with typedefs
        "const volatile int var[1]",
        new CArrayType(true, true, CNumericTypes.INT, CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile int
        "const volatile int var[1]",
        new CArrayType(false, false, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE),
      },
      { // not possible to specify directly, but with typedefs
        "const volatile const volatile int var[1]",
        new CArrayType(true, true, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of pointer to int
        "int *var[1]",
        new CArrayType(
            false,
            false,
            new CPointerType(false, false, CNumericTypes.INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile pointer to int
        "int * const volatile var[1]",
        new CArrayType(
            false,
            false,
            new CPointerType(true, true, CNumericTypes.INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of pointer to const volatile int
        "const volatile int *var[1]",
        new CArrayType(
            false,
            false,
            new CPointerType(false, false, CONST_VOLATILE_INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile pointer to const volatile int
        "const volatile int * const volatile var[1]",
        new CArrayType(
            false,
            false,
            new CPointerType(true, true, CONST_VOLATILE_INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as pointer to array 1 of int
        "int (*var)[1]",
        new CPointerType(
            false,
            false,
            new CArrayType(false, false, CNumericTypes.INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as const volatile pointer to array 1 of int
        "int (* const volatile var)[1]",
        new CPointerType(
            true,
            true,
            new CArrayType(false, false, CNumericTypes.INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as pointer to array 1 of const volatile int
        "const volatile int (*var)[1]",
        new CPointerType(
            false,
            false,
            new CArrayType(false, false, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as const volatile pointer to array 1 of const volatile int
        "const volatile int (* const volatile var)[1]",
        new CPointerType(
            true,
            true,
            new CArrayType(false, false, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as function (int) returning pointer to double
        "double *var(int)",
        new CFunctionType(
            new CPointerType(false, false, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning const volatile pointer to double
        "double * const volatile var(int)",
        new CFunctionType(
            new CPointerType(true, true, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to double
        "double *var(int)",
        new CFunctionType(
            new CPointerType(false, false, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to function (double) returning void
        "void (*var(int))(double)",
        new CFunctionType(
            new CPointerType(
                false,
                false,
                new CFunctionType(CVoidType.VOID, ImmutableList.of(CNumericTypes.DOUBLE), false)),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning const volatile pointer to function (double) returning void
        "void (* const volatile var(int))(double)",
        new CFunctionType(
            new CPointerType(
                true,
                true,
                new CFunctionType(CVoidType.VOID, ImmutableList.of(CNumericTypes.DOUBLE), false)),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to function (double) returning pointer to char
        "char *(*var(int))(double)",
        new CFunctionType(
            new CPointerType(
                false,
                false,
                new CFunctionType(
                    new CPointerType(false, false, CNumericTypes.CHAR),
                    ImmutableList.of(CNumericTypes.DOUBLE),
                    false)),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
    };
  }

  @Parameter(0)
  public String stringRepr;

  @Parameter(1)
  public CType type;

  private static CParser parser;

  @BeforeClass
  public static void setupParser() {
    parser =
        Parsers.getCParser(
            LogManager.createTestLogManager(),
            new EclipseCParserOptions(),
            MachineModel.LINUX32,
            ShutdownNotifier.createDummy());
  }

  @Test
  public void testToString() {
    assertThat(type.toASTString(VAR)).isEqualTo(stringRepr);
  }

  @Test
  public void testParse() throws CParserException, InterruptedException {
    CType parsed =
        (CType)
            parser
                .parseString("dummy", stringRepr + ";")
                .getGlobalDeclarations()
                .get(0)
                .getFirst()
                .getType();
    assertThat(parsed.getCanonicalType()).isEqualTo(type.getCanonicalType());
  }
}
