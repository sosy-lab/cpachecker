// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class to execute the SMG2-CPA with the MemSafety specification and check the exported YAML
 * correctness witnesses in version 2.4.
 */
public class SmgCpaWitnessV2d4IntegrationTest extends SMGCPAIntegrationTest0 {

  private static final String MEMSAFETY_CORRECTNESS_WITNESS_PROGRAMS =
      "witness-v2-validation/memsafety-correctness/";

  @Before
  public void reduceCILoad() {
    onlyTestMemSafetySpecification();
    onlyTestSMGSymbolicExecutionConfiguration();
  }

  // Concrete values of a global variable, a local struct, memory on the heap reachable from a local
  // pointer, and the loop counter are exported as loop invariant of the for loop in line 29.
  private static final ImmutableList<String> CONCRETE_VALUES_LOOP_INVARIANT_EXPRESSIONS =
      ImmutableList.of(
          "counter == (0)",
          "counter == (1)",
          "counter == (2)",
          "i == (0)",
          "i == (1)",
          "i == (2)",
          "(*(heap_value)) == (5)",
          "(local_pair.first) == (1)",
          "(local_pair.second) == (2)");

  @Test
  public void correctnessWitnessV2d4WithConcreteValuesILP32() throws Exception {
    assertThatILP32Program(MEMSAFETY_CORRECTNESS_WITNESS_PROGRAMS + "concrete_values_loop.c")
        .returnsCorrectnessWitnessV2d4WithLoopInvariantContaining(
            "main", 29, 3, CONCRETE_VALUES_LOOP_INVARIANT_EXPRESSIONS);
  }

  @Test
  public void correctnessWitnessV2d4WithConcreteValuesLP64() throws Exception {
    assertThatLP64Program(MEMSAFETY_CORRECTNESS_WITNESS_PROGRAMS + "concrete_values_loop.c")
        .returnsCorrectnessWitnessV2d4WithLoopInvariantContaining(
            "main", 29, 3, CONCRETE_VALUES_LOOP_INVARIANT_EXPRESSIONS);
  }
}
