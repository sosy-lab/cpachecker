// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class SMTHeapTest {

  private SMTHeap heap;
  private FormulaManagerView fmgr;
  private BitvectorFormulaManager bvfmgr;
  private SolverContext solvingContext;

  @Before
  public void init() throws InvalidConfigurationException {
    Solvers solver = Solvers.MATHSAT5;

    LogManager logger = LogManager.createNullLogManager();

    Configuration config = Configuration.defaultConfiguration();

    ShutdownNotifier notifier = ShutdownNotifier.createDummy();

    SolverContextFactory solverFactory = new SolverContextFactory(config, logger, notifier);

    solvingContext = solverFactory.generateContext(solver);
    fmgr = new FormulaManagerView(solvingContext.getFormulaManager(), config, logger);
    bvfmgr = fmgr.getBitvectorFormulaManager();
    MachineModel model = MachineModel.LINUX64;
    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(config);
    TypeHandlerWithPointerAliasing handler =
        new TypeHandlerWithPointerAliasing(logger, model, options);
    heap = new SMTHeapWithByteArray(fmgr, handler, model);
  }

  @Test
  public void test64BitVector() throws InterruptedException, SolverException {

    final BitvectorFormula value = storeBitVector(64, 0x1122334455667788L);

    final BitvectorFormula result = readBitVector(64);

    BooleanFormula testEquality = bvfmgr.equal(value, result);
    ProverEnvironment prover1 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);

    prover1.addConstraint(testEquality);

    Assert.assertFalse(prover1.isUnsat());
    prover1.close();
  }

  @Test
  public void test32BitVector() throws InterruptedException, SolverException {


    final BitvectorFormula value = storeBitVector(32, 0x112233L);

    final BitvectorFormula result = readBitVector(32);

    BooleanFormula testEquality = bvfmgr.equal(value, result);
    ProverEnvironment prover1 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);

    prover1.addConstraint(testEquality);

    Assert.assertFalse(prover1.isUnsat());
    prover1.close();
  }

  private BitvectorFormula storeBitVector(int length, long value){
    final String targetName = "testTarget"+length;
    final FormulaType<BitvectorFormula> pTargetType =
        fmgr.getFormulaType(bvfmgr.makeBitvector(length, 0));
    final int oldIndex = 0;
    final int newIndex = 1;
    final BitvectorFormula address = bvfmgr.makeBitvector(64, 1234+length);
    final BitvectorFormula bvValue = bvfmgr.makeBitvector(length, value);

    @SuppressWarnings("unused")
    BooleanFormula bf = heap.makePointerAssignment(targetName, pTargetType, oldIndex, newIndex, address, bvValue);

    return bvValue;
  }

  private BitvectorFormula readBitVector(int length){
    final String targetName = "testTarget"+length;
    final FormulaType<BitvectorFormula> pTargetType =
        fmgr.getFormulaType(bvfmgr.makeBitvector(length, 0));
    final int newIndex = 1;
    final BitvectorFormula address = bvfmgr.makeBitvector(64, 1234+length);
    return heap.makePointerDereference(targetName, pTargetType, newIndex, address);
}


  @Test
  public void testMixedBitVectors() throws InterruptedException, SolverException {

    final BitvectorFormula value8 = storeBitVector(8, 0x1);
    final BitvectorFormula value16 = storeBitVector(16, 0x112);
    final BitvectorFormula value32 = storeBitVector(32, 0x112233L);
    final BitvectorFormula value64 = storeBitVector(64, 0x1122334455667788L);
    final BitvectorFormula result8 = readBitVector(8);
    final BitvectorFormula result16 = readBitVector(16);
    final BitvectorFormula result32 = readBitVector(32);
    final BitvectorFormula result64 = readBitVector(64);

    BooleanFormula testEquality8 = bvfmgr.equal(value8, result8);
    ProverEnvironment prover8 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);
    prover8.addConstraint(testEquality8);
    Assert.assertFalse(prover8.isUnsat());
    prover8.close();


    BooleanFormula testEquality16 = bvfmgr.equal(value16, result16);
    ProverEnvironment prover16 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);
    prover16.addConstraint(testEquality16);
    Assert.assertFalse(prover16.isUnsat());
    prover16.close();


    BooleanFormula testEquality32 = bvfmgr.equal(value32, result32);
    ProverEnvironment prover32 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);
    prover32.addConstraint(testEquality32);
    Assert.assertFalse(prover32.isUnsat());
    prover32.close();



    BooleanFormula testEquality64 = bvfmgr.equal(value64, result64);
    ProverEnvironment prover64 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);
    prover64.addConstraint(testEquality64);
    Assert.assertFalse(prover64.isUnsat());
    prover64.close();


  }
}
