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

/** Test class to execute the SMG2 CPA with LP32 test programs. */
public class SMGCPA32Test extends SMGBaseCPATest {

  @Test
  public void pointerArithmeticsAndComparisonsIntPtrViaMallocProof() throws Exception {
    String testProgram =
        "test/programs/pointer_arithmetics/pointer_arithmetics_int_malloc_32_safe.c";
    runAndAssertSafe(testProgram);
  }

  // Tests casting of pointers to numeric values + pointer arithmetics and comparisons,
  //  as well as their correctness in relation to memory layout
  @Test
  public void pointerArithmeticsAndComparisonsIntPtrCastNumericViaMallocProof() throws Exception {
    String testProgram =
        "test/programs/pointer_arithmetics/pointer_arithmetics_numeric_cast_int_malloc_32_safe.c";
    runAndAssertSafe(testProgram);
  }

  // Tests that types are not comparable to values from larger types, e.g. nondet_bool() != 2;
  @Ignore // Ignore as we currently fail this in SMG2
  @Test
  public void nondetNumericTypeBoundsProof() throws Exception {
    String testProgram =
        "test/programs/basics/type_tests/nondet_generator_numeric_types_32_true.c";
    runAndAssertSafe(testProgram);
  }

  protected static MachineModel getMachineModel() {
    return MachineModel.LINUX32;
  }
}
