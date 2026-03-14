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

/**
 * Test class to execute the SMG2-CPA with ILP32 test programs. All programs listed here are
 * executed for all valid specifications supported by the SMG2-CPA, i.e. default specification,
 * MemSafety, MemCleanup, No-Overflow, in two configurations; SMG based Symbolic Execution and SMG
 * based Value Analysis.
 */
public class SMGCPA32Test extends SMGBaseCPATest0 {

  @Ignore // Arrays have a problem in SMG2 currently
  @Test
  public void arrayUsageWithPointerComplex32Proof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_modified_pointers_in_methods_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrViaMallocProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/pointer_arithmetics/pointer_arithmetics_int_malloc_32_safe.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests casting of pointers to numeric values + pointer arithmetics and comparisons,
  //  as well as their correctness in relation to memory layout
  @Test
  public void pointerArithmeticsAndComparisonsIntPtrCastNumericViaMallocProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram =
        "basics/pointer_arithmetics/pointer_arithmetics_numeric_cast_int_malloc_32_safe.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests that (integer) types are not comparable to values from larger types,
  //  e.g. nondet_bool() != 2;
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetIntegerTypeBoundsProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/type_tests/nondet_generator_integer_types_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests that (float) types are not comparable to values from larger types
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetFloatingPointTypeBoundsProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/type_tests/nondet_generator_float_types_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests basic usage of arrays with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests basic usage of arrays with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageViolation() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_32_false.c";
    assertThatILP32Program(testProgram).isUnsafe();
  }

  // Tests basic usage of arrays in methods with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_methods_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests basic usage of arrays in methods with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsViolation() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_methods_32_false.c";
    assertThatILP32Program(testProgram).isUnsafe();
  }

  // Tests basic usage of arrays in methods as pointers with constants
  @Ignore // TODO: enable and see whether we pass this
  @Test
  public void arrayUsageInMethodsAsPointersProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/arrays/array_usage_pointers_in_methods_32_true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests basic usage of function pointers
  @Test
  public void functionPointerSimpleUsageViolation() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/function_pointers/function_pointers_simple_concrete-false.c";
    assertThatILP32Program(testProgram).isUnsafe();
  }

  // Tests basic usage of function pointers
  @Test
  public void functionPointerSimpleUsageProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram = "basics/function_pointers/function_pointers_simple_concrete-true.c";
    assertThatILP32Program(testProgram).isSafe();
  }

  // Tests basic usage of function pointers in/from functions
  @Test
  public void functionPointerSimpleUsageInFunctionsAndReturnsViolation() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram =
        "basics/function_pointers/function_pointers_in_functions_simple_concrete-false.c";
    assertThatILP32Program(testProgram).isUnsafe();
  }

  // Tests basic usage of function pointers in/from functions
  @Test
  public void functionPointerSimpleUsageInFunctionsAndReturnsProof() throws Exception {
    doNotTestOverflowSpecification();
    String testProgram =
        "basics/function_pointers/function_pointers_in_functions_simple_concrete-true.c";
    assertThatILP32Program(testProgram).isSafe();
  }
}
