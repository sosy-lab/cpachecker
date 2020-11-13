// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.truth.TruthJUnit;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;

/**
 * Abstract base class for tests that use SMTHeap and SMT solver just like {@link
 * SolverViewBasedTest0}, but additionally providing {@link SMTHeap}.
 */
@RunWith(Parameterized.class)
public abstract class SMTHeapBasedTest0 extends SolverViewBasedTest0 {

  protected SMTHeap heap;

  @Before
  public final void initSMTHeap() throws InvalidConfigurationException {
    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(config);
    TypeHandlerWithPointerAliasing handler =
        new TypeHandlerWithPointerAliasing(logger, modelToUse(), options);

    switch (heapToUse()) {
      case UF:
        heap = new SMTHeapWithUninterpretedFunctionCalls(mgrv);
        break;
      case ARRAYS:
        heap = new SMTHeapWithArrays(mgrv, handler);
        break;
      case SINGLE_BYTE_ARRAY:
        heap = new SMTHeapWithByteArray(mgrv, handler, modelToUse());
        break;
    }
  }

  protected void requireSingleByteArrayHeap() {
    TruthJUnit.assume()
        .withMessage("SMT Heap %s does not use Single Byte Array", this.heapToUse())
        .that(heapToUse())
        .isEqualTo(HeapOptions.SINGLE_BYTE_ARRAY);
  }

  protected void requireArraysHeap() {
    TruthJUnit.assume()
        .withMessage("SMT Heap %s  does not use Arrays", this.heapToUse())
        .that(heapToUse())
        .isEqualTo(HeapOptions.ARRAYS);
  }

  protected void requireUFHeap() {
    TruthJUnit.assume()
        .withMessage("SMT Heap %s does not use UF", this.heapToUse())
        .that(heapToUse())
        .isEqualTo(HeapOptions.UF);
  }

  protected abstract HeapOptions heapToUse();

  protected abstract MachineModel modelToUse();

  protected enum HeapOptions {
    UF,
    SINGLE_BYTE_ARRAY,
    ARRAYS
  }
}
