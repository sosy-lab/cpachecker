// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCpaOptions;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStatistics;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class TPATransferRelation extends PredicateTransferRelation {
  public TPATransferRelation(
      LogManager pLogger,
      AnalysisDirection pDirection,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      PredicateStatistics pStatistics,
      PredicateCpaOptions pOptions) {
    super(pLogger, pDirection, pFmgr, pPfmgr, pBlk, pPredAbsManager, pStatistics, pOptions);
  }

}
