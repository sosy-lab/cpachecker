// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withConst;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withVolatile;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class CanonicalTypeTest {

  private static final CType VOLATILE_CONST_INT =
      withVolatile(withConst(CNumericTypes.INT)).getCanonicalType();

  @Test
  public void simpleTypeChar() {
    assertThat(CNumericTypes.CHAR.getCanonicalType()).isNotEqualTo(CNumericTypes.SIGNED_CHAR);
    assertThat(CNumericTypes.CHAR.getCanonicalType()).isNotEqualTo(CNumericTypes.UNSIGNED_CHAR);
    assertThat(CNumericTypes.SIGNED_CHAR.getCanonicalType())
        .isNotEqualTo(CNumericTypes.UNSIGNED_CHAR);
    assertThat(CNumericTypes.UNSIGNED_CHAR.getCanonicalType()).isNotEqualTo(CNumericTypes.CHAR);
  }

  @Test
  public void simpleTypeInt() {
    assertThat(CNumericTypes.INT.getCanonicalType()).isEqualTo(CNumericTypes.SIGNED_INT);
    assertThat(CNumericTypes.SIGNED_INT.getCanonicalType()).isEqualTo(CNumericTypes.SIGNED_INT);
    assertThat(CNumericTypes.UNSIGNED_INT.getCanonicalType()).isEqualTo(CNumericTypes.UNSIGNED_INT);

    CType longType =
        new CSimpleType(
            false, false, CBasicType.UNSPECIFIED, true, false, false, false, false, false, false);
    assertThat(longType.getCanonicalType()).isEqualTo(CNumericTypes.SIGNED_LONG_INT);
  }

  @Test
  public void typedefQualifiers() {
    CTypedefType typedef = new CTypedefType(true, true, "TYPEDEF", CNumericTypes.INT);

    // typedefs push their qualifiers to the target type (C11 § 6.7.3 (5))
    assertThat(typedef.getCanonicalType()).isEqualTo(VOLATILE_CONST_INT);
  }

  @Test
  public void arrayQualifiers() {
    CArrayType array = new CArrayType(true, true, CNumericTypes.INT, null);

    // arrays push their qualifiers to the element type (C11 § 6.7.3 (9))
    CArrayType expected = new CArrayType(false, false, VOLATILE_CONST_INT, null);
    assertThat(array.getCanonicalType()).isEqualTo(expected);
  }

  @Test
  public void arrayTypedefQualifiers() {
    CTypedefType typedef = new CTypedefType(true, false, "TYPEDEF", CNumericTypes.INT);
    CArrayType array = new CArrayType(false, true, typedef, null);

    CArrayType expected = new CArrayType(false, false, VOLATILE_CONST_INT, null);
    assertThat(array.getCanonicalType()).isEqualTo(expected);
  }

  @Test
  public void typedefArrayQualifiers() {
    CArrayType array = new CArrayType(false, true, CNumericTypes.INT, null);
    CTypedefType typedef = new CTypedefType(true, false, "TYPEDEF", array);

    CArrayType expected = new CArrayType(false, false, VOLATILE_CONST_INT, null);
    assertThat(typedef.getCanonicalType()).isEqualTo(expected);
  }

  @Test
  public void functionType() {
    CTypedefType typedef = new CTypedefType(false, false, "TYPEDEF", CNumericTypes.INT);
    CFunctionType function = new CFunctionType(typedef, ImmutableList.of(typedef), false);

    CFunctionType expected =
        new CFunctionType(
            CNumericTypes.SIGNED_INT, ImmutableList.of(CNumericTypes.SIGNED_INT), false);
    assertThat(function.getCanonicalType()).isEqualTo(expected);
  }
}
