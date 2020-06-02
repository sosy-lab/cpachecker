// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;


public class ForwardingInvariantSupplier implements InvariantSupplier {

  private InvariantSupplier delegate = TrivialInvariantSupplier.INSTANCE;

  public void setInvariantSupplier(InvariantSupplier pInvSup) {
    delegate = pInvSup;
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode,
      Optional<CallstackStateEqualsWrapper> callstackInformation,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      PathFormula pContext)
      throws InterruptedException {
    return delegate.getInvariantFor(pNode, callstackInformation, pFmgr, pPfmgr, pContext);
  }

}
