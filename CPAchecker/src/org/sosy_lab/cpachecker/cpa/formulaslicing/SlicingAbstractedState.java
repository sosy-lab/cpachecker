package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Optional;

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

  /**
   * The intermediate state used to generate the inductive weakening,
   * empty for the initial state.
   */
  private final Optional<SlicingIntermediateState> generatingState;

  private final CFANode node;

  private SlicingAbstractedState(
      BooleanFormula pSlice,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      Optional<SlicingIntermediateState> pGeneratingState, CFANode pNode) {
    slice = pSlice;
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    fmgr = pFmgr;
    generatingState = pGeneratingState;
    node = pNode;
  }

  public static SlicingAbstractedState of(BooleanFormula pSlice,
      SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      CFANode pNode,
      Optional<SlicingIntermediateState> pGeneratingState) {
    return new SlicingAbstractedState(pSlice, pSsaMap, pPointerTargetSet, pFmgr,
        pGeneratingState, pNode);
  }

  public Optional<SlicingIntermediateState> getGeneratingState(){
    return generatingState;
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
        pFmgr, startingNode,
        Optional.<SlicingIntermediateState>absent());
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
}
