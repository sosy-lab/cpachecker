package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Wrapper for a template.
 */
public class Template {
  final LinearExpression<CIdExpression> linearExpression;
  final CSimpleType type;
  final Kind kind;

  /**
   * Kind of a template.
   */
  public static enum Kind {
    UPPER_BOUND, NEG_LOWER_BOUND, COMPLEX
  }

  private Template(LinearExpression<CIdExpression> pLinearExpression,
      CSimpleType pType, Kind pKind) {
    linearExpression = pLinearExpression;
    type = pType;
    kind = pKind;
  }

  public Kind getKind() {
    return kind;
  }

  /**
   *
   */
  public static Template of(LinearExpression<CIdExpression> expr,
      CSimpleType pType) {
    Kind kind;
    int s = expr.size();
    if (s == 1 && Iterables.getOnlyElement(expr).getValue() == Rational.ONE) {

      kind = Kind.UPPER_BOUND;
    } else if (s == 1 && Iterables.getOnlyElement(expr).getValue() ==
        Rational.NEG_ONE) {

      kind = Kind.NEG_LOWER_BOUND;
    } else {
      kind = Kind.COMPLEX;
    }

    return new Template(expr, pType, kind);
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

      @Override
      public String apply(CIdExpression pCIdExpression) {
        return pCIdExpression.getDeclaration().getQualifiedName();
      }
    }));
  }
}
