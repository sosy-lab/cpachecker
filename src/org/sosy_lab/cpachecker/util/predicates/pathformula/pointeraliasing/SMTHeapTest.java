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
    final String targetName = "testTarget";
    final FormulaType<BitvectorFormula> pTargetType =
        fmgr.getFormulaType(bvfmgr.makeBitvector(64, 0));
    final int oldIndex = 0;
    final int newIndex = 1;
    final BitvectorFormula address = bvfmgr.makeBitvector(64, 1234);
    final BitvectorFormula value = bvfmgr.makeBitvector(64, 0x1122334455667788L);
    BooleanFormula bf =
        heap.makePointerAssignment(targetName, pTargetType, oldIndex, newIndex, address, value);

    BitvectorFormula result =
        heap.makePointerDereference(targetName, pTargetType, newIndex, address);

    BooleanFormula testEquality = bvfmgr.equal(value, result);
    ProverEnvironment prover1 = solvingContext.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT);

    prover1.addConstraint(testEquality);

    Assert.assertFalse(prover1.isUnsat());
    prover1.close();
  }
}
