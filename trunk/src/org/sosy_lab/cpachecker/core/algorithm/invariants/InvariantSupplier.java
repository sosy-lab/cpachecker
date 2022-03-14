// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface InvariantSupplier {

  /**
   * Return an invariant that holds at a given node. This method should be relatively cheap and do
   * not block (i.e., do not start an expensive invariant generation procedure).
   *
   * <p>Invariants returned by this supplier can be assumed to be correct in the given {@code
   * pContext} e.g. respect the {@linkplain PointerTargetSet} and the {@link SSAMap}.
   *
   * @param node The CFANode.
   * @param callstackInformation Optional callstack information, to filter invariants by callstack.
   *     Obtained from {@link CallstackStateEqualsWrapper}. Ignored if absent.
   * @param fmgr The formula manager which should be used for creating the invariant formula.
   * @param pfmgr The {@link PathFormulaManager} which should be used for creating the invariant
   *     formula.
   * @param pContext the context of the formula.
   * @return An invariant boolean formula without SSA indices.
   * @throws InterruptedException if retrieving the invariant is interrupted.
   */
  BooleanFormula getInvariantFor(
      CFANode node,
      Optional<CallstackStateEqualsWrapper> callstackInformation,
      FormulaManagerView fmgr,
      PathFormulaManager pfmgr,
      @Nullable PathFormula pContext)
      throws InterruptedException;

  enum TrivialInvariantSupplier implements InvariantSupplier {
    INSTANCE;

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pNode,
        Optional<CallstackStateEqualsWrapper> callstackInformation,
        FormulaManagerView pFmgr,
        PathFormulaManager pfmgr,
        PathFormula pContext) {
      return pFmgr.getBooleanFormulaManager().makeTrue();
    }
  }
}
