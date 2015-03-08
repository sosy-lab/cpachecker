package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Function;

/**
 * Wrapper for a template.
 */
public final class Template {
  final LinearExpression<CIdExpression> linearExpression;
  final CSimpleType type;

  public Template(LinearExpression<CIdExpression> pLinearExpression,
      CSimpleType pType) {
    linearExpression = pLinearExpression;
    type = pType;
  }

  public boolean isLowerBound() {
    return linearExpression.size() == 1 &&
        linearExpression.iterator().next().getValue() == Rational.NEG_ONE;
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
    return linearExpression.equals(other.linearExpression);
  }

  @Override
  public int hashCode() {
    return linearExpression.hashCode();
  }

  @Override
  public String toString() {
    return String.format("%s", linearExpression.toString(new Function<CIdExpression, String>() {
      public String apply(CIdExpression pCIdExpression) {
        return pCIdExpression.getDeclaration().getQualifiedName();
      }
    }));
  }
}
