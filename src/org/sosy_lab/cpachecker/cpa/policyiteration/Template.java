package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

/**
 * Wrapper for a template.
 *
 * TODO: use a type for formula creation.
 */
public class Template {

  final LinearExpression linearExpression;
  final Type type;

  public Template(LinearExpression pLinearExpression,
      Type pType) {
    linearExpression = pLinearExpression;
    type = pType;
  }

  @Override
  public boolean equals(Object o) {
    return o != null &&
          o.getClass() == getClass() &&
        linearExpression.equals(((Template)o).linearExpression);
  }

  @Override
  public int hashCode() {
    return linearExpression.hashCode();
  }

  @Override
  public String toString() {
    return linearExpression.toString();
  }
}
