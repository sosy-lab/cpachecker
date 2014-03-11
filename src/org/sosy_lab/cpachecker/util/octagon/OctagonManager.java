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
package org.sosy_lab.cpachecker.util.octagon;

import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.*;

import org.sosy_lab.common.Pair;

import com.google.common.collect.BiMap;


public class OctagonManager {

  /* Initialization */
  public static boolean init() {
    return J_init();
  }

  /* num handling function*/

  /* allocate new space for num array and init*/
  public static NumArray init_num_t (int n) {
    long l = J_init_n(n);
    return new NumArray(l);
  }

  /* num copy */
  public static void num_set(NumArray n1, NumArray n2) {
    J_num_set(n1.getArray(), n2.getArray());
  }

  public static Octagon set_bounds(Octagon oct, int pos, NumArray lower, NumArray upper) {
    return new Octagon(J_set_bounds(oct.getOctId(), pos, lower.getArray(), upper.getArray(), false));
  }

  /* set int */
  public static void num_set_int(NumArray n, int pos, int i) {
    J_num_set_int(n.getArray(), pos, i);
  }
  /* set float */
  public static void num_set_float(NumArray n, int pos, double d) {
    J_num_set_float(n.getArray(), pos, d);
  }
  /* set infinity */
  public static void num_set_inf(NumArray n, int pos) {
    J_num_set_inf(n.getArray(), pos);
  }

  public static long num_get_int(NumArray n, int pos) {
    return J_num_get_int(n.getArray(), pos);
  }

  public static double num_get_float(NumArray n, int pos) {
    return J_num_get_float(n.getArray(), pos);
  }

  public static boolean num_infty(NumArray n, int pos) {
    return J_num_infty(n.getArray(), pos);
  }

  public static void num_clear_n(NumArray n, int size) {
    J_num_clear_n(n.getArray(), size);
  }

  /* Octagon handling functions */

  /* Octagon Creation */
  public static Octagon empty(int n) {
    long l = J_empty(n);
    return new Octagon(l);
  }

  public static Octagon universe(int n) {
    long l = J_universe(n);
    return new Octagon(l);
  }
  static void free(Long oct) {
    J_free(oct);
  }

  public static Octagon copy(Octagon oct) {
    long l = J_copy(oct.getOctId());
    return new Octagon(l);
  }

  public static Octagon full_copy(Octagon oct) {
    long l = J_full_copy(oct.getOctId());
    return new Octagon(l);
  }

  /* Query Functions */
  public static int dimension(Octagon oct) {
    return J_dimension(oct.getOctId());
  }

  public static int nbconstraints(Octagon oct) {
    return J_nbconstraints(oct.getOctId());
  }

  /* Test Functions */
  public static boolean isEmpty(Octagon oct) {
    return J_isEmpty(oct.getOctId());
  }

  public static int isEmptyLazy(Octagon oct) {
    return J_isEmptyLazy(oct.getOctId());
  }

  public static boolean isUniverse(Octagon oct) {
    return J_isUniverse(oct.getOctId());
  }

  public static boolean isIncludedIn(Octagon oct1, Octagon oct2) {
    return J_isIncludedIn(oct1.getOctId(), oct2.getOctId());
  }

  public static int isIncludedInLazy(Octagon oct1, Octagon oct2) {
    return J_isIncludedInLazy(oct1.getOctId(), oct2.getOctId());
  }

  public static boolean isEqual(Octagon oct1, Octagon oct2) {
    return J_isEqual(oct1.getOctId(), oct2.getOctId());
  }

  public static int isEqualLazy(Octagon oct1, Octagon oct2) {
    return J_isEqualLazy(oct1.getOctId(), oct2.getOctId());
  }

  public static boolean isIn(Octagon oct1, NumArray array) {
    return J_isIn(oct1.getOctId(), array.getArray());
  }

  /* Operators */
  public static Octagon intersection(Octagon oct1, Octagon oct2) {
    long l = J_intersection(oct1.getOctId(), oct2.getOctId(), false);
    return new Octagon(l);
  }

  public static Octagon union(Octagon oct1, Octagon oct2) {
    long l = J_union(oct1.getOctId(), oct2.getOctId(), false);
    return new Octagon(l);
  }

  /* int widening = 0 -> OCT_WIDENING_FAST
   * int widening = 1 ->  OCT_WIDENING_ZERO
   * int widening = 2 -> OCT_WIDENING_UNIT*/
  public static Octagon widening(Octagon oct1, Octagon oct2) {
    long l = J_widening(oct1.getOctId(), oct2.getOctId(), false, 1);
    return new Octagon(l);
  }

  public static Octagon narrowing(Octagon oct1, Octagon oct2) {
    long l = J_narrowing(oct1.getOctId(), oct2.getOctId(), false);
    return new Octagon(l);
  }

  /* Transfer Functions */
  public static Octagon forget(Octagon oct, int k) {
    long l = J_forget(oct.getOctId(), k, false);
    return new Octagon(l);
  }

  public static Octagon assingVar(Octagon oct, int k, NumArray array) {
    long l = J_assingVar(oct.getOctId(), k, array.getArray(), false);
    return new Octagon(l);
  }

  public static Octagon addBinConstraint(Octagon oct, int noOfConstraints, NumArray array) {
    long  l = J_addBinConstraints(oct.getOctId(), noOfConstraints, array.getArray(), false);
    return new Octagon(l);
  }

  public static Octagon substituteVar(Octagon oct, int x, NumArray array) {
    long l = J_substituteVar(oct.getOctId(), x, array.getArray(), false);
    return new Octagon(l);
  }

  public static Octagon addConstraint(Octagon oct, NumArray array) {
    long l = J_addConstraint(oct.getOctId(), array.getArray(), false);
    return new Octagon(l);
  }
  public static Octagon intervAssingVar(Octagon oct, int k, NumArray array) {
    long l = J_intervAssingVar(oct.getOctId(), k, array.getArray(), false);
    return new Octagon(l);
  }
  public static Octagon intervSubstituteVar(Octagon oct, int x, NumArray array) {
    long l = J_intervSubstituteVar(oct.getOctId(), x, array.getArray(), false);
    return new Octagon(l);
  }
  public static Octagon intervAddConstraint(Octagon oct, NumArray array) {
    long l = J_intervAddConstraint(oct.getOctId(), array.getArray(), false);
    return new Octagon(l);
  }

  /* change of dimensions */
  public static Octagon addDimensionAndEmbed(Octagon oct, int k) {
    long l = J_addDimenensionAndEmbed(oct.getOctId(), k, false);
    return new Octagon(l);
  }
  public static Octagon addDimensionAndProject(Octagon oct, int k) {
    long l = J_addDimenensionAndProject(oct.getOctId(), k, false);
    return new Octagon(l);
  }
  public static Octagon removeDimension(Octagon oct, int k) {
    long l = J_removeDimension(oct.getOctId(), k, false);
    return new Octagon(l);
  }

  public static void printNum(NumArray arr, int size) {
      J_printNum(arr.getArray(), size);
  }

  public static void printOct(Octagon oct) {
    J_print(oct.getOctId());
  }

  public static String print(Octagon oct, BiMap<Integer, String> map) {
    StringBuilder str = new StringBuilder();
    int dimension = dimension(oct);
    long pointer = oct.getOctId();
    str.append("Octagon (id: " + pointer + ") (dimension: " + dimension + ")\n");
    if (isEmpty(oct)) {
      str.append("[Empty]\n");
      return str.toString();
    }

    NumArray lower = OctagonManager.init_num_t(1);
    NumArray upper = OctagonManager.init_num_t(1);

    for (int i = 0; i < map.size(); i++) {
      str.append(" ").append(map.get(i)).append(" -> [");
      J_get_bounds(oct.getOctId(), i, upper.getArray(), lower.getArray());
      if (J_num_infty(lower.getArray(), 0)) {
        str.append("-INFINITY, ");
      } else {
        str.append(J_num_get_int(lower.getArray(), 0)*-1).append(", ");
      }
      if (J_num_infty(upper.getArray(), 0)) {
        str.append("INFINITY]\n");
      } else {
        str.append(J_num_get_int(upper.getArray(), 0)).append("]\n");
      }
    }
    J_num_clear_n(lower.getArray(), 1);
    J_num_clear_n(upper.getArray(), 1);
    return str.toString();
  }

  public static Pair<Long, Long> getVariableBounds(Octagon oct, int id) {
    NumArray lower = OctagonManager.init_num_t(1);
    NumArray upper = OctagonManager.init_num_t(1);
    J_get_bounds(oct.getOctId(), id, upper.getArray(), lower.getArray());
    Pair<Long, Long> retVal = Pair.of(J_num_get_int(lower.getArray(), 0)*-1,
                                      J_num_get_int(upper.getArray(), 0));
    J_num_clear_n(lower.getArray(), 1);
    J_num_clear_n(upper.getArray(), 1);
    return retVal;
  }
}