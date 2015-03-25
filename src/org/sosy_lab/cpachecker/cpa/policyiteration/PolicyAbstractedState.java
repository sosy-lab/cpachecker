package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

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
   * State used to generate the abstraction.
   */
  private final PolicyIntermediateState generatingState;

  private transient Optional<PolicyAbstractedState> newVersion =
      Optional.absent();

  private static final UniqueIdGenerator uuidGenerator = new UniqueIdGenerator();
  private final int uniqueID;

  // the same version on nodes might be just good enough.
  private static final Multiset<CFANode> updateCounter = HashMultiset.create();

  private PolicyAbstractedState(CFANode node,
      Set<Template> pTemplates,
      Map<Template, PolicyBound> pAbstraction,
      PolicyIntermediateState pGeneratingState) {
    super(pTemplates, node);

    updateCounter.add(node);
    abstraction = ImmutableMap.copyOf(pAbstraction);
    generatingState = pGeneratingState;
    uniqueID = uuidGenerator.getFreshId();
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public static ImmutableMultiset<CFANode> getUpdateCounter() {
    return ImmutableMultiset.copyOf(updateCounter);
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
    for (PolicyAbstractedState visited : toUpdate) {
      visited.newVersion = Optional.of(latest);
    }
    return latest;
  }

  public static PolicyAbstractedState of(
      Map<Template, PolicyBound> data,
      Set<Template> templates,
      CFANode node,
      PolicyIntermediateState pGeneratingState
  ) {
    return new PolicyAbstractedState(node, templates, data, pGeneratingState);
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
        getNode(), newTemplates, builder.build(),  generatingState
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
        ImmutableSet.<Template>of(),
        initial,  ImmutableSet.<PolicyAbstractedState>of()
    );
    return PolicyAbstractedState.of(
        ImmutableMap.<Template, PolicyBound>of(), // abstraction
        ImmutableSet.<Template>of(), // templates
        node, // node
        initialState // generating state
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
    return String.format("(id=%s)%s", uniqueID, abstraction);
  }

  @Override
  public Iterator<Entry<Template, PolicyBound>> iterator() {
    return abstraction.entrySet().iterator();
  }
}
