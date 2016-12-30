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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;

/**
 * Linear expression over program variables.
 */
public class Template {
  private final LinearExpression<TVariable> linearExpression;

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

  @SuppressWarnings("unchecked")
  private Template(LinearExpression<? extends TVariable> pLinearExpression) {
    linearExpression = (LinearExpression<TVariable>) pLinearExpression;
  }

  public Kind getKind() {
    int s = linearExpression.size();

    if (s == 1 && getIterator().next().equals(Rational.ONE)) {
      return Kind.UPPER_BOUND;

    } else if (s == 1 && getIterator().next().equals(Rational.NEG_ONE)) {
      return Kind.NEG_LOWER_BOUND;

    } else if (s == 2) {
      Iterator<Rational> it = getIterator();
      Rational a = it.next();
      Rational b = it.next();
      if (a.equals(Rational.ONE) && b.equals(Rational.ONE)) {
        return Kind.SUM;
      } else if (a.equals(Rational.NEG_ONE) && b.equals(Rational.NEG_ONE)) {
        return Kind.NEG_SUM_LOWER_BOUND;
      } else if ((a.equals(Rational.ONE) && b.equals(Rational.NEG_ONE))
          || (a.equals(Rational.NEG_ONE) && b.equals(Rational.ONE))) {
        return Kind.DIFFERENCE;
      }
    }
    return Kind.COMPLEX;
  }

  private Iterator<Rational> getIterator() {
    return linearExpression.getMap().values().iterator();
  }

  public LinearExpression<TVariable> getLinearExpression() {
    return linearExpression;
  }

  public boolean isUnsigned() {
    return linearExpression.getMap().keySet()
        .stream().allMatch(k -> k.getType().isUnsigned());
  }

  public boolean isIntegral() {
    return linearExpression.getMap().entrySet().stream()
        .allMatch(e -> e.getValue().isIntegral()
            && e.getKey().getType().getType().isIntegerType());
  }

  public static Template of(LinearExpression<? extends TVariable> expr) {
    return new Template(expr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Template)) {
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
    List<TVariable> keys = new ArrayList<>(linearExpression.getMap().keySet());
    keys.sort(Comparator.comparing(pO -> pO.getName()));

    StringBuilder b = new StringBuilder();
    for (TVariable var : keys) {
      Rational coeff = linearExpression.getCoeff(var);
      LinearExpression.writeMonomial(var.getName(), coeff, b);
    }
    return b.toString();
  }
}
