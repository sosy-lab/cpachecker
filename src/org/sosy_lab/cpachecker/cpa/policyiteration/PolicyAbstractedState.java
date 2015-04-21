package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>> {

  final CongruenceState congruence;

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
      int pLocationID) {
    super(node);
    abstraction = ImmutableMap.copyOf(pAbstraction);
    generatingState = pGeneratingState;
    congruence = pCongruence;
    locationID = pLocationID;
  }

  public int getLocationID() {
    return locationID;
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
      int pLocationID
  ) {
    return new PolicyAbstractedState(node, data, pGeneratingState,
        pCongruence, pLocationID);
  }

  public PolicyAbstractedState withUpdates(
      Map<Template, PolicyBound> updates,
      Set<Template> unbounded,
      CongruenceState newCongruence) {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();

    for (Template template : abstraction.keySet()) {
      if (unbounded.contains(template)) {
        continue;
      }
      if (updates.containsKey(template)) {
        builder.put(template, updates.get(template));
      } else {
        PolicyBound v = abstraction.get(template);
        if (v != null) {
          builder.put(template, abstraction.get(template));
        }
      }
    }
    return new PolicyAbstractedState(
        getNode(), builder.build(),  generatingState,
        newCongruence, locationID
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
      PathFormula initial) {
    PolicyIntermediateState initialState = PolicyIntermediateState.of(
        node,
        initial,  ImmutableMap.<Integer, PolicyAbstractedState>of()
    );
    return PolicyAbstractedState.of(
        ImmutableMap.<Template, PolicyBound>of(), // abstraction
        node, // node
        initialState, // generating state
        CongruenceState.empty(),
        -1
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
}
