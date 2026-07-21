// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
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
    CType parsed =
        (CType)
            parser
                .parseString(Path.of("dummy"), stringRepr + ";")
                .globalDeclarations()
                .getFirst()
                .getFirst()
                .getType();
    assertThat(parsed.getCanonicalType()).isEqualTo(type.getCanonicalType());
  }

  @Test
  public void testAtomicTypeSpecifierParses() throws CParserException, InterruptedException {
    // C23 § 6.7.3.5 (issue #1667): the atomic type specifier "_Atomic(T)" denotes the same type as
    // the "_Atomic T" qualifier (footnote 147). Its string representation is the qualifier form, so
    // this cannot be checked with the round-trip test above.
    assertThat(parseGlobalType("_Atomic(int) v;").getCanonicalType())
        .isEqualTo(CNumericTypes.INT.withAtomic().getCanonicalType());
    assertThat(parseGlobalType("_Atomic (unsigned long) v;").getCanonicalType())
        .isEqualTo(CNumericTypes.UNSIGNED_LONG_INT.withAtomic().getCanonicalType());

    // The parentheses are significant: "_Atomic(int) *" is a pointer to an _Atomic int, whereas
    // "_Atomic(int*)" is an _Atomic pointer to a (non-atomic) int.
    assertThat(parseGlobalType("_Atomic(int) *v;").getCanonicalType())
        .isEqualTo(
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT.withAtomic())
                .getCanonicalType());
    assertThat(parseGlobalType("_Atomic(int*) v;").getCanonicalType())
        .isEqualTo(new CPointerType(CTypeQualifiers.ATOMIC, CNumericTypes.INT).getCanonicalType());
    assertThat(parseGlobalType("_Atomic(int**) v;").getCanonicalType())
        .isEqualTo(
            new CPointerType(
                    CTypeQualifiers.ATOMIC,
                    new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT))
                .getCanonicalType());
  }

  @Test
  public void testAtomicTypeSpecifierWithArrayAndAggregateTypes()
      throws CParserException, InterruptedException {
    // The atomic type specifier gives the element type, while the array is a separate declarator,
    // so
    // "_Atomic(int) x[3]" is an array of _Atomic int (an array *as the type name* is forbidden, see
    // testAtomicTypeSpecifierRejectsUnsupportedTypeNames).
    CType arrayOfAtomicInt = parseLastGlobalType("_Atomic(int) x[3];").getCanonicalType();
    assertThat(arrayOfAtomicInt).isInstanceOf(CArrayType.class);
    assertThat(((CArrayType) arrayOfAtomicInt).getType().getCanonicalType())
        .isEqualTo(CNumericTypes.INT.withAtomic().getCanonicalType());

    CType arrayOfAtomicPointers = parseLastGlobalType("_Atomic(int*) x[3];").getCanonicalType();
    assertThat(arrayOfAtomicPointers).isInstanceOf(CArrayType.class);
    assertThat(((CArrayType) arrayOfAtomicPointers).getType().getCanonicalType())
        .isEqualTo(new CPointerType(CTypeQualifiers.ATOMIC, CNumericTypes.INT).getCanonicalType());

    // struct/union/enum/typedef type names are plain type names and are handled as well.
    assertThat(parseLastGlobalType("struct s { int a; }; _Atomic(struct s) v;").isAtomic())
        .isTrue();
    assertThat(parseLastGlobalType("union u { int a; }; _Atomic(union u) v;").isAtomic()).isTrue();
    assertThat(parseLastGlobalType("enum e { A }; _Atomic(enum e) v;").isAtomic()).isTrue();
    assertThat(parseLastGlobalType("typedef int t; _Atomic(t) v;").getCanonicalType())
        .isEqualTo(CNumericTypes.INT.withAtomic().getCanonicalType());
  }

  @Test
  public void testAtomicTypeSpecifierWithFunctionPointerTypes()
      throws CParserException, InterruptedException {
    // A parenthesized-pointer type name (function pointer or pointer to array) is a valid pointer
    // type; "_Atomic(int (*)(void)) fp" is an atomic pointer to a function.
    CType atomicFunctionPointer =
        parseLastGlobalType("_Atomic(int (*)(void)) fp;").getCanonicalType();
    assertThat(atomicFunctionPointer.isAtomic()).isTrue();
    assertThat(atomicFunctionPointer).isInstanceOf(CPointerType.class);
    assertThat(((CPointerType) atomicFunctionPointer).getType().getCanonicalType())
        .isInstanceOf(CFunctionType.class);

    // "_Atomic(int (*)[3]) p" is an atomic pointer to an array.
    CType atomicArrayPointer = parseLastGlobalType("_Atomic(int (*)[3]) p;").getCanonicalType();
    assertThat(atomicArrayPointer.isAtomic()).isTrue();
    assertThat(atomicArrayPointer).isInstanceOf(CPointerType.class);
    assertThat(((CPointerType) atomicArrayPointer).getType().getCanonicalType())
        .isInstanceOf(CArrayType.class);

    // The shared type name applies to every declarator, so both are atomic function pointers.
    assertThat(parseLastGlobalType("_Atomic(int (*)(void)) fp, gp;").isAtomic()).isTrue();
  }

  @Test
  public void testAtomicTypeSpecifierRejectsForbiddenTypeNames() {
    // C23 § 6.7.3.5 (3): the type name in an atomic type specifier shall not be an array or a
    // function type. These are constraint violations and are rejected (as they are by GCC).
    assertThrows(CParserException.class, () -> parseLastGlobalType("_Atomic(int[3]) v;"));
    assertThrows(CParserException.class, () -> parseLastGlobalType("_Atomic(int(void)) f;"));
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
