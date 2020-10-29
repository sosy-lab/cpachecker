// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;

public class TestCompTestcaseLoaderTest {

  static final String PREFIX = "test/programs/testcomp-tests/";

  @Test
  public void testLoadingSingleInput() throws Exception {
    // check if a test-case with a single input is loaded correctly
    Map<Integer, String> expected = new HashMap<>();
    expected.put(0, "2");

    Map<Integer, String> loadedInputs =
        TestCompTestcaseLoader.loadTestcase(new File(PREFIX + "testfile1.xml").toPath());
    assertMapEquals(expected, loadedInputs);
  }

  @Test
  public void testLoadingSeveralInputs() throws Exception {
    // check if a test-case with a single input is loaded correctly
    Map<Integer, String> expected = new HashMap<>();
    expected.put(0, "1");
    expected.put(1, "2");
    expected.put(2, "42");

    Map<Integer, String> loadedInputs =
        TestCompTestcaseLoader.loadTestcase(new File(PREFIX + "testfile2.xml").toPath());
    assertMapEquals(expected, loadedInputs);
  }

  /** Verifies that two maps are equal */
  private void assertMapEquals(Map<Integer, String> pExpected, Map<Integer, String> pActual) {
    assertThat(pActual.entrySet()).hasSize(pExpected.entrySet().size());

    for (Entry<Integer, String> e : pExpected.entrySet()) {
      assertThat(pActual.get(e.getKey())).isEqualTo(e.getValue());
    }
  }


}
