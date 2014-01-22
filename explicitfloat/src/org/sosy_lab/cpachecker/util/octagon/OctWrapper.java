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
package org.sosy_lab.cpachecker.util.octagon;

import org.sosy_lab.cpachecker.util.NativeLibraries;

class OctWrapper {

  private OctWrapper() { }

  static {
    NativeLibraries.loadLibrary("JOct");
  }

  /* Initialization */
  static native boolean J_init();        //int oct_init()

  /* num handling function*/

  /* allocate new space for num array and init*/
  static native long J_init_n (int n); // first allocates space with new_n for num_t* and calls void num_init_n (num_t* a, size_t n), returns the pointer to allocated space
  /* num copy */
  static native void J_num_set(long n1, long n2); // void num_set (num_t* a, const num_t* b)
  /* set int */
  static native void J_num_set_int(long n, int pos, int i); // void num_set_int (num_t* a, long i)
  /* set float */
  static native void J_num_set_float(long n, int pos, double d); // void num_set_float (num_t* a, double d)
  /* set infinity */
  static native void J_num_set_inf(long n, int pos); // void num_set_infty (num_t* a)

  static native long J_num_get_int(long n, int pos); // long num_get_int (const num_t* a)
  static native double J_num_get_float(long n, int pos); // double num_get_float (const num_t* a)
  static native boolean J_num_infty(long n, int pos); // bool num_infty(const num_t* a)

  static native void J_num_clear_n(long n, int size); // call void num_clear_n (num_t* a, size_t n) and oct_mm_free(c) afterwards


  /* Octagon handling functions */

  /* Octagon Creation */
  static native long J_empty(int n);      //oct_t* oct_empty (var_t n)
  static native long J_universe(int n);    //oct_t* oct_universe (var_t n)
  static native void J_free(long oct);    //void oct_free (oct_t* m)
  static native long J_copy(long oct); //oct_t* oct_copy (oct_t* m)
  static native long J_full_copy(long oct); //oct_t* oct_full_copy (oct_t* m)

  /* Query Functions */
  static native int J_dimension(long oct);      //var_t oct_dimension (oct_t* m)
  static native int J_nbconstraints(long oct);    //size_t oct_nbconstraints (oct_t* m)

  /* Test Functions */
  static native boolean J_isEmpty(long oct);      //bool oct_is_empty (oct_t* m)
  static native int J_isEmptyLazy(long oct);      //tbool oct_is_empty_lazy (oct_t* m)
  static native boolean J_isUniverse(long oct);    //bool oct_is_universe (oct_t* m)
  static native boolean J_isIncludedIn(long oct1, long oct2);  //bool oct_is_included_in (oct_t* ma, oct_t* mb)
  static native int J_isIncludedInLazy(long oct1, long oct2);  //tbool oct_is_included_in_lazy (oct_t* ma, oct_t* mb)
  static native boolean J_isEqual(long oct1, long oct2);    //bool oct_is_equal (oct_t* ma, oct_t* mb)
  static native int J_isEqualLazy(long oct1, long oct2);    //tbool oct_is_equal_lazy (oct_t* ma, oct_t* mb)
  static native boolean J_isIn(long oct1, long array);      //bool oct_is_in (oct_t* m, const num_t* v)

  /* Operators */
  static native long J_intersection(long oct1, long oct2, boolean dest);  //oct_t* oct_intersection (oct_t* ma, oct_t* mb, bool destructive)
  static native long J_union(long oct1, long oct2, boolean dest);      //oct_t* oct_convex_hull (oct_t* ma, oct_t* mb, bool destructive)
  /* int widening = 0 -> OCT_WIDENING_FAST
   * int widening = 1 ->  OCT_WIDENING_ZERO
   * int widening = 2 -> OCT_WIDENING_UNIT*/
  static native long J_widening(long oct1, long oct2, boolean dest, int widening);  //oct_t* oct_widening( oct_t* ma, oct_t* mb, bool destructive,oct_widening_type)
  static native long J_narrowing(long oct1, long oct2, boolean dest);    //oct_t* oct_narrowing (oct_t* ma, oct_t* mb, bool destructive)

  /* Transfer Functions */
  static native long J_forget(long oct, int k, boolean dest);    //oct_t* oct_forget (oct_t* m, var_t k, bool destructive)
  //static native long J_addBinConstraints (long oct, int nb, Constraint[] consArray, boolean dest);    //oct_t* oct_add_bin_constraints ( oct_t* m, unsigned int nb, const oct_cons* cons, bool destructive)
  static native long J_assingVar(long oct, int k, long array, boolean dest);  // oct_t* oct_assign_variable ( oct_t* m, var_t x, const num_t* tab,bool destructive)
  // options for add J_addBinConstraints
  //  px   = 0,  /*    x <= c  (y ignored) */
  //  mx   = 1,  /*   -x <= c  (y ignored) */
  //  pxpy = 2,  /*  x+y <= c */
  //  pxmy = 3,  /*  x-y <= c */
  //  mxpy = 4,  /* -x+y <= c */
  //  mxmy = 5   /* -x-y <= c */
  static native long J_addBinConstraints(long oct, int k, long array, boolean dest); // oct_t* oct_add_bin_constraints ( oct_t* m, unsigned int nb, const oct_cons* cons, bool destructive)
  static native long J_substituteVar(long oct, int x, long array, boolean dest);  //oct_t* oct_substitute_variable ( oct_t* m, var_t x, const num_t* tab, bool destructive)
  static native long J_addConstraint(long oct, long array, boolean dest);  //oct_t* oct_add_constraint ( oct_t* m, const num_t* tab, bool destructive)
  static native long J_intervAssingVar(long oct, int k, long array, boolean dest);  // oct_t* oct_inter_assign_variable ( oct_t* m, var_t x, const num_t* tab,bool destructive
  static native long J_intervSubstituteVar(long oct, int x, long array, boolean dest);  //oct_t* oct_inter_substitute_variable ( oct_t* m, var_t x, const num_t* tab, bool destructive)
  static native long J_intervAddConstraint(long oct, long array, boolean dest);  //oct_t* oct_inter_add_constraint ( oct_t* m, const num_t* tab, bool destructive)

  /* change of dimensions */
  static native long J_addDimenensionAndEmbed(long oct, int k, boolean dest);    //oct_t* oct_add_dimensions_and_embed( oct_t* m, var_t dimsup, bool destructive)
  static native long J_addDimenensionAndProject(long oct, int k, boolean dest);  //oct_t* oct_add_dimensions_and_project( oct_t* m, var_t dimsup, bool destructive)
  static native long J_removeDimension(long oct, int k, boolean dest);      //oct_t*  oct_remove_dimensions( oct_t* m, var_t dimsup, bool destructive)

  // TODO implement rest of the functions

  static native void J_print(long oct);    //void oct_print (const oct_t* m)
  static native void J_printNum(long numArr, int k);
  static native double J_getValueFor(long oct, long valI, long valJ);

  /* For debuggin purposes */
  static native long getRandomOct();
}