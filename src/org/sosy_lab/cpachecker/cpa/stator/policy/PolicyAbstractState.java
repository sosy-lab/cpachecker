package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Abstract state for policy iteration: bounds on each expression (from the template),
 * for the given control node.
 *
 * Logic-less container class.
 */
public class PolicyAbstractState implements AbstractState,
    Iterable<Entry<LinearExpression, PolicyTemplateBound>>,
    Graphable {

  public static class Templates implements Iterable<LinearExpression> {
    private final ImmutableSet<LinearExpression> templates;

    private Templates(ImmutableSet<LinearExpression> pTemplates) {
      templates = pTemplates;
    }

    static Templates empty() {
      return new Templates(ImmutableSet.<LinearExpression>of());
    }

    Templates withTemplates(Iterable<LinearExpression> newTemplates) {
      return new Templates(
          ImmutableSet.<LinearExpression>builder()
              .addAll(templates)
              .addAll(newTemplates)
              .build()
      );
    }

    Templates merge(Templates other) {
      return new Templates(
          ImmutableSet.<LinearExpression>builder()
            .addAll(templates).addAll(other.templates).build());
    }

    @Override
    public Iterator<LinearExpression> iterator() {
      return templates.iterator();
    }

    public String toString() {
      return templates.toString();
    }
  }

  // NOTE: It should not be there, we are wasting memory.
  // But hey, can't really find an easier way to do that.
  private final CFANode node;

  /**
   * Finite bounds for templates.
   */
  private final ImmutableMap<LinearExpression, PolicyTemplateBound> data;

  /**
   * Templates tracked.
   */
  private final Templates templates;

  /**
   * @return {@link PolicyTemplateBound} for the given {@link LinearExpression}
   * or an empty optional if it is unbounded.
   */
  public Optional<PolicyTemplateBound> getBound(LinearExpression e) {
    return Optional.fromNullable(data.get(e));
  }

  public Templates getTemplates() {
    return templates;
  }

  public CFANode getNode() {
    return node;
  }

  private PolicyAbstractState(
      ImmutableMap<LinearExpression, PolicyTemplateBound> pData,
      Templates pTemplates,
      CFANode pNode) {
    templates = pTemplates;
    data = pData;
    node = pNode;
  }


  public static PolicyAbstractState withState(
      ImmutableMap<LinearExpression, PolicyTemplateBound> data,
      Templates templates,
      CFANode node
  ) {
    return new PolicyAbstractState(data, templates, node);
  }

  public static PolicyAbstractState withEmptyState(CFANode node) {
    return new PolicyAbstractState(
        ImmutableMap.<LinearExpression, PolicyTemplateBound>of(),
        Templates.empty(),
        node
    );
  }

  @Override
  public Iterator<Entry<LinearExpression, PolicyTemplateBound>> iterator() {
    return data.entrySet().iterator();
  }

  @Override
  public String toString() {
    if (data == null) return "BOTTOM";
    return String.format("%s: %s", node, data);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder b = new StringBuilder();
    b.append("\n");

    // TODO: might want to do it as a pre-processing rather than
    // only for printing.

    // Convert to the readable format.
    Map<LinearExpression, PolicyTemplateBound> toSort = new HashMap<>();

    // Pretty-printing is tricky.
    Map<LinearExpression, ExtendedRational> lessThan = new HashMap<>();
    Map<LinearExpression, Pair<ExtendedRational, ExtendedRational>> bounded
        = new HashMap<>();
    Map<LinearExpression, ExtendedRational> equal = new HashMap<>();

    toSort.putAll(data);
    while (toSort.size() > 0) {
      LinearExpression template, negTemplate;
      template = toSort.keySet().iterator().next();
      ExtendedRational upperBound = toSort.get(template).bound;

      negTemplate = template.negate();

      toSort.remove(template);

      if (toSort.containsKey(negTemplate)) {
        ExtendedRational lowerBound = toSort.get(negTemplate).bound.negate();
        toSort.remove(negTemplate);

        // Rotate the pair if necessary.
        boolean negated = false;
        if (template.toString().startsWith("-")) {
          negated = true;
        }

        if (lowerBound.equals(upperBound)) {
          if (negated) {
            equal.put(template.negate(), lowerBound.negate());
          } else {
            equal.put(template, lowerBound);
          }
        } else {
          if (negated) {
            bounded.put(
                template.negate(), Pair.of(upperBound.negate(), lowerBound.negate()));
          } else {
            bounded.put(
                template, Pair.of(lowerBound, upperBound));
          }
        }
      } else {
        lessThan.put(template, upperBound);
      }
    }

    // Print equals.
    for (Entry<LinearExpression, ExtendedRational> entry : equal.entrySet()) {
      b.append(entry.getKey())
          .append("=")
          .append(entry.getValue())
          .append("\n");
    }

    // Print bounded.
    for (Entry<LinearExpression, Pair<ExtendedRational, ExtendedRational>> entry
        : bounded.entrySet()) {
      b
          .append(entry.getValue().getFirst())
          .append("≤")
          .append(entry.getKey())
          .append("≤")
          .append(entry.getValue().getSecond())
          .append("\n");
    }

    // Print less-than.
    for (Entry<LinearExpression, ExtendedRational> entry : lessThan.entrySet()) {
      b.append(entry.getKey())
          .append("≤")
          .append(entry.getValue())
          .append("\n");
    }

    return b.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyAbstractState other = (PolicyAbstractState)o;
    return (data.equals(other.data) && node.equals(other.node));
  }

  @Override
  public int hashCode() {
    int result = node.hashCode();
    result = 31 * result + (data != null ? data.hashCode() : 0);
    return result;
  }

  public PolicyAbstractState withUpdates(
      Map<LinearExpression, PolicyTemplateBound> updates,
      Set<LinearExpression> unbounded,
      Templates newTemplates) {

    ImmutableSet<LinearExpression> allTemplates = ImmutableSet.<LinearExpression>
        builder().addAll(updates.keySet()).addAll(data.keySet()).build();

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> builder =
        ImmutableMap.builder();

    for (LinearExpression template : allTemplates) {
      if (unbounded.contains(template)) {
        continue;
      }
      if (updates.containsKey(template)) {
        builder.put(template, updates.get(template));
      } else {
        builder.put(template, data.get(template));
      }
    }
    return new PolicyAbstractState(builder.build(), newTemplates, node);
  }
}
