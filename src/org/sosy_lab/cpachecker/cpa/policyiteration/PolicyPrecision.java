package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.collect.ImmutableSet;

/**
 * Policy iteration precision is simply a set of templates.
 */
public class PolicyPrecision implements Precision, Iterable<Template> {
  private final ImmutableSet<Template> templates;

  public PolicyPrecision(Set<Template> pTemplates) {
    templates = ImmutableSet.copyOf(pTemplates);
  }

  public static PolicyPrecision empty() {
    return new PolicyPrecision(ImmutableSet.<Template>of());
  }

  @Override
  public Iterator<Template> iterator() {
    return templates.iterator();
  }
}
