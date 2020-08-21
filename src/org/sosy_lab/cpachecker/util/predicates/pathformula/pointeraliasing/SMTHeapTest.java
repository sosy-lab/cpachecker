// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

public class SMTHeapTest extends SolverBasedTest0 {

  private SMTHeap init() throws InvalidConfigurationException {
    requireUnsatCore();
    requireBitvectors();
    MachineModel model = MachineModel.LINUX64;
    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(config);
    TypeHandlerWithPointerAliasing handler =
        new TypeHandlerWithPointerAliasing(logger, model, options);
    return new SMTHeapWithByteArray(new FormulaManagerView(mgr, config, logger), handler, model);
  }

  @Override
  protected Solvers solverToUse(){
    return Solvers.MATHSAT5;
  }

  @Test
  public void test64BitVector()
      throws InterruptedException, SolverException, InvalidConfigurationException {

    SMTHeap heap = init();


    final BitvectorFormula value = storeBitVector(heap, 64, 0x1122334455667788L);

    final BitvectorFormula result = readBitVector(heap,64);

    BooleanFormula testEquality = bvmgr.equal(value, result);
    this.assertThatFormula(testEquality).isTautological();
  }

  @Test
  public void test32BitVector()
      throws InterruptedException, SolverException, InvalidConfigurationException {

    SMTHeap heap = init();
    final BitvectorFormula value = storeBitVector(heap,32, 0x112233L);

    final BitvectorFormula result = readBitVector(heap, 32);
    BooleanFormula testEquality = bvmgr.equal(value, result);
    this.assertThatFormula(testEquality).isTautological();
  }

  private BitvectorFormula storeBitVector(SMTHeap heap, int length, long value){
    final String targetName = "testTarget"+length;
    final FormulaType<BitvectorFormula> pTargetType =
        mgr.getFormulaType(bvmgr.makeBitvector(length, 0));
    final int oldIndex = 0;
    final int newIndex = 1;
    final BitvectorFormula address = bvmgr.makeBitvector(64, 1234+length);
    final BitvectorFormula bvValue = bvmgr.makeBitvector(length, value);

    @SuppressWarnings("unused")
    BooleanFormula bf = heap.makePointerAssignment(targetName, pTargetType, oldIndex, newIndex, address, bvValue);

    return bvValue;
  }

  private BitvectorFormula readBitVector(SMTHeap heap, int length){
    final String targetName = "testTarget"+length;
    final FormulaType<BitvectorFormula> pTargetType =
        mgr.getFormulaType(bvmgr.makeBitvector(length, 0));
    final int newIndex = 1;
    final BitvectorFormula address = bvmgr.makeBitvector(64, 1234+length);
    return heap.makePointerDereference(targetName, pTargetType, newIndex, address);
}


  @Test
  public void testMixedBitVectors()
      throws InterruptedException, SolverException, InvalidConfigurationException {

    SMTHeap heap = init();
    final BitvectorFormula value8 = storeBitVector(heap, 8, 0x1);
    final BitvectorFormula value16 = storeBitVector(heap, 16, 0x112);
    final BitvectorFormula value32 = storeBitVector(heap, 32, 0x112233L);
    final BitvectorFormula value64 = storeBitVector(heap, 64, 0x1122334455667788L);
    final BitvectorFormula result8 = readBitVector(heap, 8);
    final BitvectorFormula result16 = readBitVector(heap, 16);
    final BitvectorFormula result32 = readBitVector(heap, 32);
    final BitvectorFormula result64 = readBitVector(heap, 64);

    BooleanFormula testEquality8 = bvmgr.equal(value8, result8);
    this.assertThatFormula(testEquality8).isTautological();

    BooleanFormula testEquality16 = bvmgr.equal(value16, result16);
    this.assertThatFormula(testEquality16).isTautological();

    BooleanFormula testEquality32 = bvmgr.equal(value32, result32);
    this.assertThatFormula(testEquality32).isTautological();

    BooleanFormula testEquality64 = bvmgr.equal(value64, result64);
    this.assertThatFormula(testEquality64).isTautological();
  }
}
