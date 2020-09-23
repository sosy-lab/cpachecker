// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class SMTHeapTest extends SolverViewBasedTest0 {

  private static final String TEST_TARGET_PRE = "testTarget";

  @Parameter(1)
  public MachineModel model;

  @Parameter(0)
  public Solvers solverUnderTest;

  private SMTHeap heap;

  @Parameters(name = "solver= {0}, model={1}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();

    for (Solvers solver : Solvers.values()) {
      for (MachineModel model : MachineModel.values()) {
        if (solver != Solvers.PRINCESS) {
          params.add(new Object[] {solver, model});
        }
      }
    }
    return params;
  }

  @Before
  public void init() throws InvalidConfigurationException {
    requireBitvectors();
    requireArrays();
    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(config);
    TypeHandlerWithPointerAliasing handler =
        new TypeHandlerWithPointerAliasing(logger, model, options);
    heap = new SMTHeapWithByteArray(mgrv, handler, model);
    assert bvmgr != null;
  }

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  @Test
  public void test64BitVector()
      throws InterruptedException, SolverException, InvalidConfigurationException {
    final BitvectorFormula value = bvmgr.makeBitvector(64, 0x1122334455667788L);
    BooleanFormula atom = bvmgr.equal(bvmgr.makeVariable(64, "a"), value);

    BooleanFormula bf = storeBitVector(value);
    final Formula result = readBitVector(64, bf);
    BooleanFormula testEquality = mgrv.makeEqual(atom, result);
    // Use SMT - Variables
    this.assertThatFormula(testEquality).isTautological();
  }

  @Test
  public void test8BitVector() throws InterruptedException, SolverException {
    final BitvectorFormula value = bvmgr.makeBitvector(8, 0x11L);
    BooleanFormula atom = bvmgr.equal(bvmgr.makeVariable(8, "a"), value);

    BooleanFormula bf = storeBitVector(value);
    final Formula result = readBitVector(8, bf);
    BooleanFormula testEquality = mgrv.makeEqual(atom, result);
    this.assertThatFormula(testEquality).isTautological();
  }

  @Test
  public void test32BitVector() throws InterruptedException, SolverException {
    final BitvectorFormula value = bvmgr.makeBitvector(32, 0x112233L);
    BooleanFormula atom = mgrv.assignment(bvmgr.makeVariable(32, "a"), value);

    BooleanFormula bf = storeBitVector(value);
    final Formula result = readBitVector(32, bf);
    BooleanFormula testEquality = mgrv.makeEqual(atom, result);
    this.assertThatFormula(testEquality).isTautological();
  }

  @Test
  public void testMixedBitVectors() throws InterruptedException, SolverException {
    final BitvectorFormula value8 = bvmgr.makeBitvector(8, 0x11L);
    BooleanFormula atom8 = bvmgr.equal(bvmgr.makeVariable(8, "a"), value8);

    final BitvectorFormula value16 = bvmgr.makeBitvector(16, 0x1122L);
    BooleanFormula atom16 = bvmgr.equal(bvmgr.makeVariable(16, "b"), value16);

    final BitvectorFormula value32 = bvmgr.makeBitvector(32, 0x112233L);
    BooleanFormula atom32 = bvmgr.equal(bvmgr.makeVariable(32, "c"), value32);

    final BitvectorFormula value64 = bvmgr.makeBitvector(64, 0x1122334455667788L);
    BooleanFormula atom64 = bvmgr.equal(bvmgr.makeVariable(64, "d"), value64);

    BooleanFormula bf8 = storeBitVector(value8);
    BooleanFormula bf16 = storeBitVector(value16);
    BooleanFormula bf32 = storeBitVector(value32);
    BooleanFormula bf64 = storeBitVector(value64);

    final Formula result8 = readBitVector(8, bf8);
    final Formula result16 = readBitVector(16, bf16);
    final Formula result32 = readBitVector(32, bf32);
    final Formula result64 = readBitVector(64, bf64);

    BooleanFormula testEquality8 = mgrv.makeEqual(atom8, result8);
    this.assertThatFormula(testEquality8).isTautological();

    BooleanFormula testEquality16 = mgrv.makeEqual(atom16, result16);
    this.assertThatFormula(testEquality16).isTautological();

    BooleanFormula testEquality32 = mgrv.makeEqual(atom32, result32);
    this.assertThatFormula(testEquality32).isTautological();

    BooleanFormula testEquality64 = mgrv.makeEqual(atom64, result64);
    this.assertThatFormula(testEquality64).isTautological();
  }

  private BooleanFormula storeBitVector(BitvectorFormula value) {
    int length = bvmgr.getLength(value);
    final String targetName = TEST_TARGET_PRE + length;
    final FormulaType<BitvectorFormula> pTargetType = FormulaType.getBitvectorTypeWithSize(length);
    final int oldIndex = 0;
    final int newIndex = 1;
    final BitvectorFormula address = bvmgr.makeBitvector(model.getSizeofPtrInBits(), 1234 + length);
    // Konjunktion mit DEREF!!!
    return heap.makePointerAssignment(targetName, pTargetType, oldIndex, newIndex, address, value);
  }

  private Formula readBitVector(int length, BooleanFormula contextBF) {
    final String targetName = TEST_TARGET_PRE + length;
    final FormulaType<BitvectorFormula> pTargetType = FormulaType.getBitvectorTypeWithSize(length);
    final int newIndex = 1;
    final BitvectorFormula address = bvmgr.makeBitvector(model.getSizeofPtrInBits(), 1234 + length);
    final BitvectorFormula valueFormula =
        heap.makePointerDereference(targetName, pTargetType, newIndex, address);

    final BooleanFormula cf =
        mgrv.assignment(
            mgrv.makeVariable(mgrv.getFormulaType(contextBF), "context" + length), contextBF);
    final BooleanFormula booleanFormula =
        mgrv.assignment(bvmgr.makeVariable(length, "var" + length), valueFormula);
    final BooleanFormula resBF = mgrv.makeEqual(booleanFormula, contextBF);

    return mgrv.assignment(mgrv.makeVariable(mgrv.getFormulaType(resBF), "res" + length), resBF);
  }
}
