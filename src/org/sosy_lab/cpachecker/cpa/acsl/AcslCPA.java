// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * This is a CPA whose goal is to find and inject ACSL invariants. For now this class is used for
 * testing and proof of concept.
 */
public class AcslCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  private final CFA cfa;
  private final LogManager logger;
  private final Solver solver;
  private final CToFormulaConverterWithPointerAliasing converter;

  private AcslCPA(
      CFA pCFA, LogManager pLogManager, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    this.cfa = pCFA;
    this.logger = pLogManager;
    solver = Solver.create(pConfig, this.logger, pShutdownNotifier);
    FormulaManagerView fmgr = solver.getFormulaManager();
    converter =
        initializeCToFormulaConverter(
            fmgr, this.logger, pConfig, pShutdownNotifier, pCFA.getMachineModel());
    logger.log(Level.INFO,"AcslCPA created.");
  }

  private CToFormulaConverterWithPointerAliasing initializeCToFormulaConverter(
      FormulaManagerView pFormulaManager,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel)
      throws InvalidConfigurationException {

    CFormulaEncodingWithPointerAliasingOptions options =
        new CFormulaEncodingWithPointerAliasingOptions(pConfig);
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, pMachineModel, options);

    return new CToFormulaConverterWithPointerAliasing(
        options,
        pFormulaManager,
        pMachineModel,
        Optional.empty(),
        pLogger,
        pShutdownNotifier,
        typeHandler,
        AnalysisDirection.FORWARD);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AcslCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new AcslTransferRelation(cfa, logger);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    // TODO return to this when I have thought more about how my abstract states should look
    return new AcslState(logger, cfa, solver, converter, ImmutableSet.of());
  }
}
