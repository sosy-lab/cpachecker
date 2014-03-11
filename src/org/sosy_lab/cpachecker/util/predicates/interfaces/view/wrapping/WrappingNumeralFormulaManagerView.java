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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.wrapping;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.wrapping.NumeralFormulaView.IntegerFormulaView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.wrapping.NumeralFormulaView.RationalFormulaView;


public class WrappingNumeralFormulaManagerView
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends NumeralFormulaManagerView<ParamFormulaType, ResultFormulaType> {

  public WrappingNumeralFormulaManagerView(NumeralFormulaManager<ParamFormulaType, ResultFormulaType> pManager) {
    super(pManager);
  }

  @Override
  public ResultFormulaType wrapInView(ResultFormulaType pFormula) {
    if (pFormula instanceof IntegerFormula) {
      return (ResultFormulaType) new IntegerFormulaView((IntegerFormula)pFormula, this);
    } else if (pFormula instanceof RationalFormula) {
      return (ResultFormulaType) new RationalFormulaView((RationalFormula)pFormula, this);
    } else
      throw new AssertionError("unhandled formulatype: " + pFormula.getClass());
  }

  @Override
  public ParamFormulaType extractFromView(ParamFormulaType pFormula) {
    return (ParamFormulaType)((NumeralFormulaView)pFormula).getWrapped();
  }
}
