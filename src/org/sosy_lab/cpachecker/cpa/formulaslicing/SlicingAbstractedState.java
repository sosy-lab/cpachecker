package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

/**
 * Abstracted state, containing invariants obtained by slicing.
 */
public class SlicingAbstractedState extends SlicingState implements
    FormulaReportingState {

  /**
   * Slice with respect to the current SCC.
   * The slice does not contain any intermediate variables, and thus can be
   * represented as an *un-instantiated* formula.
   */
  private final BooleanFormula slice;

  /**
   * Expected starting {@link PointerTargetSet} and {@link SSAMap}.
   */
  private final SSAMap ssaMap;
  private final PointerTargetSet pointerTargetSet;
  private final FormulaManagerView fmgr;

  private SlicingAbstractedState(BooleanFormula pSlice, SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet, FormulaManagerView pFmgr) {
    slice = pSlice;
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    fmgr = pFmgr;
  }

  public static SlicingAbstractedState of(BooleanFormula pSlice,
      SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr) {
    return new SlicingAbstractedState(pSlice, pSsaMap, pPointerTargetSet, pFmgr);
  }

  public SSAMap getSSA() {
    return ssaMap;
  }

  public PointerTargetSet getPointerTargetSet() {
    return pointerTargetSet;
  }

  public BooleanFormula getAbstraction() {
    return slice;
  }

  public static SlicingAbstractedState empty(
      FormulaManagerView pFmgr
  ) {
    return SlicingAbstractedState.of(
        pFmgr.getBooleanFormulaManager().makeBoolean(true),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pFmgr);
  }

  // todo: note on comparison.
  // The only comparable states should be (false) and (true).
  // (false) should be weaker than anything, and
  // (true) should be stronger than anything.

  @Override
  public boolean isAbstracted() {
    return true;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager,
      PathFormulaManager pfmgr) {
    return manager.parse(fmgr.dumpFormula(slice).toString());
  }
}
