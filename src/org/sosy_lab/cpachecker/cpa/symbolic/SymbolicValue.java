/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.symbolic;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.base.Optional;

/**
 * Represents a symbolic value of an expression.
 *
 * <p>A symbolic value consists of a set of conditions that
 *    the value fulfills.</p>
 */
public class SymbolicValue implements Value {

  private final Condition lesserCondition;
  private final Condition greaterCondition;

  /**
   * Creates a new <code>SymbolicValue</code> object with the given
   * condition.
   *
   * @param pCondition the condition of the newly created symbolic value object
   */
  public SymbolicValue(Condition pCondition) {
    if (pCondition.isLesserCondition()) {
      lesserCondition = pCondition;
      greaterCondition = null;
    } else {
      lesserCondition = null;
      greaterCondition = pCondition;
    }
  }

  SymbolicValue(Condition pFirstCondition, Condition pSndCondition) {
    assert pFirstCondition.isLesserCondition() ^ pSndCondition.isLesserCondition();

    if (pFirstCondition.isLesserCondition()) {
      lesserCondition = pFirstCondition;
      greaterCondition = pSndCondition;
    } else {
      lesserCondition = pSndCondition;
      greaterCondition = pFirstCondition;
    }
  }

  public Optional<Condition> getLesserCondition() {
    return Optional.of(lesserCondition);
  }

  public Optional<Condition> getGreaterCondition() {
    return Optional.of(greaterCondition);
  }

  /**
   * Returns whether the given value is included in the concrete values this symbolic value
   * describes.
   *
   * <p>Example in pseudo-code:
   * <pre>
   *  SymbolicValue a = '> 5 and < 10';
   *  return a.includes(7);
   * </pre>
   * will return <code>true</code>.
   * </p>
   *
   * <p>This method only works on numeric values (like {@link NumericValue}) and other
   * <code>SymbolicValue</code> instances. It will return
   * <code>false</code> for all incompatible values.
   * </p>
   *
   * @param pValue the value to check against this symbolic value
   * @return <code>true</code> if the given value is included in this symbolic value, <code>false</code> otherwise.
   */
  public boolean includes(Value pValue) {
    boolean result = true;

    if (lesserCondition != null) {
      result = lesserCondition.includes(pValue);
    }

    if (greaterCondition != null) {
      result &= greaterCondition.includes(pValue);
    }

    return result;
  }

  /**
   * Merges the given value and this symbolic value.
   *
   * <p>This method returns the most concrete value possibly derivable from the two
   * symbolic values, wrapped in an <code>Optional</code>. If the two symbolic values
   * have no intersection, an empty <code>Optional</code> instance is returned.</p>
   *
   * <p>This method is commutative, that means a.mergeWith(b) equals b.mergeWith(a)</p>
   *
   * <p>Example 1 in pseudo-code:
   * <pre>
   *   SymbolicValue a = '>= 5';
   *   SymbolicValue b = '<= 5';
   *   return a.mergeWith(b);
   * </pre>
   * will return an <code>Optional</code> with the numeric value '5'.</p>
   *
   * <p>Example 2 in pseudo-code:
   * <pre>
   *   SymbolicValue a = '> 10';
   *   SymbolicValue b = '> 12';
   *   return a.mergeWith(b);
   * </pre>
   * will return an <code>Optional</code> with the symbolic value '> 12'.</p>
   *
   * <p>Example 3 in pseudo-code:
   * <pre>
   *   SymbolicValue a = '> 10';
   *   SymbolicValue b = '< 1';
   *   return a.mergeWith(b);
   * </pre>
   * will return an empty Optional.
   * </p>
   *
   * @param pValue the value to merge with this value
   * @return a {@link Value} object representing the intersection of both values.
   */
  public Optional<Value> mergeWith(Value pValue) {
    if (pValue.isNumericValue() && includes(pValue)) {
      return Optional.of(pValue);

    } else if (pValue instanceof SymbolicValue) {
      final SymbolicValue otherValue = (SymbolicValue) pValue;
      SymbolicValue lesserValue = null;
      SymbolicValue greaterValue = null;

      if (otherValue.lesserCondition != null && lesserCondition != null) {
        lesserValue = (SymbolicValue) lesserCondition.mergeWith(otherValue.lesserCondition).get();

      } else {
        if (otherValue.lesserCondition != null) {
          lesserValue = new SymbolicValue(otherValue.lesserCondition);

        } else if (lesserCondition != null) {
          lesserValue = new SymbolicValue(lesserCondition);
        }
      }

      if (otherValue.greaterCondition != null && greaterCondition != null) {
        greaterValue = (SymbolicValue) greaterCondition.mergeWith(otherValue.greaterCondition).get();

      } else {
        if (otherValue.greaterCondition != null) {
          greaterValue = new SymbolicValue(otherValue.greaterCondition);

        } else if (greaterCondition != null) {
          greaterValue = new SymbolicValue(greaterCondition);
        }
      }

      if (lesserValue != null && greaterValue != null) {
        return lesserValue.lesserCondition.mergeWith(greaterValue.greaterCondition);

      } else if (lesserValue != null) {
        return Optional.<Value>of(lesserValue);

      } else if (greaterValue != null) {
        return Optional.<Value>of(greaterValue);

      } else {
        throw new AssertionError("Symbolic value without lesser or greater condition!");
      }
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns <code>false</code>. A symbolic value is never a numeric value.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Returns <code>false</code>. A symbolic value is never unknown.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Returns <code>false</code>. A symbolic value is never explicitly known.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SymbolicValue [");

    if (lesserCondition != null) {
      sb.append(lesserCondition);

      if (greaterCondition != null) {
        sb.append(", ");
      }
    }

    if (greaterCondition != null) {
      sb.append(greaterCondition);
    }

    sb.append("]");

    return sb.toString();
  }
}
