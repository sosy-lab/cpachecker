package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

/**
 * Wrapper for a template.
 *
 * TODO: use a type for formula creation.
 */
public class Template {

  final LinearExpression linearExpression;
  final ASimpleDeclaration declaration;

  public Template(LinearExpression pLinearExpression,
      ASimpleDeclaration pDeclaration) {
    linearExpression = pLinearExpression;
    declaration = pDeclaration;
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
