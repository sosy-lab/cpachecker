package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>>,
                 FormulaReportingState {

  private final StateFormulaConversionManager manager;

  /**
   * Finite bounds for templates: binds each template from above.
   */
  private final ImmutableMap<Template, PolicyBound> abstraction;

  /**
   * Expected starting {@link PointerTargetSet} and {@link SSAMap}.
   */
  private final SSAMap ssaMap;
  private final PointerTargetSet pointerTargetSet;

  /**
   * Uninstantiated invariant associated with a state,
   * derived from other analyses.
   */
  private final BooleanFormula extraInvariant;

  /**
   * Intermediate state used to generate this abstraction,
   * empty only for the initial state.
   */
  private final @Nullable PolicyIntermediateState generator;

  /**
   * If state A and state B can potentially get merged, they share the same
   * location.
   */
  private final int locationID;

  /**
   * A pointer to the sibling state.
   *
   * <p>Only exists for states on which value determination was just performed.
   */
  private final transient @Nullable PolicyAbstractedState sibling;

  private transient int hashCache = 0;

  private final static UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId;

  /**
   * Pointer to the unmodified state, used only for summary generation.
   */
  private final @Nullable PolicyAbstractedState original;

  private PolicyAbstractedState(
      CFANode node,
      Map<Template, PolicyBound> pAbstraction,
      int pLocationID,
      StateFormulaConversionManager pManager,
      SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet,
      BooleanFormula pPredicate,
      PolicyIntermediateState pGenerator,
      PolicyAbstractedState pSibling,
      PolicyAbstractedState pOriginal) {
    super(node);
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    extraInvariant = pPredicate;
    generator = pGenerator;
    abstraction = ImmutableMap.copyOf(pAbstraction);
    locationID = pLocationID;
    manager = pManager;
    sibling = pSibling;
    original = pOriginal;
    stateId = idGenerator.getFreshId();
  }

  int getStateId() {
    return stateId;
  }

  StateFormulaConversionManager getManager() {
    return manager;
  }

  public Optional<PolicyAbstractedState> getSibling() {
    return Optional.ofNullable(sibling);
  }

  int getLocationID() {
    return locationID;
  }

  public static PolicyAbstractedState of(
      Map<Template, PolicyBound> data,
      CFANode node,
      int pLocationID,
      StateFormulaConversionManager pManager,
      SSAMap pSSAMap,
      PointerTargetSet pPointerTargetSet,
      BooleanFormula pPredicate,
      Optional<PolicyIntermediateState> pPredecessor,
      Optional<PolicyAbstractedState> pSibling) {
    return new PolicyAbstractedState(
        node,
        data,
        pLocationID,
        pManager,
        pSSAMap,
        pPointerTargetSet,
        pPredicate,
        pPredecessor.orElse(null),
        pSibling.orElse(null),
        null);
  }

  /**
   * Replace the abstraction with the given input.
   */
  PolicyAbstractedState withNewAbstraction(
      Map<Template, PolicyBound> newAbstraction) {
    return new PolicyAbstractedState(
        getNode(),
        newAbstraction,
        locationID,
        manager,
        ssaMap,
        pointerTargetSet,
        extraInvariant,
        generator,
        sibling,
        null);
  }

  /**
   * Replace the abstraction and SSA map with the given input.
   */
  PolicyAbstractedState withNewSSA(SSAMap pSsaMap) {
    return new PolicyAbstractedState(
        getNode(),
        abstraction,
        locationID,
        manager,
        pSsaMap,
        pointerTargetSet,
        extraInvariant,
        generator,
        sibling,
        this);
  }

  public ImmutableMap<Template, PolicyBound> getAbstraction() {
    return abstraction;
  }

  BooleanFormula getExtraInvariant() {
    return extraInvariant;
  }

  public SSAMap getSSA() {
    return ssaMap;
  }

  public PointerTargetSet getPointerTargetSet() {
    return pointerTargetSet;
  }

  /**
   * @return {@link PolicyBound} for the given {@link Template}
   * <code>e</code> or an empty optional if it is unbounded.
   */
  Optional<PolicyBound> getBound(Template e) {
    return Optional.ofNullable(abstraction.get(e));
  }

  /**
   * Create a TOP state with empty abstraction.
   */
  public static PolicyAbstractedState top(
      CFANode node,
      int pLocationID,
      StateFormulaConversionManager pManager,
      SSAMap pSSAMap,
      PointerTargetSet pPointerTargetSet,
      BooleanFormula pPredicate,
      PolicyIntermediateState pPredecessor,
      Optional<PolicyAbstractedState> pSibling) {
    return new PolicyAbstractedState(
        node,
        ImmutableMap.of(),
        pLocationID,
        pManager,
        pSSAMap,
        pPointerTargetSet,
        pPredicate,
        pPredecessor,
        pSibling.orElse(null),
        null);
  }

  /**
   * @return Empty abstracted state associated with {@code node}.
   */
  public static PolicyAbstractedState empty(CFANode node,
                                            BooleanFormula pPredicate,
                                            StateFormulaConversionManager pManager) {
    return new PolicyAbstractedState(
        node, // node
        ImmutableMap.of(), // abstraction
        -1,
        pManager,
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        pPredicate,
        null,
        null,
        null);
  }

  public int size() {
    return abstraction.size();
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "(node=%s, locID=%s, ssa=%s)%s%n",
        getNode(), locationID, ssaMap, manager.toDOTLabel(abstraction)
    );
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("(loc=%s, node=%s)%s", locationID, getNode(),
        abstraction);
  }

  @Override
  public Iterator<Entry<Template, PolicyBound>> iterator() {
    return abstraction.entrySet().iterator();
  }

  Optional<PolicyIntermediateState> getGeneratingState() {
    return Optional.ofNullable(generator);
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView fmgr) {
    return fmgr.uninstantiate(fmgr.getBooleanFormulaManager().and(
        manager.abstractStateToConstraints(fmgr, this, false)
    ));
  }

  /**
   * During summary generation, a copy state may be created just for the sake
   * of assigning a new {@link SSAMap}.
   * In such a case we still need a pointer to the original state,
   * which is contained in a {@link org.sosy_lab.cpachecker.core.reachedset.ReachedSet}.
   * This functionality might be removed when LPI no longer relies on
   * {@code ReachedSet} to find "sibling" states.
   */
  PolicyAbstractedState getOriginal() {
    return original == null ? this : original;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof PolicyAbstractedState)) {
      return false;
    }
    PolicyAbstractedState entries = (PolicyAbstractedState) pO;
    return Objects.equals(abstraction, entries.abstraction) &&
        Objects.equals(ssaMap, entries.ssaMap) &&
        Objects.equals(pointerTargetSet, entries.pointerTargetSet) &&
        Objects.equals(extraInvariant, entries.extraInvariant) &&
        Objects.equals(getNode(), entries.getNode());
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(
          getNode(),
          abstraction,
          ssaMap,
          pointerTargetSet,
          extraInvariant);
    }
    return hashCache;
  }
}
