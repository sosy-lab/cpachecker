// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/**
 * Test class to execute the SMG2-CPA with LP64 test programs. All programs listed here are executed
 * for all valid specifications supported by the SMG2-CPA, i.e. default specification, MemSafety,
 * MemCleanup, No-Overflow, in two configurations; SMG based Symbolic Execution and SMG based Value
 * Analysis.
 */
public class SMGCPA64Test extends SMGBaseCPATest {

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrViaMallocProof() throws Exception {
    String testProgram = "pointer_arithmetics/pointer_arithmetics_int_malloc_64_safe.c";
    assertThatProgram(testProgram).isSafe();
  }

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrCastNumericViaMallocProof() throws Exception {
    String testProgram =
        "pointer_arithmetics/pointer_arithmetics_numeric_cast_int_malloc_64_safe.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests that (integer) types are not comparable to values from larger types,
  //  e.g. nondet_bool() != 2;
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetIntegerTypeBoundsProof() throws Exception {
    String testProgram = "basics/type_tests/nondet_generator_integer_types_64_true.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests that (float) types are not comparable to values from larger types
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetFloatingPointTypeBoundsProof() throws Exception {
    String testProgram = "basics/type_tests/nondet_generator_float_types_64_true.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests basic usage of arrays with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageProof() throws Exception {
    String testProgram = "basics/array_tests/array_usage_64_true.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests basic usage of arrays with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageViolation() throws Exception {
    String testProgram = "basics/array_tests/array_usage_64_false.c";
    assertThatProgram(testProgram).isUnsafe();
  }

  // Tests basic usage of arrays in methods with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsProof() throws Exception {
    String testProgram = "basics/array_tests/array_usage_methods_64_true.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests basic usage of arrays in methods with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsViolation() throws Exception {
    String testProgram = "basics/array_tests/array_usage_methods_64_false.c";
    assertThatProgram(testProgram).isUnsafe();
  }

  // Tests basic usage of arrays in methods as pointers with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsAsPointersProof() throws Exception {
    String testProgram = "basics/array_tests/array_usage_pointers_in_methods_64_true.c";
    assertThatProgram(testProgram).isSafe();
  }

  // Tests basic usage of arrays in methods as pointers with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsAsPointersViolation() throws Exception {
    String testProgram = "basics/array_tests/array_usage_pointers_in_methods_64_false.c";
    assertThatProgram(testProgram).isUnsafe();
  }

  protected static MachineModel getMachineModel() {
    return MachineModel.LINUX64;
  }
}
