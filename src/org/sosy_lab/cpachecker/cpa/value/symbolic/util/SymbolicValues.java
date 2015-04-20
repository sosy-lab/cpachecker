/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

/**
 * Util class for {@link SymbolicValue}.
 */
public class SymbolicValues {

  /**
   * Returns whether the given constraints are equal in their meaning.
   * This is the case if two constraints are completely equal after replacing symbolic expressions
   * with the program variables they represent.
   *
   * <p>Example: <code>s1 < 5</code> is equal to <code>s2 + 2 < 5</code> in respect to its meaning
   * with <code>s1</code> and <code>s2</code> being symbolic identifiers, if both constraints
   * represent <code>a < 5</code> with <code>a</code> being a program variable.</p>
   *
   * @param pValue1 the first symbolic value
   * @param pValue2 the second symbolic value
   * @return <code>true</code> if both symbolic values represent the same meaning
   */
  public static boolean haveEqualMeaning(
      final SymbolicValue pValue1,
      final SymbolicValue pValue2
  ) {

    if (!pValue1.getClass().equals(pValue2.getClass())) {
      return pValue1.getRepresentedLocation().equals(pValue2.getRepresentedLocation());
    }

    final Optional<MemoryLocation> maybeRepLocVal1 = pValue1.getRepresentedLocation();
    final Optional<MemoryLocation> maybeRepLocVal2 = pValue2.getRepresentedLocation();

    if (pValue1 instanceof SymbolicIdentifier || pValue1 instanceof ConstantSymbolicExpression) {
      assert pValue2 instanceof SymbolicIdentifier || pValue2 instanceof ConstantSymbolicExpression;

      return maybeRepLocVal1.equals(maybeRepLocVal2);

    } else if (pValue1 instanceof UnarySymbolicExpression) {
      assert pValue2 instanceof UnarySymbolicExpression;

      final SymbolicValue val1Op = ((UnarySymbolicExpression) pValue1).getOperand();
      final SymbolicValue val2Op = ((UnarySymbolicExpression) pValue2).getOperand();

      return maybeRepLocVal1.equals(maybeRepLocVal2) && haveEqualMeaning(val1Op, val2Op);

    } else if (pValue1 instanceof BinarySymbolicExpression) {
      assert pValue2 instanceof BinarySymbolicExpression;

      final SymbolicValue val1Op1 = ((BinarySymbolicExpression) pValue1).getOperand1();
      final SymbolicValue val1Op2 = ((BinarySymbolicExpression) pValue1).getOperand2();
      final SymbolicValue val2Op1 = ((BinarySymbolicExpression) pValue2).getOperand1();
      final SymbolicValue val2Op2 = ((BinarySymbolicExpression) pValue2).getOperand2();

      return maybeRepLocVal1.equals(maybeRepLocVal2)
          && haveEqualMeaning(val1Op1, val2Op1)
          && haveEqualMeaning(val1Op2, val2Op2);

    } else {
      throw new AssertionError("Unhandled symbolic value type " + pValue1.getClass());
    }
  }
}
