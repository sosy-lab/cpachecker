// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public interface PathFormulaBuilder {

  PathFormulaBuilder makeOr(PathFormulaBuilder other);

  PathFormulaBuilder makeAnd(CFAEdge pEdge);

  PathFormulaBuilder makeAnd(CExpression pCExpression);

  PathFormula build(PathFormulaManager pPfmgr, PathFormula pathFormula)
      throws CPATransferException, InterruptedException;
}
