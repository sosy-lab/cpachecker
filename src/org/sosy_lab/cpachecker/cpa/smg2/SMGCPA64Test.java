// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/** Test class to execute the SMG2 CPA with LP64 test programs. */
public class SMGCPA64Test extends SMGBaseCPATest {

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrViaMallocProof() throws Exception {
    String testProgram =
        "test/programs/pointer_arithmetics/pointer_arithmetics_int_malloc_64_safe.c";
    runAndAssertSafe(testProgram);
  }

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrCastNumericViaMallocProof() throws Exception {
    String testProgram =
        "test/programs/pointer_arithmetics/pointer_arithmetics_numeric_cast_int_malloc_64_safe.c";
    runAndAssertSafe(testProgram);
  }

  // Tests that (integer) types are not comparable to values from larger types,
  //  e.g. nondet_bool() != 2;
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetIntegerTypeBoundsProof() throws Exception {
    String testProgram = "test/programs/basics/type_tests/nondet_generator_integer_types_64_true.c";
    runAndAssertSafe(testProgram);
  }

  // Tests that (float) types are not comparable to values from larger types
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetFloatingPointTypeBoundsProof() throws Exception {
    String testProgram = "test/programs/basics/type_tests/nondet_generator_float_types_64_true.c";
    runAndAssertSafe(testProgram);
  }

  protected static MachineModel getMachineModel() {
    return MachineModel.LINUX64;
  }
}
