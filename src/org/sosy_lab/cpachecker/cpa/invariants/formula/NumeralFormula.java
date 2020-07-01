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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.Typed;

/**
 * Instances of implementing classes represent invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public interface NumeralFormula<ConstantType> extends Typed {

  /**
   * Accepts the given invariants formula visitor.
   *
   * @param pVisitor the visitor to accept.
   *
   * @return the result computed by the visitor for this specific invariants
   * formula.
   */
  <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor);

  /**
   * Accepts the given parameterized formula visitor.
   *
   * @param pVisitor the visitor to accept.
   * @param pParameter the parameter to be handed to the visitor for this visit.
   *
   * @return the result computed by the visitor for this specific invariants
   * formula.
   */
  <ReturnType, ParamType> ReturnType accept(ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter);

}
