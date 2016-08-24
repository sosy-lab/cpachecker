/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withConst;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withVolatile;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

public class CanonicalTypeTest {

  private static final CType VOLATILE_CONST_INT =
      withVolatile(withConst(CNumericTypes.INT)).getCanonicalType();

  @Test
  public void simpleTypeInt() {
    assertThat(CNumericTypes.INT.getCanonicalType()).isEqualTo(CNumericTypes.SIGNED_INT);
    assertThat(CNumericTypes.SIGNED_INT.getCanonicalType()).isEqualTo(CNumericTypes.SIGNED_INT);
    assertThat(CNumericTypes.UNSIGNED_INT.getCanonicalType()).isEqualTo(CNumericTypes.UNSIGNED_INT);

    CType longType = new CSimpleType(false, false, CBasicType.UNSPECIFIED, true, false, false, false, false, false, false);
    CType signedLongIntType = new CSimpleType(false, false, CBasicType.INT, true, false, true, false, false, false, false);
    assertThat(longType.getCanonicalType()).isEqualTo(signedLongIntType);
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
    CFunctionType function = new CFunctionType(false, false, typedef, ImmutableList.<CType>of(typedef), false);

    CFunctionType expected = new CFunctionType(false, false, CNumericTypes.SIGNED_INT, ImmutableList.<CType>of(CNumericTypes.SIGNED_INT), false);
    assertThat(function.getCanonicalType()).isEqualTo(expected);
  }
}
