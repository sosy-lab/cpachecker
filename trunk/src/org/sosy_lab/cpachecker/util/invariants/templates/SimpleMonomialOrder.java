package org.sosy_lab.cpachecker.util.invariants.templates;

public class SimpleMonomialOrder implements MonomialOrder {

  @Override
  public int compare(TemplateTerm t1, TemplateTerm t2) {
    String s1 = t1.getMonomialString(VariableWriteMode.PLAIN);
    String s2 = t2.getMonomialString(VariableWriteMode.PLAIN);
    return s1.compareTo(s2);
  }

}
