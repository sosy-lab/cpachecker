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
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class PredicateCoverageCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCoverageCPA.class);
  }

  private final Map<Long, Double> timeStampsPerCoverage;

  private PredicateCoverageCPA(ConfigurableProgramAnalysis pCpa) {
    super(pCpa);
    timeStampsPerCoverage = new HashMap<>();
  }

  public Map<Long, Double> getTimeStampsPerPredicateCoverage() {
    return timeStampsPerCoverage;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new PredicateCoverageCPATransferRelation(
        getWrappedCpa().getTransferRelation(), timeStampsPerCoverage);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new PredicateCoveragePrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }
}
