// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.truth.TruthJUnit.assume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This test method writes a bitvector values different lengths in different Heap, SMT solver and
 * machine model combinations. In the second step the written value is read and compared with the
 * initial value.
 */
@RunWith(Parameterized.class)
public class SMTHeapReadAndWriteTest extends SMTHeapBasedTest0 {

  private static final String TEST_TARGET_PRE = "testTarget";
  private static final String TEST_VAR_NAME_PRE = "var";
  private static final int TEST_ADDRESS_PRE = 1234;

  @Parameter(0)
  public Solvers solverUnderTest;

  @Parameter(1)
  public MachineModel model;

  @Parameter(2)
  public HeapOptions heapToUse;

  private int index;

  @Parameters(name = "smtSolver= {0}, machineModel={1}, heapEncoding={2}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();

    for (Solvers solver : Solvers.values()) {
      for (MachineModel model : MachineModel.values()) {
        for (HeapOptions heapOption : HeapOptions.values()) {
          params.add(new Object[] {solver, model, heapOption});
        }
      }
    }
    return params;
  }

  @Before
  public void init() {
    requireBitvectors();
    requireArrays();
    assume()
        .withMessage("Solver %s does not support arrays of bitvectors", solverToUse())
        .that(solverToUse())
        .isNotEqualTo(Solvers.PRINCESS);
    index = 0;
  }

  @Test
  public void test8BitVector() throws InterruptedException, SolverException {
    testWrittenValueIsEquisatisfiableToReadValue(8, 0x11L);
  }

  @Test
  public void test16BitVector() throws InterruptedException, SolverException {
    testWrittenValueIsEquisatisfiableToReadValue(16, 0x1122L);
  }

  @Test
  public void test32BitVector() throws InterruptedException, SolverException {
    testWrittenValueIsEquisatisfiableToReadValue(32, 0x112233L);
  }

  @Test
  public void test64BitVector() throws InterruptedException, SolverException {
    testWrittenValueIsEquisatisfiableToReadValue(64, 0x1122334455667788L);
  }

  @Test
  public void testMixedBitVectors() throws InterruptedException, SolverException {
    testWrittenValueIsEquisatisfiableToReadValue(8, 0x11L);
    testWrittenValueIsEquisatisfiableToReadValue(16, 0x1122L);
    testWrittenValueIsEquisatisfiableToReadValue(32, 0x112233L);
    testWrittenValueIsEquisatisfiableToReadValue(64, 0x1122334455667788L);
  }

  /**
   * This test method writes a bitvector value with a given length in the heap. In the second step
   * the written value is read and compared with the initial value.
   *
   * @param length - the bitvectors length
   * @param pValue - the value
   */
  private void testWrittenValueIsEquisatisfiableToReadValue(int length, long pValue)
      throws SolverException, InterruptedException {
    final BitvectorFormula value = bvmgr.makeBitvector(length, pValue);
    BooleanFormula atom =
        mgrv.assignment(bvmgr.makeVariable(length, TEST_VAR_NAME_PRE + length), value);
    BooleanFormula wroteArrayFormula = storeBitVector(value);
    final BooleanFormula readResultFormula = readBitVector(length, wroteArrayFormula);
    this.assertThatFormula(readResultFormula)
        .isEquisatisfiableTo(atom); // =and(result, not(atom)).isUnsatisfiable()
  }

  private BooleanFormula storeBitVector(BitvectorFormula value) {
    int length = bvmgr.getLength(value);
    final String targetName = TEST_TARGET_PRE + length;
    final FormulaType<BitvectorFormula> pTargetType = FormulaType.getBitvectorTypeWithSize(length);
    final BitvectorFormula address =
        bvmgr.makeBitvector(model.getSizeofPtrInBits(), TEST_ADDRESS_PRE + length);
    return heap.makePointerAssignment(targetName, pTargetType, index, ++index, address, value);
  }

  private BooleanFormula readBitVector(int length, BooleanFormula contextBF) {
    final String targetName = TEST_TARGET_PRE + length;
    final FormulaType<BitvectorFormula> pTargetType = FormulaType.getBitvectorTypeWithSize(length);
    final BitvectorFormula address =
        bvmgr.makeBitvector(model.getSizeofPtrInBits(), TEST_ADDRESS_PRE + length);
    final BitvectorFormula valueFormula =
        heap.makePointerDereference(targetName, pTargetType, index, address);
    final BooleanFormula booleanFormula =
        mgrv.assignment(bvmgr.makeVariable(length, TEST_VAR_NAME_PRE + length), valueFormula);
    return mgrv.makeAnd(booleanFormula, contextBF);
  }

  @Override
  protected SMTHeapBasedTest0.HeapOptions heapToUse() {
    return heapToUse;
  }

  @Override
  protected MachineModel modelToUse() {
    return model;
  }

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }
}
