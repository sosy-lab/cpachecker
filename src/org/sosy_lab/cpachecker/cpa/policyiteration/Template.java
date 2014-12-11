package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Objects;

/**
 * Wrapper for a template.
 */
public final class Template {
  final LinearExpression<String> linearExpression;
  final CSimpleType type;

  public Template(LinearExpression<String> pLinearExpression,
      CSimpleType pType) {
    linearExpression = pLinearExpression;
    type = pType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (o.getClass() != this.getClass()) {
      return false;
    }
    Template other = (Template) o;
    return linearExpression.equals(other.linearExpression) &&
        type.equals(other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(linearExpression, type);
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", linearExpression.toString(), type);
  }
}
