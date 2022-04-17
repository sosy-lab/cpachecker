// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class PredicateCoverageCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCoverageCPA.class);
  }

  private final CoverageData coverageData = new CoverageData();
  private final FormulaManagerView fmgr;
  private final CFA cfa;

  private PredicateCoverageCPA(ConfigurableProgramAnalysis pCpa, CFA pCFA)
      throws InvalidConfigurationException {
    super(pCpa);
    coverageData.putCFA(pCFA);
    cfa = pCFA;
    fmgr = getFormulaManagerView();
  }

  private FormulaManagerView getFormulaManagerView() throws InvalidConfigurationException {
    if (getWrappedCpa() instanceof PredicateCPA) {
      PredicateCPA predicateCPA = (PredicateCPA) getWrappedCpa();
      Solver solver = predicateCPA.getSolver();
      return solver.getFormulaManager();
    }
    throw new InvalidConfigurationException(
        "PredicateCoverageCPA is a wrapper CPA that requires the contained CPA to be an "
            + "instance of PredicateCPA, but configured was a "
            + getWrappedCpa().getClass().getSimpleName());
  }

  public CoverageData getCoverageData() {
    return coverageData;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new PredicateCoverageCPATransferRelation(
        getWrappedCpa().getTransferRelation(), fmgr, cfa, coverageData);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new PredicateCoveragePrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }
}
