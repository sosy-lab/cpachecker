/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;


public abstract class WrappingFormula<TWrap extends Formula, TOut extends Formula> {
  private TWrap wrapped;
  private FormulaType<TOut> type;
  public WrappingFormula(FormulaType<TOut> type, TWrap toWrap) {
    wrapped = toWrap;
    this.type = type;
  }

  public TWrap getWrapped() { return wrapped; }
  public FormulaType<TOut> getType() { return type; }
  @Override
  public String toString(){
    return "Wrapped(" + getWrapped().toString() + ")";
  }
  @Override
  public int hashCode(){
    return getWrapped().hashCode();
  }
}

class WrappingBitvectorFormula<TWrap extends Formula>
extends WrappingFormula<TWrap, BitvectorFormula>
implements BitvectorFormula {
  public WrappingBitvectorFormula(FormulaType<BitvectorFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}
class WrappingRationalFormula<TWrap extends Formula>
extends WrappingFormula<TWrap, RationalFormula>
implements RationalFormula {
  public WrappingRationalFormula(FormulaType<RationalFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}
class WrappingBooleanFormula<TWrap extends Formula>
extends WrappingFormula<TWrap, BooleanFormula>
implements BooleanFormula {
  public WrappingBooleanFormula(FormulaType<BooleanFormula> type, TWrap pToWrap) {
    super(type, pToWrap);
  }
}