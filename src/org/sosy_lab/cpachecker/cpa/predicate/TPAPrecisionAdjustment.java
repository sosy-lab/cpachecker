// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class TPAPrecisionAdjustment extends PredicatePrecisionAdjustment{
  public TPAPrecisionAdjustment(
      LogManager pLogger,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      PredicateCPAInvariantsManager pInvariantSupplier,
      PredicateProvider pPredicateProvider,
      PredicateStatistics pPredicateStatistics) {
    super(pLogger, pFmgr, pPfmgr, pBlk, pPredAbsManager, pInvariantSupplier, pPredicateProvider,
        pPredicateStatistics);
  }
}
