// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class FunArrayTest {

  @Test
  public void ofInitializerListWithEmptyListReturnsBottom() {
    FunArray result = FunArray.ofInitializerList(ImmutableList.of(), null);
    assertThat(result).isEqualTo(FunArray.BOTTOM);
  }
}
