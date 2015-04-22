package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>>, FormulaReportingState {

  private final CongruenceState congruence;

  private final PolicyIterationManager manager;

  /**
   * Finite bounds for templates.
   */
  private final ImmutableMap<Template, PolicyBound> abstraction;

  /**
   * State used to generate the abstraction.
   */
  private final PolicyIntermediateState generatingState;

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
      PolicyIntermediateState pGeneratingState,
      CongruenceState pCongruence,
      int pLocationID,
      PolicyIterationManager pManager) {
    super(node);
    abstraction = ImmutableMap.copyOf(pAbstraction);
    generatingState = pGeneratingState;
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
      PolicyIntermediateState pGeneratingState,
      CongruenceState pCongruence,
      int pLocationID,
      PolicyIterationManager pManager
  ) {
    return new PolicyAbstractedState(node, data, pGeneratingState,
        pCongruence, pLocationID, pManager);
  }

  public PolicyAbstractedState withUpdates(
      Map<Template, PolicyBound> updates,
      Set<Template> unbounded,
      CongruenceState newCongruence) {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();

    // We only iterate over the existing templates, because if the value was
    // unbounded at some point, it stays unbounded.
    for (Entry<Template, PolicyBound> entry : abstraction.entrySet()) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      if (unbounded.contains(template)) {
        continue;
      }
      if (updates.containsKey(template)) {
        bound = updates.get(template);
      }
      builder.put(template, bound);
    }
    return new PolicyAbstractedState(
        getNode(), builder.build(),  generatingState,
        newCongruence, locationID, manager
    );
  }

  public PathFormula getPathFormula() {
    return generatingState.getPathFormula();
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
      PathFormula initial, PolicyIterationManager pManager) {
    PolicyIntermediateState initialState = PolicyIntermediateState.of(
        node,
        initial,  ImmutableMap.<Integer, PolicyAbstractedState>of()
    );
    return PolicyAbstractedState.of(
        ImmutableMap.<Template, PolicyBound>of(), // abstraction
        node, // node
        initialState, // generating state
        CongruenceState.empty(),
        -1,
        pManager
    );
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "(node=%s)%s%n %n %s %n",
        getNode(),
        (new PolicyDotWriter()).toDOTLabel(abstraction),
        generatingState.getPathFormula()
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
    return fmgr.getBooleanFormulaManager().and(
        manager.abstractStateToConstraints(fmgr, pfmgr, this)
    );
  }
}
