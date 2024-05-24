// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.primitives.Ints;
import org.junit.Test;

public class StringUtilTest {

  @Test
  public void test_singleton() {
    assertThat(rangeToStringCollapsed(0)).isEqualTo("0");
    assertThat(rangeToStringCollapsed(1)).isEqualTo("1");
  }

  @Test
  public void test_range_length_2() {
    assertThat(rangeToStringCollapsed(0, 1)).isEqualTo("0,1");
    assertThat(rangeToStringCollapsed(1, 2)).isEqualTo("1,2");
  }

  @Test
  public void test_longer_range() {
    assertThat(rangeToStringCollapsed(0, 1, 2, 3, 4)).isEqualTo("0-4");
    assertThat(rangeToStringCollapsed(1, 2, 3)).isEqualTo("1-3");
  }

  @Test
  public void test_ranges() {
    assertThat(rangeToStringCollapsed(0, 1, 2, 3, 4, 10, 11, 12, 13, 14)).isEqualTo("0-4,10-14");
    assertThat(rangeToStringCollapsed(1, 2, 3, 5, 6, 7)).isEqualTo("1-3,5-7");
  }

  @Test
  public void test_mixed() {
    assertThat(rangeToStringCollapsed(0, 2, 3, 5, 6, 7, 9, 10)).isEqualTo("0,2,3,5-7,9,10");
  }

  @Test
  public void test_duplicates_unsorted() {
    assertThat(rangeToStringCollapsed(7, 5, 1, 3, 4, 5, 7)).isEqualTo("1,3-5,7");
  }

  private static String rangeToStringCollapsed(int... values) {
    return StringUtil.convertIntegerRangesToStringCollapsed(Ints.asList(values)).toString();
  }
}
