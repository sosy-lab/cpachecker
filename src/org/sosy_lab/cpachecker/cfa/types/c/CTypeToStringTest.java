// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.CONST_VOLATILE_INT;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
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
    justification = "Fields are filled by parameterization of JUnit")
public class CTypeToStringTest {

  private static final String VAR = "var";

  @Parameters(name = "{0} [{1}]")
  @SuppressWarnings("checkstyle:NoWhitespaceAfter") // nicely readable in this special case
  public static Object[][] types() {
    return new Object[][] {
      {
        "int var", CNumericTypes.INT,
      },
      {
        "_Atomic int var", CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.ATOMIC),
      },
      {
        "const int var", CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.CONST),
      },
      {
        "volatile int var", CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.VOLATILE),
      },
      {
        "_Atomic const int var",
        CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.ATOMIC_CONST),
      },
      {
        "_Atomic volatile int var",
        CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.ATOMIC_VOLATILE),
      },
      {
        "const volatile int var",
        CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.CONST_VOLATILE),
      },
      {
        "_Atomic const volatile int var",
        CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.ATOMIC_CONST_VOLATILE),
      },
      {
        "_Atomic int *var", new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT.withAtomic()),
      },
      { // declare var as pointer to int
        "int *var", new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT),
      },
      { // declare var as const volatile pointer to int
        "int *const volatile var",
        new CPointerType(CTypeQualifiers.CONST_VOLATILE, CNumericTypes.INT),
      },
      { // declare var as pointer to const volatile int
        "const volatile int *var", new CPointerType(CTypeQualifiers.NONE, CONST_VOLATILE_INT),
      },
      { // declare var as const volatile pointer to const volatile int
        "const volatile int *const volatile var",
        new CPointerType(CTypeQualifiers.CONST_VOLATILE, CONST_VOLATILE_INT),
      },
      { // declare var as _Atomic pointer to int (issue #1670)
        "int *_Atomic var", new CPointerType(CTypeQualifiers.ATOMIC, CNumericTypes.INT),
      },
      { // declare var as _Atomic pointer to pointer to int
        "int **_Atomic var",
        new CPointerType(
            CTypeQualifiers.ATOMIC, new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT)),
      },
      { // declare var as pointer to _Atomic pointer to int
        "int *_Atomic *var",
        new CPointerType(
            CTypeQualifiers.NONE, new CPointerType(CTypeQualifiers.ATOMIC, CNumericTypes.INT)),
      },
      { // declare var as array 1 of int
        "int var[1]",
        new CArrayType(CTypeQualifiers.NONE, CNumericTypes.INT, CIntegerLiteralExpression.ONE),
      },
      { // not possible to specify directly, but with typedefs
        "const volatile int var[1]",
        new CArrayType(
            CTypeQualifiers.CONST_VOLATILE, CNumericTypes.INT, CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile int
        "const volatile int var[1]",
        new CArrayType(CTypeQualifiers.NONE, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE),
      },
      { // not possible to specify directly, but with typedefs
        "const volatile const volatile int var[1]",
        new CArrayType(
            CTypeQualifiers.CONST_VOLATILE, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of pointer to int
        "int *var[1]",
        new CArrayType(
            CTypeQualifiers.NONE,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile pointer to int
        "int *const volatile var[1]",
        new CArrayType(
            CTypeQualifiers.NONE,
            new CPointerType(CTypeQualifiers.CONST_VOLATILE, CNumericTypes.INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of pointer to const volatile int
        "const volatile int *var[1]",
        new CArrayType(
            CTypeQualifiers.NONE,
            new CPointerType(CTypeQualifiers.NONE, CONST_VOLATILE_INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as array 1 of const volatile pointer to const volatile int
        "const volatile int *const volatile var[1]",
        new CArrayType(
            CTypeQualifiers.NONE,
            new CPointerType(CTypeQualifiers.CONST_VOLATILE, CONST_VOLATILE_INT),
            CIntegerLiteralExpression.ONE),
      },
      { // declare var as pointer to array 1 of int
        "int (*var)[1]",
        new CPointerType(
            CTypeQualifiers.NONE,
            new CArrayType(CTypeQualifiers.NONE, CNumericTypes.INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as const volatile pointer to array 1 of int
        "int (*const volatile var)[1]",
        new CPointerType(
            CTypeQualifiers.CONST_VOLATILE,
            new CArrayType(CTypeQualifiers.NONE, CNumericTypes.INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as pointer to array 1 of const volatile int
        "const volatile int (*var)[1]",
        new CPointerType(
            CTypeQualifiers.NONE,
            new CArrayType(
                CTypeQualifiers.NONE, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as const volatile pointer to array 1 of const volatile int
        "const volatile int (*const volatile var)[1]",
        new CPointerType(
            CTypeQualifiers.CONST_VOLATILE,
            new CArrayType(
                CTypeQualifiers.NONE, CONST_VOLATILE_INT, CIntegerLiteralExpression.ONE)),
      },
      { // declare var as function (int) returning pointer to double
        "double *var(int)",
        new CFunctionType(
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning const volatile pointer to double
        "double *const volatile var(int)",
        new CFunctionType(
            new CPointerType(CTypeQualifiers.CONST_VOLATILE, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to double
        "double *var(int)",
        new CFunctionType(
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.DOUBLE),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to function (double) returning void
        "void (*var(int))(double)",
        new CFunctionType(
            new CPointerType(
                CTypeQualifiers.NONE,
                new CFunctionType(CVoidType.VOID, ImmutableList.of(CNumericTypes.DOUBLE), false)),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning const volatile pointer to function (double)
        // returning void
        "void (*const volatile var(int))(double)",
        new CFunctionType(
            new CPointerType(
                CTypeQualifiers.CONST_VOLATILE,
                new CFunctionType(CVoidType.VOID, ImmutableList.of(CNumericTypes.DOUBLE), false)),
            ImmutableList.of(CNumericTypes.INT),
            false),
      },
      { // declare var as function (int) returning pointer to function (double) returning pointer to
        // char
        "char *(*var(int))(double)",
        new CFunctionType(
            new CPointerType(
                CTypeQualifiers.NONE,
                new CFunctionType(
                    new CPointerType(CTypeQualifiers.NONE, CNumericTypes.CHAR),
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
    assertThat(parseGlobalType(stringRepr + ";").getCanonicalType())
        .isEqualTo(type.getCanonicalType());
  }

  @Test
  public void testAtomicToTheRightOfPointer() throws CParserException, InterruptedException {
    // Issue #1670: "_Atomic" right of a "*" qualifies the pointer, not the pointed-to object.
    // The round-trip test above covers the basic cases; here we test the ones that cannot be
    // expressed as a single type.
    CType atomicPointerToInt =
        new CPointerType(CTypeQualifiers.ATOMIC, CNumericTypes.INT).getCanonicalType();
    CType pointerToInt =
        new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT).getCanonicalType();

    // In contrast to the above, here the pointed-to object is atomic and the pointer is not.
    assertThat(parseGlobalType("_Atomic int *v;").getCanonicalType())
        .isEqualTo(
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT.withAtomic())
                .getCanonicalType());

    // Combination with the other qualifiers.
    assertThat(parseGlobalType("int *const _Atomic v;").getCanonicalType())
        .isEqualTo(
            new CPointerType(CTypeQualifiers.ATOMIC_CONST, CNumericTypes.INT).getCanonicalType());
    assertThat(parseGlobalType("int *volatile _Atomic v;").getCanonicalType())
        .isEqualTo(
            new CPointerType(CTypeQualifiers.ATOMIC_VOLATILE, CNumericTypes.INT)
                .getCanonicalType());

    // Each declarator of a declaration has its own qualifiers.
    assertThat(parseGlobalType("int *_Atomic v, *w;").getCanonicalType())
        .isEqualTo(atomicPointerToInt);
    assertThat(parseLastGlobalType("int *_Atomic v, *w;").getCanonicalType())
        .isEqualTo(pointerToInt);
    assertThat(parseGlobalType("int *v, *_Atomic w;").getCanonicalType()).isEqualTo(pointerToInt);
    assertThat(parseLastGlobalType("int *v, *_Atomic w;").getCanonicalType())
        .isEqualTo(atomicPointerToInt);

    // In a typedef.
    assertThat(parseLastGlobalType("typedef int *_Atomic t; t v;").getCanonicalType())
        .isEqualTo(atomicPointerToInt);

    // As element type of an array.
    CType arrayType = parseGlobalType("int *_Atomic v[2];").getCanonicalType();
    assertThat(arrayType).isInstanceOf(CArrayType.class);
    assertThat(((CArrayType) arrayType).getType().getCanonicalType())
        .isEqualTo(atomicPointerToInt);

    // As member of a struct.
    CType structType =
        parseLastGlobalType("struct s { int *_Atomic m; }; struct s v;").getCanonicalType();
    assertThat(structType).isInstanceOf(CCompositeType.class);
    assertThat(((CCompositeType) structType).getMembers().getFirst().getType().getCanonicalType())
        .isEqualTo(atomicPointerToInt);

    // As parameter of a function.
    CType functionType = parseGlobalType("void f(int *_Atomic p);").getCanonicalType();
    assertThat(functionType).isInstanceOf(CFunctionType.class);
    assertThat(((CFunctionType) functionType).getParameters().getFirst().getCanonicalType())
        .isEqualTo(atomicPointerToInt);
  }

  private static CType parseGlobalType(String pDeclaration)
      throws CParserException, InterruptedException {
    return (CType)
        parser
            .parseString(Path.of("dummy"), pDeclaration)
            .globalDeclarations()
            .getFirst()
            .getFirst()
            .getType();
  }

  private static CType parseLastGlobalType(String pCode)
      throws CParserException, InterruptedException {
    return (CType)
        parser
            .parseString(Path.of("dummy"), pCode)
            .globalDeclarations()
            .getLast()
            .getFirst()
            .getType();
  }
}
