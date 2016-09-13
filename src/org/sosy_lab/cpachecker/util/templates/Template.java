/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.templates;

import com.google.common.collect.Iterables;

import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Linear expression over program variables.
 */
public class Template {
  // todo: switch to MemoryLocation, additionally track type.
  private final LinearExpression<CIdExpression> linearExpression;

  public Stream<String> getUsedVars() {
    return linearExpression.getMap().keySet()
        .stream()
        .map(s -> s.getDeclaration().getQualifiedName());
  }

  public boolean hasGlobals() {
    return getUsedVars().filter(s -> !s.contains("::")).findAny().isPresent();
  }

  /**
   * Template type.
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

  private Template(LinearExpression<CIdExpression> pLinearExpression) {
    linearExpression = pLinearExpression;
  }

  public Kind getKind() {
    return getKind(linearExpression);
  }

  public LinearExpression<CIdExpression> getLinearExpression() {
    return linearExpression;
  }

  public boolean isUnsigned() {
    for (Entry<CIdExpression, Rational> e: linearExpression) {
      CIdExpression expr = e.getKey();
      CSimpleType type = (CSimpleType)expr.getExpressionType();
      if (!type.isUnsigned()) {
        return false;
      }
    }
    return true;
  }

  public boolean isIntegral() {
    for (Entry<CIdExpression, Rational> e : linearExpression) {
      Rational coeff = e.getValue();
      CIdExpression id = e.getKey();
      if (!(coeff.isIntegral() &&
          ((CSimpleType)id.getExpressionType()).getType().isIntegerType())) {
        return false;
      }
    }
    return true;
  }

  public static Template of(LinearExpression<CIdExpression> expr) {
    return new Template(expr);
  }

  private static Kind getKind(LinearExpression<CIdExpression> expr) {
    int s = expr.size();
    if (s == 1
        && Objects.equals(Iterables.getOnlyElement(expr).getValue(), Rational.ONE)) {

      return Kind.UPPER_BOUND;
    } else if (s == 1
        && Objects.equals(Iterables.getOnlyElement(expr).getValue(), Rational.NEG_ONE)) {

      return Kind.NEG_LOWER_BOUND;
    } else if (s == 2) {
      Iterator<Entry<CIdExpression, Rational>> it = expr.iterator();
      Rational a = it.next().getValue();
      Rational b = it.next().getValue();
      if (Objects.equals(a, Rational.ONE) && Objects.equals(b, Rational.ONE)) {
        return Kind.SUM;
      } else if (Objects.equals(a, Rational.NEG_ONE)
          && Objects.equals(b, Rational.NEG_ONE)) {
        return Kind.NEG_SUM_LOWER_BOUND;
      } else if ((Objects.equals(a, Rational.ONE) && Objects.equals(b, Rational.NEG_ONE))
          || (Objects.equals(a, Rational.NEG_ONE) && Objects.equals(b, Rational.ONE))) {
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


  public int size() {
    return linearExpression.size();
  }

  /**
   * @return String suitable for formula serialization.
   * Guarantees that two equal templates will get an equal serialization.
   */
  @Override
  public String toString() {

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
}
