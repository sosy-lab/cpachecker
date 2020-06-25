/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
 * Feasibility checker for value analysis handling symbolic values.
 * A composition of {@link ConstraintsTransferRelation} and {@link ValueAnalysisTransferRelation}
 * is used for checking feasibility.
 * In contrast to this approach, {@link ValueAnalysisFeasibilityChecker} only uses a
 * ValueAnalysisTransferRelation and as such cannot fully handle symbolic values.
 */
public class SymbolicValueAnalysisFeasibilityChecker
    extends GenericFeasibilityChecker<ForgettingCompositeState>
    implements SymbolicFeasibilityChecker {

  public SymbolicValueAnalysisFeasibilityChecker(
      final SymbolicStrongestPostOperator pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa
  ) throws InvalidConfigurationException {

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
