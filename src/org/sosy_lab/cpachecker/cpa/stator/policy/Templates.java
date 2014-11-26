package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Iterator;

import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.collect.ImmutableSet;

public class Templates implements Iterable<LinearExpression> {
  private final ImmutableSet<LinearExpression> templates;

  Templates(ImmutableSet<LinearExpression> pTemplates) {
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

  @Override
  public String toString() {
    return templates.toString();
  }
}
