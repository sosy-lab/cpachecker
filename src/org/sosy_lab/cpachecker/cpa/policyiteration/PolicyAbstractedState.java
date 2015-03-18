package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>> {
  /**
   * Finite bounds for templates.
   */
  private final ImmutableMap<Template, PolicyBound> abstraction;

  /**
   * Non-abstracted section of the formula.
   *
   * NOTE: contains {@code PointerTargetSet} inside.
   */

  /**
   * State used to generate the abstraction.
   */
  private final PolicyIntermediateState generatingState;

  /**
   * Version per location. Starts from zero, incremented with each update
   * to the location.
   */
  private final int version;
  private int hashCache = 0;

  private static final Multiset<Location> updateCounter = HashMultiset.create();

  private PolicyAbstractedState(Location pLocation,
      Set<Template> pTemplates,
      Map<Template, PolicyBound> pAbstraction,
      PolicyIntermediateState pGeneratingState) {
    super(pLocation, pTemplates);

    version = updateCounter.count(pLocation);
    updateCounter.add(pLocation);

    abstraction = ImmutableMap.copyOf(pAbstraction);
    generatingState = pGeneratingState;
  }

  public static ImmutableMultiset<Location> getUpdateCounter() {
    return ImmutableMultiset.copyOf(updateCounter);
  }

  public int getVersion() {
    return version;
  }

  public static PolicyAbstractedState of(
      Map<Template, PolicyBound> data,
      Set<Template> templates,
      Location pLocation,
      PolicyIntermediateState pGeneratingState
  ) {
    return new PolicyAbstractedState(pLocation, templates, data, pGeneratingState);
  }

  public PolicyAbstractedState withUpdates(
      Map<Template, PolicyBound> updates,
      Set<Template> unbounded,
      Set<Template> newTemplates) {

    ImmutableMap.Builder<Template, PolicyBound> builder =
        ImmutableMap.builder();

    for (Template template : newTemplates) {
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
        getLocation(), newTemplates, builder.build(),  generatingState
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
  public static PolicyAbstractedState empty(Location pLocation,
      PathFormula initial) {
    PolicyIntermediateState Iinitial = PolicyIntermediateState.of(
        pLocation, ImmutableSet.<Template>of(),
        initial,  ImmutableMap.<Location, PolicyAbstractedState>of()
    );
    return PolicyAbstractedState.of(
        ImmutableMap.<Template, PolicyBound>of(), // abstraction
        ImmutableSet.<Template>of(), // templates
        pLocation, // node
        Iinitial // generating state
    );
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "(v=%s, loc=%s)%s%n %n %s %n",
        version, getLocation().toID(),
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
    return String.format("%s(%s): %s", getLocation(), version, abstraction);
  }

  @Override
  public Iterator<Entry<Template, PolicyBound>> iterator() {
    return abstraction.entrySet().iterator();
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(
          generatingState,
          abstraction,
          super.hashCode());
    }
    return hashCache;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyAbstractedState other = (PolicyAbstractedState)o;
    return (
        generatingState.equals(other.generatingState) &&
        abstraction.equals(other.abstraction) && super.equals(o));
  }
}
