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
}
