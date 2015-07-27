package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

/**
 * Abstracted state, containing invariants obtained by slicing.
 *
 * The invariant is universally true wrt the intermediate state
 * which was used for the abstraction.
 */
public class SlicingAbstractedState extends SlicingState implements
    FormulaReportingState, Graphable {

  /**
   * Slice with respect to the current loop.
   * The slice does not contain any intermediate variables, and thus can be
   * represented as an *un-instantiated* formula.
   */
  private final BooleanFormula slice;

  /**
   * Expected starting {@link PointerTargetSet} and {@link SSAMap}.
   */
  private final SSAMap ssaMap;
  private final PointerTargetSet pointerTargetSet;

  /**
   * Backpointer to the current {@link FormulaManagerView}, used for dumping
   * the formulas.
   */
  private final FormulaManagerView fmgr;

  private final CFANode node;

  private SlicingAbstractedState(
      BooleanFormula pSlice,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr, CFANode pNode) {
    slice = pSlice;
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    fmgr = pFmgr;
    node = pNode;
  }

  public static SlicingAbstractedState of(BooleanFormula pSlice,
      SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      CFANode pNode) {
    return new SlicingAbstractedState(pSlice, pSsaMap, pPointerTargetSet, pFmgr,
        pNode);
  }

  public SSAMap getSSA() {
    return ssaMap;
  }

  public PointerTargetSet getPointerTargetSet() {
    return pointerTargetSet;
  }

  public CFANode getNode() {
    return node;
  }

  public BooleanFormula getAbstraction() {
    return slice;
  }

  public static SlicingAbstractedState empty(
      FormulaManagerView pFmgr,
      CFANode startingNode
  ) {
    return SlicingAbstractedState.of(
        pFmgr.getBooleanFormulaManager().makeBoolean(true),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pFmgr, startingNode);
  }

  @Override
  public boolean isAbstracted() {
    return true;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager,
      PathFormulaManager pfmgr) {
    return manager.parse(fmgr.dumpFormula(slice).toString());
  }

  @Override
  public String toDOTLabel() {
    return slice.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * Dummy state for signalling that the processed edge comes from within the
   * loop and no slicing is necessary.
   */
  static class SubsumedSlicingState extends SlicingState {
    private final SlicingIntermediateState wrapped;

    private SubsumedSlicingState(SlicingIntermediateState pWrapped) {
      wrapped = pWrapped;
    }

    public SlicingIntermediateState getWrapped() {
      return wrapped;
    }

    public static SubsumedSlicingState of(SlicingIntermediateState wrapped) {
      return new SubsumedSlicingState(wrapped);
    }

    @Override
    public boolean isAbstracted() {
      return true;
    }
  }
}
