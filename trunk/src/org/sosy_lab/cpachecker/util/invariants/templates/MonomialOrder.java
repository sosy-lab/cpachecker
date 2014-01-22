package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Comparator;

public interface MonomialOrder extends Comparator<TemplateTerm> {

  @Override
  public int compare(TemplateTerm t1, TemplateTerm t2);

}
