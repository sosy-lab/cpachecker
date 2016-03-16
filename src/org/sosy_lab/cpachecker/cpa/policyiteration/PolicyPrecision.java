package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.Iterator;
import java.util.Set;

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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PolicyPrecision)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    PolicyPrecision other = (PolicyPrecision) o;
    return other.templates.equals(templates);
  }

  @Override
  public int hashCode() {
    return templates.hashCode();
  }
}
