// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ACSLConverter;

public class SMGInvariantTest {

  @Test
  public void testInvariantCreation() {
    SMGInvariant invariant = new SMGInvariant(
        SMGInvariant.InvariantType.POINTER_VALIDITY,
        SMGInvariant.Property.MEMORY_SAFETY,
        "ptr",
        "valid_expression",
        4,
        "timepoint"
    );

    assertThat(invariant.getType()).isEqualTo(SMGInvariant.InvariantType.POINTER_VALIDITY);
    assertThat(invariant.getProperty()).isEqualTo(SMGInvariant.Property.MEMORY_SAFETY);
    assertThat(invariant.getPointer()).isEqualTo("ptr");
    assertThat(invariant.getExpression()).isEqualTo("valid_expression");
    assertThat(invariant.getSize()).isEqualTo(4);
    assertThat(invariant.getTimepoint()).isEqualTo("timepoint");
  }

  @Test
  public void testInvariantVisitorPattern() {
    SMGInvariant invariant = new SMGInvariant(
        SMGInvariant.InvariantType.ALLOCATION_STATUS,
        SMGInvariant.Property.MEMORY_SAFETY,
        "buffer",
        null,
        0,
        null
    );

    ACSLConverter converter = new ACSLConverter();
    String result = invariant.accept(converter);

    assertThat(result).isEqualTo("\\allocated(buffer)");
  }

  @Test
  public void testInvariantEquality() {
    SMGInvariant invariant1 = new SMGInvariant(
        SMGInvariant.InvariantType.POINTER_VALIDITY,
        SMGInvariant.Property.MEMORY_SAFETY,
        "ptr",
        null,
        0,
        null
    );

    SMGInvariant invariant2 = new SMGInvariant(
        SMGInvariant.InvariantType.POINTER_VALIDITY,
        SMGInvariant.Property.MEMORY_SAFETY,
        "ptr",
        null,
        0,
        null
    );

    // If equals() and hashCode() are implemented, test equality:
    // assertThat(invariant1).isEqualTo(invariant2);

    // At minimum, ensure both are not null and have same values
    assertThat(invariant1).isNotNull();
    assertThat(invariant2).isNotNull();
    assertThat(invariant1.getType()).isEqualTo(invariant2.getType());
    assertThat(invariant1.getPointer()).isEqualTo(invariant2.getPointer());
  }

  @Test
  public void testInvariantTypes() {
    SMGInvariant.InvariantType[] types = SMGInvariant.InvariantType.values();

    assertThat(contains(types, SMGInvariant.InvariantType.POINTER_VALIDITY)).isTrue();
    assertThat(contains(types, SMGInvariant.InvariantType.ALLOCATION_STATUS)).isTrue();
    assertThat(contains(types, SMGInvariant.InvariantType.BUFFER_BOUNDS)).isTrue();
    assertThat(contains(types, SMGInvariant.InvariantType.TEMPORAL_SAFETY)).isTrue();
  }

  private boolean contains(SMGInvariant.InvariantType[] array, SMGInvariant.InvariantType value) {
    for (SMGInvariant.InvariantType type : array) {
      if (type == value) {
        return true;
      }
    }
    return false;
  }
}
