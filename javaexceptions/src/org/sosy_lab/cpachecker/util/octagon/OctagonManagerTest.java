// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

import static com.google.common.truth.Truth.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

public class OctagonManagerTest {

  static OctagonManager manager;

  @BeforeClass
  public static void setUpBeforeClass() {
    manager = new OctagonFloatManager();
  }

  @Test
  public void testNum_Int() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_int(num, 0, 3);
    assertThat(manager.num_infty(num, 0)).isFalse();
    assertThat(manager.num_get_int(num, 0)).isEqualTo(3);
    assertThat(manager.num_get_float(num, 0)).isWithin(0).of(3);
  }

  @Test
  public void testNum_Float() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_float(num, 0, 3.3);
    assertThat(manager.num_infty(num, 0)).isFalse();
    assertThat(manager.num_get_int(num, 0)).isEqualTo(3);
    assertThat(manager.num_get_float(num, 0)).isWithin(0).of(3.3);
  }
}
