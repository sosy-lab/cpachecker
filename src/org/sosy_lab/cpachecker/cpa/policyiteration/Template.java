package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

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
  public enum Kind {
    // Intervals.
    UPPER_BOUND, // +x
    NEG_LOWER_BOUND, // -x

    // Octagons.
    SUM, // +x+y
    DIFFERENCE, //x-y
    NEG_SUM_LOWER_BOUND, //-x-y

    // Everything else.
    COMPLEX
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

  public CSimpleType getType() {
    return type;
  }

  public static Template of(LinearExpression<CIdExpression> expr,
      CSimpleType pType) {
    return new Template(expr, pType, getKind(expr));
  }

  private static Kind getKind(LinearExpression<CIdExpression> expr) {
    int s = expr.size();
    if (s == 1 && Iterables.getOnlyElement(expr).getValue() == Rational.ONE) {

      return Kind.UPPER_BOUND;
    } else if (s == 1 && Iterables.getOnlyElement(expr).getValue() ==
        Rational.NEG_ONE) {

      return Kind.NEG_LOWER_BOUND;
    } else if (s == 2) {
      Iterator<Entry<CIdExpression, Rational>> it = expr.iterator();
      Rational a = it.next().getValue();
      Rational b = it.next().getValue();
      if (a == Rational.ONE && b == Rational.ONE) {
        return Kind.SUM;
      } else if (a == Rational.NEG_ONE && b == Rational.NEG_ONE) {
        return Kind.NEG_SUM_LOWER_BOUND;
      } else if (a == Rational.ONE && b == Rational.NEG_ONE ||
          a == Rational.NEG_ONE && b == Rational.ONE) {
        return Kind.DIFFERENCE;
      }
    }
    return Kind.COMPLEX;
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

  /**
   * @return Expression converted to C-like string, e.g. "x + 3 y".
   */
  public String toCString() {
    StringBuilder b = new StringBuilder();
    for (Entry<CIdExpression, Rational> e : linearExpression) {
      LinearExpression.writeMonomial(e.getKey().toASTString(), e.getValue(), b);
    }
    return b.toString();
  }

  /**
   * @return String suitable for formula serialization. Guarantees that two
   * equal templates will get an equal serialization.
   */
  public String toFormulaString() {

    // Sort by .getQualifiedName() first.
    Map<String, CIdExpression> mapping = new HashMap<>();
    ArrayList<String> varNames = new ArrayList<>();

    for (Entry<CIdExpression, Rational> monomial : linearExpression) {
      CIdExpression key = monomial.getKey();
      String varName = key.getDeclaration().getQualifiedName();

      mapping.put(varName, key);
      varNames.add(varName);
    }

    Collections.sort(varNames);

    StringBuilder b = new StringBuilder();
    for (String varName : varNames) {
      Rational coeff = linearExpression.getCoeff(mapping.get(varName));

      LinearExpression.writeMonomial(varName, coeff, b);
    }
    return b.toString();
  }

  @Override
  public String toString() {
    return linearExpression.toString();
  }
}
