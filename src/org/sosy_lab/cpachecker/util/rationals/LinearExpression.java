/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.rationals;

import java.util.Iterator;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.util.ImmutableMapMerger;

import com.google.common.collect.ImmutableMap;

/**
 * Simple <i>sparse</i> implementation for <i>homogeneous</i> linear expression
 * of the form $\Sigma a_i  x_i$, where $x_i$ is a set of variables and $a_i$
 * is a set of constants.
 */
public class LinearExpression implements Iterable<Entry<String, Rational>> {
  private final ImmutableMap<String, Rational> data;
  private int hashCache = 0;

  private LinearExpression(ImmutableMap<String, Rational> data) {
    this.data = data;
  }

  public static LinearExpression empty() {
    return new LinearExpression(ImmutableMap.<String, Rational>of());
  }

  public static LinearExpression pair(String var, Rational coeff) {
    return new LinearExpression(ImmutableMap.of(var, coeff));
  }

  public static LinearExpression ofVariable(String var) {
    return LinearExpression.pair(var, Rational.ONE);
  }

  /**
   * Add {@code other} linear expression.
   */
  public LinearExpression add(LinearExpression other) {
    return new LinearExpression(ImmutableMapMerger.merge(
        data,
        other.data,
        new ImmutableMapMerger.MergeFunc<String, Rational>() {
          @Override
          public Rational apply(String var, Rational a, Rational b) {
            return a.plus(b);
          }
        }
    ));
  }

  /**
   * Subtract {@code other} linear expression.
   */
  public LinearExpression sub(LinearExpression other) {
    return add(other.negate());
  }

  /**
   * Multiply the linear expression by {@code constant}.
   */
  public LinearExpression multByConst(final Rational constant) {
    return new LinearExpression(ImmutableMapMerger.merge(
        data,
        data,
        new ImmutableMapMerger.MergeFunc<String, Rational>() {
          @Override
          public Rational apply(String var, Rational a, Rational b) {
            return a.times(constant);
          }
        }
    ));
  }

  @SuppressWarnings("unused")
  public Rational getCoeff(String variable) {
    Rational out = data.get(variable);
    if (out == null) {
      return Rational.ZERO;
    }
    return out;
  }

  /**
   * Negate the linear expression.
   */
  public LinearExpression negate() {
    return multByConst(Rational.NEG_ONE);
  }

  public int size() {
    return data.size();
  }

  @Override
  public Iterator<Entry<String, Rational>> iterator() {
    return data.entrySet().iterator();
  }

  /**
   * @return Pretty-printing for linear expressions.
   * E. g. <i>-x + 2y + z</i>
   */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (Entry<String, Rational> monomial : this) {
      if (b.length() != 0) {
        b.append(" + ");
      }
      Rational coeff = monomial.getValue();
      String var = monomial.getKey();
      if (coeff == Rational.ONE) {
        b.append(var);
      } else if (coeff == Rational.NEG_ONE) {
        b.append("-").append(var);
      } else {
        b.append(coeff.toString()).append(var);
      }
    }
    return b.toString();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (object.getClass() != this.getClass()) {
      return false;
    }
    LinearExpression other = (LinearExpression) object;
    return data.equals(other.data);
  }

  @Override
  public int hashCode() {
    // Caching the hashing procedure.
    if (hashCache == 0) {
      hashCache = data.hashCode();
    }

    // Safe to do so, since we are immutable.
    return hashCache;
  }
}
