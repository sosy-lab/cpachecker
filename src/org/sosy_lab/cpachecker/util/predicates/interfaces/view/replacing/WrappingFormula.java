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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;


abstract class WrappingFormula<TWrap extends Formula, TOut extends Formula> {

  private final TWrap wrapped;
  private final FormulaType<TOut> type;

  WrappingFormula(FormulaType<TOut> pType, TWrap pWrapped) {
    wrapped = checkNotNull(pWrapped);
    type = checkNotNull(pType);
  }

  TWrap getWrapped() {
    return wrapped;
  }

  FormulaType<TOut> getType() {
    return type;
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return (prime + type.hashCode()) * prime + wrapped.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if ((pObj == null)
        || !getClass().equals(pObj.getClass())) {
      return false;
    }

    WrappingFormula<?, ?> other = (WrappingFormula<?, ?>)pObj;

    return wrapped.equals(other.wrapped)
        && type.equals(other.type);
  }
}

final class WrappingBitvectorFormula<TWrap extends Formula>
    extends WrappingFormula<TWrap, BitvectorFormula>
    implements BitvectorFormula {

  WrappingBitvectorFormula(FormulaType<BitvectorFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}

final class WrappingIntegerFormula<TWrap extends Formula>
    extends WrappingFormula<TWrap, IntegerFormula>
    implements IntegerFormula {

  WrappingIntegerFormula(FormulaType<IntegerFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}

final class WrappingRationalFormula<TWrap extends Formula>
    extends WrappingFormula<TWrap, RationalFormula>
    implements RationalFormula {

  WrappingRationalFormula(FormulaType<RationalFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}

final class WrappingBooleanFormula<TWrap extends Formula>
    extends WrappingFormula<TWrap, BooleanFormula>
    implements BooleanFormula {

  WrappingBooleanFormula(FormulaType<BooleanFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}