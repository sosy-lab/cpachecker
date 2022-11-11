// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

@RunWith(Parameterized.class)
public class CTypesTest {

  private static final MachineModel TEST_MACHINE_MODEL = MachineModel.LINUX32;
  public static final CIntegerLiteralExpression TWO =
      CIntegerLiteralExpression.createDummyLiteral(2L, CNumericTypes.INT);

  private static final CIdExpression VAR_N =
      new CIdExpression(
          FileLocation.DUMMY,
          CNumericTypes.INT,
          "n",
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              CNumericTypes.INT,
              "n",
              "n",
              "n",
              null));
  private static final CIdExpression VAR_CONST =
      new CIdExpression(
          FileLocation.DUMMY,
          CTypes.withConst(CNumericTypes.INT),
          "c",
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              CTypes.withConst(CNumericTypes.INT),
              "c",
              "c",
              "c",
              new CInitializerExpression(FileLocation.DUMMY, TWO)));

  private static final CArrayType CONSTANT_ARRAY =
      new CArrayType(false, false, CNumericTypes.INT, TWO);
  private static final CArrayType VARIABLE_ARRAY =
      new CArrayType(false, false, CNumericTypes.INT, VAR_N);
  private static final CArrayType UNKNOWN_ARRAY =
      new CArrayType(false, false, CNumericTypes.INT, null);

  @Parameters(name = "{0}")
  public static List<Object[]> parameters() {

    CCompositeType simpleStruct =
        new CCompositeType(false, false, ComplexTypeKind.STRUCT, "simpleStruct", "simpleStruct");
    simpleStruct.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "i")));

    CCompositeType arrayStruct =
        new CCompositeType(false, false, ComplexTypeKind.STRUCT, "arrayStruct", "arrayStruct");
    arrayStruct.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(CONSTANT_ARRAY, "a")));

    CCompositeType flexibleArrayStruct =
        new CCompositeType(
            false, false, ComplexTypeKind.STRUCT, "flexibleArrayStruct", "flexibleArrayStruct");
    flexibleArrayStruct.setMembers(
        ImmutableList.of(
            new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "i"),
            new CCompositeTypeMemberDeclaration(UNKNOWN_ARRAY, "a")));

    CCompositeType variableArrayStruct =
        new CCompositeType(
            false, false, ComplexTypeKind.STRUCT, "variableArrayStruct", "variableArrayStruct");
    variableArrayStruct.setMembers(
        ImmutableList.of(
            new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "i"),
            new CCompositeTypeMemberDeclaration(VARIABLE_ARRAY, "a")));

    return ImmutableList.<Object[]>of(
        new Object[] {CONSTANT_ARRAY, true, true, 8},
        new Object[] {UNKNOWN_ARRAY, false, false, -1},
        new Object[] {VARIABLE_ARRAY, true, false, -1},
        new Object[] {new CArrayType(false, false, CNumericTypes.INT, VAR_CONST), true, false, -1},
        new Object[] {new CArrayType(false, false, CONSTANT_ARRAY, TWO), true, true, 2 * 8},
        new Object[] {new CArrayType(false, false, CONSTANT_ARRAY, null), false, false, -1},
        new Object[] {new CArrayType(false, false, CONSTANT_ARRAY, VAR_N), true, false, -1},
        new Object[] {new CArrayType(false, false, VARIABLE_ARRAY, TWO), true, false, -1},
        new Object[] {
          new CElaboratedType(false, false, ComplexTypeKind.ENUM, "e", "e", null), true, true, 4
        },
        new Object[] {
          new CElaboratedType(false, false, ComplexTypeKind.STRUCT, "s", "s", null),
          false,
          false,
          -1
        },
        new Object[] {simpleStruct, true, true, 4},
        new Object[] {arrayStruct, true, true, 8},
        new Object[] {flexibleArrayStruct, true, true, 4},
        new Object[] {variableArrayStruct, true, false, -1},
        new Object[] {CNumericTypes.CHAR, true, true, 1},
        new Object[] {CVoidType.VOID, false, true, 1} // GCC extension
        );
  }

  @Parameter(0)
  public CType type;

  @Parameter(1)
  public boolean isComplete;

  @Parameter(2)
  public boolean hasKnownConstantSize;

  @Parameter(3)
  public int size;

  @Test
  public void testIsIncomplete() {
    assertWithMessage("Result of isIncomplete() is wrong")
        .that(type.isIncomplete())
        .isEqualTo(!isComplete);
  }

  @Test
  public void testHasKnownConstantSize() {
    assertWithMessage("Result of hasKnownConstantSize() is wrong")
        .that(type.hasKnownConstantSize())
        .isEqualTo(hasKnownConstantSize);
  }

  @Test
  public void testConstantSizeof() {
    assume().that(hasKnownConstantSize).isTrue();
    assertThat(TEST_MACHINE_MODEL.getSizeof(type)).isEqualTo(BigInteger.valueOf(size));
  }

  @Test
  @Ignore // FIXME, cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1031
  public void testSizeofShouldThrowIfNotConstant() {
    assume().that(hasKnownConstantSize).isFalse();
    assertThrows(IllegalArgumentException.class, () -> TEST_MACHINE_MODEL.getSizeof(type));
  }
}
