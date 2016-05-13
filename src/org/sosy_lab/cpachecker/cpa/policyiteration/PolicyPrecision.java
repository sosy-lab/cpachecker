package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * Policy iteration precision is simply a set of templates.
 */
public class PolicyPrecision implements Precision, Iterable<Template> {
  private final ImmutableList<Template> templates;

  public PolicyPrecision(Set<Template> pTemplates) {
    ArrayList<Template> t = new ArrayList<>(pTemplates);
    Collections.sort(t, new Comparator<Template>() {
      @Override
      public int compare(
          Template o1, Template o2) {
        return ComparisonChain.start()
            .compare(o1.getLinearExpression().size(),
                o2.getLinearExpression().size())
            .compare(o1.toString(), o2.toString()).result();
      }
    });
    templates = ImmutableList.copyOf(t);
  }

  public static PolicyPrecision empty() {
    return new PolicyPrecision(ImmutableSet.<Template>of());
  }

  /**
   * Iterator over contained templates, in sorted order.
   */
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
