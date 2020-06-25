package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Abstracted state, containing invariants obtained by slicing.
 *
 * The invariant is universally true wrt the intermediate state
 * which was used for the abstraction.
 */
class SlicingAbstractedState
    extends SlicingState implements FormulaReportingState, Graphable {

  /**
   * Uninstantiated set of lemmas.
   */
  private final ImmutableSet<BooleanFormula> lemmas;

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

  private transient int hashCache = 0;

  private SlicingAbstractedState(
      Set<BooleanFormula> pSlice,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      FormulaManagerView pFmgr,
      Optional<SlicingIntermediateState> pGeneratingState,
      CFANode pNode,
      Iterable<PathFormulaWithStartSSA> pInductiveUnder) {
    inductiveUnder = ImmutableSet.copyOf(pInductiveUnder);
    lemmas = ImmutableSet.copyOf(pSlice);
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
        ImmutableSet.of());
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
        sliced.lemmas,
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
    return lemmas;
  }

  public Set<BooleanFormula> getInstantiatedAbstraction() {
    Set<BooleanFormula> out = new HashSet<>(lemmas.size());
    for (BooleanFormula f : lemmas) {
      out.add(fmgr.instantiate(f, ssaMap));
    }
    return out;
  }

  public static SlicingAbstractedState empty(
      FormulaManagerView pFmgr,
      CFANode startingNode
  ) {
    return new SlicingAbstractedState(
        ImmutableSet.of(),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pFmgr,
        Optional.empty(),
        startingNode,
        ImmutableSet.of());
  }


  @Override
  public boolean isAbstracted() {
    return true;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    BooleanFormula constraint = fmgr.getBooleanFormulaManager().and(lemmas);
    return manager.translateFrom(constraint, fmgr);
  }

  @Override
  public String toDOTLabel() {
    return Joiner.on("\n---\n").join(lemmas);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return lemmas.toString();
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
    return Objects.equals(lemmas, that.lemmas) &&
        Objects.equals(ssaMap, that.ssaMap) &&
        Objects.equals(pointerTargetSet, that.pointerTargetSet);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(lemmas, ssaMap, pointerTargetSet);
    }
    return hashCache;
  }
}
