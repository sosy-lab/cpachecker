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

import java.util.HashSet;
import java.util.Objects;
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

  /**
   * Under which paths this state is inductive.
   */
  private final ImmutableSet<PathFormulaWithStartSSA> inductiveUnder;

  private SlicingAbstractedState(
      Set<BooleanFormula> pSlice,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      Optional<SlicingIntermediateState> pGeneratingState,
      CFANode pNode,
      Iterable<PathFormulaWithStartSSA> pInductiveUnder) {
    inductiveUnder = ImmutableSet.copyOf(pInductiveUnder);
    semiClauses = ImmutableSet.copyOf(pSlice);
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    fmgr = pFmgr;
    generatingState = pGeneratingState;
    node = pNode;
  }

  public Set<PathFormulaWithStartSSA> getInductiveUnder() {
    return inductiveUnder;
  }

  public static SlicingAbstractedState ofClauses(Set<BooleanFormula> pSlice,
                                          SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
                                          FormulaManagerView pFmgr,
                                          CFANode pNode,
                                          Optional<SlicingIntermediateState> pGeneratingState) {
    return new SlicingAbstractedState(
        pSlice, pSsaMap, pPointerTargetSet, pFmgr,
        pGeneratingState, pNode,
        ImmutableSet.<PathFormulaWithStartSSA>of());
  }

  public static SlicingAbstractedState makeSliced(Set<BooleanFormula> pSlice,
                                          SSAMap pSsaMap, PointerTargetSet pPointerTargetSet,
                                          FormulaManagerView pFmgr,
                                          CFANode pNode,
                                          Optional<SlicingIntermediateState> pGeneratingState,
                                          Iterable<PathFormulaWithStartSSA> trace) {
    return new SlicingAbstractedState(pSlice, pSsaMap, pPointerTargetSet, pFmgr,
        pGeneratingState, pNode, trace);
  }

  public static SlicingAbstractedState copyOf(SlicingAbstractedState sliced) {
    return new SlicingAbstractedState(
        sliced.semiClauses,
        sliced.ssaMap,
        sliced.pointerTargetSet,
        sliced.fmgr,
        sliced.generatingState,
        sliced.node,
        sliced.inductiveUnder);
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

  public Set<BooleanFormula> getInstantiatedAbstraction() {
    Set<BooleanFormula> out = new HashSet<>(semiClauses.size());
    for (BooleanFormula f : semiClauses) {
      out.add(fmgr.instantiate(f, ssaMap));
    }
    return out;
  }

  public static SlicingAbstractedState empty(
      FormulaManagerView pFmgr,
      CFANode startingNode
  ) {
    return new SlicingAbstractedState(
        ImmutableSet.<BooleanFormula>of(),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pFmgr,
        Optional.<SlicingIntermediateState>absent(),
        startingNode,
        ImmutableSet.<PathFormulaWithStartSSA>of());
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    SlicingAbstractedState that = (SlicingAbstractedState) pO;
    return Objects.equals(semiClauses, that.semiClauses) &&
        Objects.equals(ssaMap, that.ssaMap) &&
        Objects.equals(pointerTargetSet, that.pointerTargetSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(semiClauses, ssaMap, pointerTargetSet);
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

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return semiClauses.toString();
  }
}
