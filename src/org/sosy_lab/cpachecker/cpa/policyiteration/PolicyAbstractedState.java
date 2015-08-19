package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>>, FormulaReportingState {

  private final CongruenceState congruence;

  private final StateFormulaConversionManager manager;

  /**
   * Finite bounds for templates.
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
   * *NOT* used in comparison.
   */
  private final BooleanFormula extraInvariant;

  /**
   * Pointer to the latest version of the state associated with the given
   * location.
   */
  private transient Optional<PolicyAbstractedState> newVersion =
      Optional.absent();

  /**
   * If state A and state B can potentially get merged, they share the same
   * location.
   */
  private final int locationID;

  private PolicyAbstractedState(CFANode node,
      Map<Template, PolicyBound> pAbstraction,
      CongruenceState pCongruence,
      int pLocationID,
      StateFormulaConversionManager pManager, SSAMap pSsaMap,
      PointerTargetSet pPointerTargetSet, BooleanFormula pPredicate) {
    super(node);
    ssaMap = pSsaMap;
    pointerTargetSet = pPointerTargetSet;
    extraInvariant = pPredicate;
    abstraction = ImmutableMap.copyOf(pAbstraction);
    congruence = pCongruence;
    locationID = pLocationID;
    manager = pManager;
  }

  public int getLocationID() {
    return locationID;
  }

  public CongruenceState getCongruence() {
    return congruence;
  }

  public void setNewVersion(PolicyAbstractedState pNewVersion) {
    newVersion = Optional.of(pNewVersion);
  }

  /**
   * @return latest version of this state found in the reached set.
   * Only used in {@code joinOnMerge} configuration.
   */
  public PolicyAbstractedState getLatestVersion() {
    PolicyAbstractedState latest = this;
    List<PolicyAbstractedState> toUpdate = new ArrayList<>();

    // Traverse the pointers up.
    while (latest.newVersion.isPresent()) {
      toUpdate.add(latest);
      latest = latest.newVersion.get();
    }

    // Update the pointers on the visited states.
    for (PolicyAbstractedState updated : toUpdate) {
      updated.newVersion = Optional.of(latest);
    }
    return latest;
  }

  public static PolicyAbstractedState of(
      Map<Template, PolicyBound> data,
      CFANode node,
      CongruenceState pCongruence,
      int pLocationID,
      StateFormulaConversionManager pManager,
      SSAMap pSSAMap,
      PointerTargetSet pPointerTargetSet,
      BooleanFormula pPredicate
  ) {
    return new PolicyAbstractedState(node, data,
        pCongruence, pLocationID, pManager, pSSAMap,
        pPointerTargetSet, pPredicate);
  }

  public PolicyAbstractedState updateAbstraction(
      Map<Template, PolicyBound> newAbstraction) {
    return new PolicyAbstractedState(getNode(),
        newAbstraction, congruence, locationID, manager, ssaMap,
        pointerTargetSet, extraInvariant);
  }

  public ImmutableMap<Template, PolicyBound> getAbstraction() {
    return abstraction;
  }

  public BooleanFormula getExtraInvariant() {
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
  public Optional<PolicyBound> getBound(Template e) {
    return Optional.fromNullable(abstraction.get(e));
  }

  /**
   * @return Empty abstracted state associated with {@code node}.
   */
  public static PolicyAbstractedState empty(CFANode node,
      SSAMap pSSAMap,
      PointerTargetSet pPointerTargetSet,
      BooleanFormula pPredicate,
      StateFormulaConversionManager pManager) {
    return PolicyAbstractedState.of(
        ImmutableMap.<Template, PolicyBound>of(), // abstraction
        node, // node
        CongruenceState.empty(),
        -1,
        pManager,
        pSSAMap,
        pPointerTargetSet,
        pPredicate
    );
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
        "(node=%s)%s%nExtra Invariant: %s %n",
        getNode(),
        (new PolicyDotWriter()).toDOTLabel(abstraction),
        extraInvariant
    );
  }

  @Override
  public boolean shouldBeHighlighted() {
    return true;
  }

  @Override
  public String toString() {
    return String.format("(loc=%s)%s", locationID, abstraction);
  }

  @Override
  public Iterator<Entry<Template, PolicyBound>> iterator() {
    return abstraction.entrySet().iterator();
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView fmgr, PathFormulaManager pfmgr) {
    BooleanFormula invariant;
    try {
      invariant = fmgr.getBooleanFormulaManager().and(
          manager.abstractStateToConstraints(fmgr, pfmgr, this, false)
      );
    } catch (CPAException e) {
      throw new UnsupportedOperationException(
          "The invariant generation exception should never be "
              + "encountered on this code path.");
    }
    return fmgr.uninstantiate(invariant);
  }
}
