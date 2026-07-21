// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CTypeQualifiersTest {

  @Test
  public void testCreate() {
    for (CTypeQualifiers qualifiers : CTypeQualifiers.values()) {
      CTypeQualifiers recreated =
          CTypeQualifiers.create(
              qualifiers.containsAtomic(),
              qualifiers.containsConst(),
              qualifiers.containsVolatile());
      assertThat(recreated).isEqualTo(qualifiers);
    }
  }

  @Test
  public void testWithoutQualifiersKeepsAtomic() {
    // C11 § 6.2.5 (27) / C23 § 6.2.5 (32): _Atomic is not a cvr-qualifier, so the unqualified
    // version of a type keeps _Atomic and drops only const and volatile.
    CType atomicConstVolatileInt =
        CNumericTypes.INT.withQualifiersSetTo(CTypeQualifiers.ATOMIC_CONST_VOLATILE);

    CType unqualified = atomicConstVolatileInt.withoutQualifiers();
    assertThat(unqualified.isAtomic()).isTrue();
    assertThat(unqualified.isConst()).isFalse();
    assertThat(unqualified.isVolatile()).isFalse();
    assertThat(unqualified).isEqualTo(CNumericTypes.INT.withAtomic());

    assertThat(CNumericTypes.INT.withConst().withVolatile().withoutQualifiers())
        .isEqualTo(CNumericTypes.INT);

    // withoutAtomic() still removes _Atomic, so the fully plain type is reachable.
    assertThat(unqualified.withoutAtomic()).isEqualTo(CNumericTypes.INT);
  }
}
