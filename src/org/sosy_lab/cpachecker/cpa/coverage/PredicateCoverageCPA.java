// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateCoverageCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCoverageCPA.class);
  }

  private final Set<BooleanFormula> predictedPredicates;
  private final FormulaManagerView fmgr;
  private final Map<Long, Double> timeStampsPerCoverage;

  private PredicateCoverageCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA)
      throws InvalidConfigurationException, UnrecognizedCodeException, InterruptedException {
    super(pCpa);

    timeStampsPerCoverage = new HashMap<>();
    Solver solver = Solver.create(config, pLogger, pShutdownNotifier);
    fmgr = solver.getFormulaManager();
    FormulaEncodingOptions options = new FormulaEncodingOptions(config);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, pCFA.getMachineModel());
    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            options,
            fmgr,
            pCFA.getMachineModel(),
            pCFA.getVarClassification(),
            pLogger,
            pShutdownNotifier,
            typeHandler,
            AnalysisDirection.FORWARD);

    predictedPredicates = PredicatePredictionHeuristic.predictPredicates(pCFA, converter);
  }

  public Map<Long, Double> getTimeStampsPerPredicateCoverage() {
    return timeStampsPerCoverage;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new PredicateCoverageCPATransferRelation(
        getWrappedCpa().getTransferRelation(), predictedPredicates, fmgr, timeStampsPerCoverage);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new PredicateCoveragePrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }
}
