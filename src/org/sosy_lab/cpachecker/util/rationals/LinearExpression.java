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

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.util.ImmutableMapMerger;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Simple <i>sparse</i> implementation for <i>homogeneous</i> linear expression
 * of the form $\Sigma a_i  x_i$, where $x_i$ is a set of variables and $a_i$
 * is a set of constants.
 */
public class LinearExpression implements Iterable<Entry<String, ExtendedRational>> {
  private final ImmutableMap<String, ExtendedRational> data;

  private LinearExpression(ImmutableMap<String, ExtendedRational> data) {
    this.data = data;
  }

  public static LinearExpression empty() {
    return new LinearExpression(ImmutableMap.<String, ExtendedRational>of());
  }

  public static LinearExpression pair(String var, ExtendedRational coeff) {
    return new LinearExpression(ImmutableMap.of(var, coeff));
  }

  public static LinearExpression ofVariable(String var) {
    return LinearExpression.pair(var, ExtendedRational.ofInt(1));
  }

  /**
   * Add [other] linear expression.
   */
  public LinearExpression add(LinearExpression other) {
    return new LinearExpression(ImmutableMapMerger.merge(
        data,
        other.data,
        new ImmutableMapMerger.MergeFunc<ExtendedRational>() {
          @Override
          public ExtendedRational apply(ExtendedRational a, ExtendedRational b) {
            return a.plus(b);
          }
        }
    ));
  }

  /**
   * Subtract [other] linear expression.
   */
  public LinearExpression sub(LinearExpression other) {
    return new LinearExpression(ImmutableMapMerger.merge(
        data,
        other.data,
        new ImmutableMapMerger.MergeFunc<ExtendedRational>() {
          @Override
          public ExtendedRational apply(ExtendedRational a, ExtendedRational b) {
            return a.minus(b);
          }
        }
    ));
  }

  /**
   * Multiply the linear expression by [constant].
   */
  public LinearExpression multByConst(final ExtendedRational constant) {
    return new LinearExpression(ImmutableMapMerger.merge(
        data,
        data,
        new ImmutableMapMerger.MergeFunc<ExtendedRational>() {
          @Override
          public ExtendedRational apply(ExtendedRational a, ExtendedRational b) {
            return a.times(constant);
          }
        }
    ));
  }

  public ExtendedRational getCoeff(String variable) {
    ExtendedRational out = data.get(variable);
    if (out == null) {
      return ExtendedRational.ZERO;
    }
    return out;
  }

  /**
   * Negate the linear expression.
   */
  public LinearExpression negate() {
    return multByConst(ExtendedRational.ONE.negate());
  }

  @Override
  public Iterator<Entry<String, ExtendedRational>> iterator() {
    return data.entrySet().iterator();
  }
}
