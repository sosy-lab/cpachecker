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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import org.sosy_lab.cpachecker.cpa.invariants.formula.AbstractFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ParameterizedInvariantsFormulaVisitor;


@SuppressWarnings("serial")
public class ArrayFormulaType<TI extends Formula, TE extends Formula, TFormulaInfo> extends AbstractFormula<TFormulaInfo> implements ArrayFormula<TI, TE> {

  private final FormulaType<TE> elementType;
  private final FormulaType<TI> indexType;

  public ArrayFormulaType(TFormulaInfo info, FormulaType<TI> indexType, FormulaType<TE> elementType) {
    this.elementType = elementType;
    this.indexType = indexType;
  }

  @Override
  public FormulaType<TE> getElementType() {
    return elementType;
  }

  @Override
  public FormulaType<TI> getIndexType() {
    return indexType;
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<TFormulaInfo, ReturnType> pVisitor) {
    return null;
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<TFormulaInfo, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return null;
  }
}
