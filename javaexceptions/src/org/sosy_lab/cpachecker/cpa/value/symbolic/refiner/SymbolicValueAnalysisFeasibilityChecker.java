// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsTransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;

/**
 * Feasibility checker for value analysis handling symbolic values. A composition of {@link
 * ConstraintsTransferRelation} and {@link ValueAnalysisTransferRelation} is used for checking
 * feasibility. In contrast to this approach, {@link ValueAnalysisFeasibilityChecker} only uses a
 * ValueAnalysisTransferRelation and as such cannot fully handle symbolic values.
 */
public class SymbolicValueAnalysisFeasibilityChecker
    extends GenericFeasibilityChecker<ForgettingCompositeState>
    implements SymbolicFeasibilityChecker {

  public SymbolicValueAnalysisFeasibilityChecker(
      final SymbolicStrongestPostOperator pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(
        pStrongestPostOperator,
        getInitialCompositeState(pCfa.getMachineModel()),
        ValueAnalysisCPA.class,
        pLogger,
        pConfig,
        pCfa);
  }

  private static ForgettingCompositeState getInitialCompositeState(MachineModel pMachineModel) {
    final ValueAnalysisState valueState = new ValueAnalysisState(pMachineModel);
    final ConstraintsState constraintsState = new ConstraintsState();

    return new ForgettingCompositeState(valueState, constraintsState);
  }
}
