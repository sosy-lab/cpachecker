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
package org.sosy_lab.cpachecker.util.invariants.balancer.interfaces;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;


public interface MatrixI {

  public MatrixI concat(MatrixI b);

  public MatrixI augment(MatrixI b);

  /*
   * Fill this matrix will zeros.
   */
  public void zeroFill();

  public RationalFunction get(int i, int j);

  public void set(int i, int j, RationalFunction f);

  /*
   * Return the product of elementary matrices that was used in the most
   * recent call to putInRREF, or null if it was never called.
   */
  public MatrixI getElemMatProd();

  /*
   * Swap rows i1 and i2.
   */
  public void swapRows(int i1, int i2);

  /*
   * Multiply row i by RationalFunction f.
   */
  public void multRow(int i, RationalFunction f);

  /*
   * Add row i2 times RationalFunction f to row i1.
   */
  public void addMultiple(int i1, int i2, RationalFunction f);


  /*
   * An "almost zero row" is one in which every nonaug entry is zero.
   * For such a row, the "terminals" are the entries in the aug columns.
   * We write the assumptions that the almost zero row terminals be zero
   */
  public AssumptionSet getAlmostZeroRowAssumptions();
  /*
   * Return the set of assumptions that all denominators in this matrix are nonzero.
   * We ignore denominators which are nonzero constants, and if we encounter a
   * denominator that is identically zero then we return a singleton set containing only
   * the assumption that zero is nonzero. (This might be useful for deriving a contradiction.)
   */
  public AssumptionSet getDenomNonZeroAssumptions();

  /*
   * Put this matrix into reduced row-echelon form, using Gaussian elimination.
   * Return set of nonzero assumptions for denominators in product of elementary matrices used.
   */
  public AssumptionSet putInRREF();

  public AssumptionSet putInRREF(LogManager logger);

}
