/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.loopinvariants.polynom;

import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor;

public class Exponent extends ExpoExpression implements PolynomExpression {

  private Variable basis;

  private MultExpression exponent;

  public Exponent(Variable pBasis, MultExpression pMultExpression) {
    super();
    basis = pBasis;
    exponent = pMultExpression;
  }

  /**
   * @return the basis
   */
  public Variable getBasis() {
    return basis;
  }


  /**
   * @return the exponent
   */
  public MultExpression getExponent() {
    return exponent;
  }

  @Override
  public <T, E extends Exception> T accept(Visitor<T, E> pVis) throws E {
    return pVis.visit(this);
  }

  @Override
  public String toString() {
    return basis + "**(" + exponent + ")";
  }
}
