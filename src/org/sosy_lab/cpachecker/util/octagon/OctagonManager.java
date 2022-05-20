// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_addBinConstraints;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_addConstraint;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_addDimenensionAndEmbed;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_addDimenensionAndProject;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_assingVar;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_copy;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_dimension;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_empty;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_forget;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_free;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_full_copy;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_init;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_init_n;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_intersection;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_intervAddConstraint;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_intervAssingVar;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_intervSubstituteVar;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isEmpty;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isEmptyLazy;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isEqual;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isEqualLazy;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isIn;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isIncludedIn;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isIncludedInLazy;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_isUniverse;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_narrowing;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_nbconstraints;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_clear_n;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_get_float;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_get_int;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_infty;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_set;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_set_float;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_set_inf;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_set_int;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_print;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_printNum;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_removeDimension;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_set_bounds;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_substituteVar;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_union;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_universe;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_widening;

import com.google.common.collect.BiMap;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public abstract class OctagonManager {

  private static boolean libraryLoaded = false;

  @SuppressWarnings("StaticAssignmentInConstructor")
  protected OctagonManager(String libraryName) {
    if (!libraryLoaded) {
      libraryLoaded = true;
      NativeLibraries.loadLibrary(libraryName);
      J_init();
    }
  }

  /* num handling function*/

  /* allocate new space for num array and init*/
  public final NumArray init_num_t (int n) {
    return new NumArray(J_init_n(n));
  }

  /* num copy */
  public final void num_set(NumArray n1, NumArray n2) {
    J_num_set(n1.getArray(), n2.getArray());
  }

  public final Octagon set_bounds(Octagon oct, int pos, NumArray lower, NumArray upper) {
    return new Octagon(J_set_bounds(oct.getOctId(), pos, lower.getArray(), upper.getArray(), false), this);
  }

  /* set int */
  public final void num_set_int(NumArray n, int pos, long i) {
    J_num_set_int(n.getArray(), pos, (int)i);
  }
  /* set float */
  public final void num_set_float(NumArray n, int pos, double d) {
    J_num_set_float(n.getArray(), pos, d);
  }
  /* set infinity */
  public final void num_set_inf(NumArray n, int pos) {
    J_num_set_inf(n.getArray(), pos);
  }

  public final long num_get_int(NumArray n, int pos) {
    return J_num_get_int(n.getArray(), pos);
  }

  public final double num_get_float(NumArray n, int pos) {
    return J_num_get_float(n.getArray(), pos);
  }

  public final boolean num_infty(NumArray n, int pos) {
    return J_num_infty(n.getArray(), pos);
  }

  public final void num_clear_n(NumArray n, int size) {
    J_num_clear_n(n.getArray(), size);
  }

  /* Octagon handling functions */

  /* Octagon Creation */
  public final Octagon empty(int n) {
    return new Octagon(J_empty(n), this);
  }

  public final Octagon universe(int n) {
    return new Octagon(J_universe(n), this);
  }
  final void free(Long oct) {
    J_free(oct);
  }

  public final Octagon copy(Octagon oct) {
    return new Octagon(J_copy(oct.getOctId()), this);
  }

  public final Octagon full_copy(Octagon oct) {
    return new Octagon(J_full_copy(oct.getOctId()), this);
  }

  /* Query Functions */
  public final int dimension(Octagon oct) {
    return J_dimension(oct.getOctId());
  }

  public final int nbconstraints(Octagon oct) {
    return J_nbconstraints(oct.getOctId());
  }

  /* Test Functions */
  public final boolean isEmpty(Octagon oct) {
    return J_isEmpty(oct.getOctId());
  }

  public final int isEmptyLazy(Octagon oct) {
    return J_isEmptyLazy(oct.getOctId());
  }

  public final boolean isUniverse(Octagon oct) {
    return J_isUniverse(oct.getOctId());
  }

  public final boolean isIncludedIn(Octagon oct1, Octagon oct2) {
    return J_isIncludedIn(oct1.getOctId(), oct2.getOctId());
  }

  public final int isIncludedInLazy(Octagon oct1, Octagon oct2) {
    return J_isIncludedInLazy(oct1.getOctId(), oct2.getOctId());
  }

  public final boolean isEqual(Octagon oct1, Octagon oct2) {
    return J_isEqual(oct1.getOctId(), oct2.getOctId());
  }

  public final int isEqualLazy(Octagon oct1, Octagon oct2) {
    return J_isEqualLazy(oct1.getOctId(), oct2.getOctId());
  }

  public final boolean isIn(Octagon oct1, NumArray array) {
    return J_isIn(oct1.getOctId(), array.getArray());
  }

  /* Operators */
  public final Octagon intersection(Octagon oct1, Octagon oct2) {
    return new Octagon(J_intersection(oct1.getOctId(), oct2.getOctId(), false), this);
  }

  public final Octagon union(Octagon oct1, Octagon oct2) {
    return new Octagon(J_union(oct1.getOctId(), oct2.getOctId(), false), this);
  }

  /* int widening = 0 -> OCT_WIDENING_FAST
   * int widening = 1 ->  OCT_WIDENING_ZERO
   * int widening = 2 -> OCT_WIDENING_UNIT*/
  public final Octagon widening(Octagon oct1, Octagon oct2) {
    return new Octagon(J_widening(oct1.getOctId(), oct2.getOctId(), false, 1), this);
  }

  public final Octagon narrowing(Octagon oct1, Octagon oct2) {
    return new Octagon(J_narrowing(oct1.getOctId(), oct2.getOctId(), false), this);
  }

  /* Transfer Functions */
  public final Octagon forget(Octagon oct, int k) {
    return new Octagon(J_forget(oct.getOctId(), k, false), this);
  }

  public final Octagon assingVar(Octagon oct, int k, NumArray array) {
    return new Octagon(J_assingVar(oct.getOctId(), k, array.getArray(), false), this);
  }

  public final Octagon addBinConstraint(Octagon oct, int noOfConstraints, NumArray array) {
    return new Octagon(J_addBinConstraints(oct.getOctId(), noOfConstraints, array.getArray(), false), this);
  }

  public final Octagon substituteVar(Octagon oct, int x, NumArray array) {
    return new Octagon(J_substituteVar(oct.getOctId(), x, array.getArray(), false), this);
  }

  public final Octagon addConstraint(Octagon oct, NumArray array) {
    return new Octagon(J_addConstraint(oct.getOctId(), array.getArray(), false), this);
  }
  public final Octagon intervAssingVar(Octagon oct, int k, NumArray array) {
    return new Octagon(J_intervAssingVar(oct.getOctId(), k, array.getArray(), false), this);
  }
  public final Octagon intervSubstituteVar(Octagon oct, int x, NumArray array) {
    return new Octagon(J_intervSubstituteVar(oct.getOctId(), x, array.getArray(), false), this);
  }
  public final Octagon intervAddConstraint(Octagon oct, NumArray array) {
    return new Octagon(J_intervAddConstraint(oct.getOctId(), array.getArray(), false), this);
  }

  /* change of dimensions */
  public final Octagon addDimensionAndEmbed(Octagon oct, int k) {
    return new Octagon(J_addDimenensionAndEmbed(oct.getOctId(), k, false), this);
  }
  public final Octagon addDimensionAndProject(Octagon oct, int k) {
    return new Octagon(J_addDimenensionAndProject(oct.getOctId(), k, false), this);
  }
  public final Octagon removeDimension(Octagon oct, int k) {
    return new Octagon(J_removeDimension(oct.getOctId(), k, false), this);
  }

  public final void printNum(NumArray arr, int size) {
      J_printNum(arr.getArray(), size);
  }

  public final void printOct(Octagon oct) {
    J_print(oct.getOctId());
  }

  public abstract String print(Octagon oct, BiMap<Integer, MemoryLocation> map);
  public abstract OctagonInterval getVariableBounds(Octagon oct, int id);
}