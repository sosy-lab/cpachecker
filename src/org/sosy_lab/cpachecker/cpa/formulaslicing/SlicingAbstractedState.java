package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Set;

/**
 * Abstracted state, containing invariants obtained by slicing.
 *
 * The invariant is universally true wrt the intermediate state
 * which was used for the abstraction.
 */
public class SlicingAbstractedState
    extends SlicingState implements FormulaReportingState, Graphable {

  /**
   * Slice with respect to the current loop.
   * The slice does not contain any intermediate variables, and thus can be
   * represented as an *un-instantiated* formula.
   */
  private final ImmutableSet<BooleanFormula> semiClauses;

  /**
   * Expected starting {@link PointerTargetSet} and {@link SSAMap}.
   */
  private final SSAMap ssaMap;
  private final PointerTargetSet pointerTargetSet;

  /**
   * Backpointer to the {@link FormulaManagerView} corresponding to this CPA, used for dumping
   * the formulas.
   */
  private final FormulaManagerView fmgr;

  /**
   * The intermediate state used to generate the inductive weakening,
   * empty for the initial state.
   */
  private final Optional<SlicingIntermediateState> generatingState;

  private final CFANode node;

  private final boolean isSliced;

  private SlicingAbstractedState(
      Set<BooleanFormula> pSlice,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      Optional<SlicingIntermediateState> pGeneratingState,
      CFANode pNode,
      boolean pIsSliced) {
    semiClauses = ImmutableSet.copyOf(pSlice);
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    fmgr = pFmgr;
    generatingState = pGeneratingState;
    node = pNode;
    isSliced = pIsSliced;
  }

  public static SlicingAbstractedState of(Set<BooleanFormula> pSlice,
      SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      CFANode pNode,
      Optional<SlicingIntermediateState> pGeneratingState, boolean pIsSliced) {
    return new SlicingAbstractedState(pSlice, pSsaMap, pPointerTargetSet, pFmgr,
        pGeneratingState, pNode, pIsSliced);
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

  public boolean isInitial() {
    return !generatingState.isPresent();
  }

  public CFANode getNode() {
    return node;
  }

  public Set<BooleanFormula> getAbstraction() {
    return semiClauses;
  }

  public static SlicingAbstractedState empty(
      FormulaManagerView pFmgr,
      CFANode startingNode
  ) {
    return SlicingAbstractedState.of(
        ImmutableSet.<BooleanFormula>of(),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pFmgr, startingNode,
        Optional.<SlicingIntermediateState>absent(),
        false);
  }

  @Override
  public boolean isAbstracted() {
    return true;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager,
      PathFormulaManager pfmgr) {
    return manager.parse(
        fmgr.dumpFormula(
            fmgr.getBooleanFormulaManager().and(semiClauses)
        ).toString());
  }

  @Override
  public String toDOTLabel() {
    return Joiner.on("\n---\n").join(semiClauses);
  }

  public boolean isSliced() {
    return isSliced;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return isSliced;
  }

  @Override
  public String toString() {
    return semiClauses.toString();
  }
}
