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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import com.google.common.collect.BiMap;

@Options(prefix="cpa.octagon")
public class OctagonManager {

  @Option(name="library", toUppercase=true, values={"INT", "FLOAT"},
      description="with this option the used number representation in the"
          + " library can be changed between floats and ints. Default is int")
  private String library = "INT";

  private static OctagonManager singleton = null;

  public static boolean init(Configuration config) throws InvalidConfigurationException {
    if (singleton == null) {
      singleton = new OctagonManager(config);
      return singleton.initLibrary();
    } else {
      return singleton.initLibrary();
    }
  }

  private boolean initLibrary() {
    if (library.equals("INT")) {
      return OctIntWrapper.J_init();
    } else {
      return OctFloatWrapper.J_init();
    }
  }

  /* Initialization */
  private OctagonManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  /* num handling function*/

  /* allocate new space for num array and init*/
  public static NumArray init_num_t (int n) {
    if (singleton.library.equals("INT")) {
      return new NumArray(OctIntWrapper.J_init_n(n));
    } else if (singleton.library.equals("FLOAT")) {
      return new NumArray(OctFloatWrapper.J_init_n(n));
    }
    return new NumArray(OctIntWrapper.J_init_n(n));
  }

  /* num copy */
  public static void num_set(NumArray n1, NumArray n2) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_num_set(n1.getArray(), n2.getArray());
      } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_num_set(n1.getArray(), n2.getArray());
    }
  }

  public static Octagon set_bounds(Octagon oct, int pos, NumArray lower, NumArray upper) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_set_bounds(oct.getOctId(), pos, lower.getArray(), upper.getArray(), false));
      } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_set_bounds(oct.getOctId(), pos, lower.getArray(), upper.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_set_bounds(oct.getOctId(), pos, lower.getArray(), upper.getArray(), false));
  }

  /* set int */
  public static void num_set_int(NumArray n, int pos, int i) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_num_set_int(n.getArray(), pos, i);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_num_set_int(n.getArray(), pos, i);
    }
  }
  /* set float */
  public static void num_set_float(NumArray n, int pos, double d) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_num_set_float(n.getArray(), pos, d);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_num_set_float(n.getArray(), pos, d);
    }
  }
  /* set infinity */
  public static void num_set_inf(NumArray n, int pos) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_num_set_inf(n.getArray(), pos);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_num_set_inf(n.getArray(), pos);
    }
  }

  public static long num_get_int(NumArray n, int pos) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_num_get_int(n.getArray(), pos);
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_num_get_int(n.getArray(), pos);
    }
    return OctIntWrapper.J_num_get_int(n.getArray(), pos);
  }

  public static double num_get_float(NumArray n, int pos) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_num_get_float(n.getArray(), pos);
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_num_get_float(n.getArray(), pos);
    }
    return OctIntWrapper.J_num_get_float(n.getArray(), pos);
  }

  public static boolean num_infty(NumArray n, int pos) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_num_infty(n.getArray(), pos);
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_num_infty(n.getArray(), pos);
    }
    return OctIntWrapper.J_num_infty(n.getArray(), pos);
  }

  public static void num_clear_n(NumArray n, int size) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_num_clear_n(n.getArray(), size);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_num_clear_n(n.getArray(), size);
    }
  }

  /* Octagon handling functions */

  /* Octagon Creation */
  public static Octagon empty(int n) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_empty(n));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_empty(n));
    }
    return new Octagon(OctIntWrapper.J_empty(n));
  }

  public static Octagon universe(int n) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_universe(n));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_universe(n));
    }
    return new Octagon(OctIntWrapper.J_universe(n));
  }
  static void free(Long oct) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_free(oct);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_free(oct);
    }
  }

  public static Octagon copy(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_copy(oct.getOctId()));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_copy(oct.getOctId()));
    }
    return new Octagon(OctIntWrapper.J_copy(oct.getOctId()));
  }

  public static Octagon full_copy(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_full_copy(oct.getOctId()));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_full_copy(oct.getOctId()));
    }
    return new Octagon(OctIntWrapper.J_full_copy(oct.getOctId()));
  }

  /* Query Functions */
  public static int dimension(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_dimension(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_dimension(oct.getOctId());
    }
    return OctIntWrapper.J_dimension(oct.getOctId());
  }

  public static int nbconstraints(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_nbconstraints(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_nbconstraints(oct.getOctId());
    }
    return OctIntWrapper.J_nbconstraints(oct.getOctId());
  }

  /* Test Functions */
  public static boolean isEmpty(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isEmpty(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isEmpty(oct.getOctId());
    }
    return OctIntWrapper.J_isEmpty(oct.getOctId());
  }

  public static int isEmptyLazy(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isEmptyLazy(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isEmptyLazy(oct.getOctId());
    }
    return OctIntWrapper.J_isEmptyLazy(oct.getOctId());
  }

  public static boolean isUniverse(Octagon oct) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isUniverse(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isUniverse(oct.getOctId());
    }
    return OctIntWrapper.J_isUniverse(oct.getOctId());
  }

  public static boolean isIncludedIn(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isIncludedIn(oct1.getOctId(), oct2.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isIncludedIn(oct1.getOctId(), oct2.getOctId());
    }
    return OctIntWrapper.J_isIncludedIn(oct1.getOctId(), oct2.getOctId());
  }

  public static int isIncludedInLazy(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isIncludedInLazy(oct1.getOctId(), oct2.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isIncludedInLazy(oct1.getOctId(), oct2.getOctId());
    }
    return OctIntWrapper.J_isIncludedInLazy(oct1.getOctId(), oct2.getOctId());
  }

  public static boolean isEqual(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isEqual(oct1.getOctId(), oct2.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isEqual(oct1.getOctId(), oct2.getOctId());
    }
    return OctIntWrapper.J_isEqual(oct1.getOctId(), oct2.getOctId());
  }

  public static int isEqualLazy(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isEqualLazy(oct1.getOctId(), oct2.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isEqualLazy(oct1.getOctId(), oct2.getOctId());
    }
    return OctIntWrapper.J_isEqualLazy(oct1.getOctId(), oct2.getOctId());
  }

  public static boolean isIn(Octagon oct1, NumArray array) {
    if (singleton.library.equals("INT")) {
      return OctIntWrapper.J_isIn(oct1.getOctId(), array.getArray());
    } else if (singleton.library.equals("FLOAT")) {
      return OctFloatWrapper.J_isIn(oct1.getOctId(), array.getArray());
    }
    return OctIntWrapper.J_isIn(oct1.getOctId(), array.getArray());
  }

  /* Operators */
  public static Octagon intersection(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_intersection(oct1.getOctId(), oct2.getOctId(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_intersection(oct1.getOctId(), oct2.getOctId(), false));
    }
    return new Octagon(OctIntWrapper.J_intersection(oct1.getOctId(), oct2.getOctId(), false));
  }

  public static Octagon union(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_union(oct1.getOctId(), oct2.getOctId(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_union(oct1.getOctId(), oct2.getOctId(), false));
    }
    return new Octagon(OctIntWrapper.J_union(oct1.getOctId(), oct2.getOctId(), false));
  }

  /* int widening = 0 -> OCT_WIDENING_FAST
   * int widening = 1 ->  OCT_WIDENING_ZERO
   * int widening = 2 -> OCT_WIDENING_UNIT*/
  public static Octagon widening(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_widening(oct1.getOctId(), oct2.getOctId(), false, 1));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_widening(oct1.getOctId(), oct2.getOctId(), false, 1));
    }
    return new Octagon(OctIntWrapper.J_widening(oct1.getOctId(), oct2.getOctId(), false, 1));
  }

  public static Octagon narrowing(Octagon oct1, Octagon oct2) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_narrowing(oct1.getOctId(), oct2.getOctId(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_narrowing(oct1.getOctId(), oct2.getOctId(), false));
    }
    return new Octagon(OctIntWrapper.J_narrowing(oct1.getOctId(), oct2.getOctId(), false));
  }

  /* Transfer Functions */
  public static Octagon forget(Octagon oct, int k) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_forget(oct.getOctId(), k, false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_forget(oct.getOctId(), k, false));
    }
    return new Octagon(OctIntWrapper.J_forget(oct.getOctId(), k, false));
  }

  public static Octagon assingVar(Octagon oct, int k, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_assingVar(oct.getOctId(), k, array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_assingVar(oct.getOctId(), k, array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_assingVar(oct.getOctId(), k, array.getArray(), false));
  }

  public static Octagon addBinConstraint(Octagon oct, int noOfConstraints, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_addBinConstraints(oct.getOctId(), noOfConstraints, array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_addBinConstraints(oct.getOctId(), noOfConstraints, array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_addBinConstraints(oct.getOctId(), noOfConstraints, array.getArray(), false));
  }

  public static Octagon substituteVar(Octagon oct, int x, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_substituteVar(oct.getOctId(), x, array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_substituteVar(oct.getOctId(), x, array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_substituteVar(oct.getOctId(), x, array.getArray(), false));
  }

  public static Octagon addConstraint(Octagon oct, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_addConstraint(oct.getOctId(), array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_addConstraint(oct.getOctId(), array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_addConstraint(oct.getOctId(), array.getArray(), false));
  }

  public static Octagon intervAssingVar(Octagon oct, int k, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_intervAssingVar(oct.getOctId(), k, array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_intervAssingVar(oct.getOctId(), k, array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_intervAssingVar(oct.getOctId(), k, array.getArray(), false));
  }

  public static Octagon intervSubstituteVar(Octagon oct, int x, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_intervSubstituteVar(oct.getOctId(), x, array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_intervSubstituteVar(oct.getOctId(), x, array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_intervSubstituteVar(oct.getOctId(), x, array.getArray(), false));
  }

  public static Octagon intervAddConstraint(Octagon oct, NumArray array) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_intervAddConstraint(oct.getOctId(), array.getArray(), false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_intervAddConstraint(oct.getOctId(), array.getArray(), false));
    }
    return new Octagon(OctIntWrapper.J_intervAddConstraint(oct.getOctId(), array.getArray(), false));
  }

  /* change of dimensions */
  public static Octagon addDimensionAndEmbed(Octagon oct, int k) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_addDimenensionAndEmbed(oct.getOctId(), k, false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_addDimenensionAndEmbed(oct.getOctId(), k, false));
    }
    return new Octagon(OctIntWrapper.J_addDimenensionAndEmbed(oct.getOctId(), k, false));
  }

  public static Octagon addDimensionAndProject(Octagon oct, int k) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_addDimenensionAndProject(oct.getOctId(), k, false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_addDimenensionAndProject(oct.getOctId(), k, false));
    }
    return new Octagon(OctIntWrapper.J_addDimenensionAndProject(oct.getOctId(), k, false));
  }

  public static Octagon removeDimension(Octagon oct, int k) {
    if (singleton.library.equals("INT")) {
      return new Octagon(OctIntWrapper.J_removeDimension(oct.getOctId(), k, false));
    } else if (singleton.library.equals("FLOAT")) {
      return new Octagon(OctFloatWrapper.J_removeDimension(oct.getOctId(), k, false));
    }
    return new Octagon(OctIntWrapper.J_removeDimension(oct.getOctId(), k, false));
  }

  public static void printNum(NumArray arr, int size) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_printNum(arr.getArray(), size);
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_printNum(arr.getArray(), size);
    }
  }

  public static void printOct(Octagon oct) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_print(oct.getOctId());
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_print(oct.getOctId());
    }
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
      OctagonManager.get_bounds(oct, i, lower, upper);
      if (OctagonManager.num_infty(lower, 0)) {
        str.append("-INFINITY, ");
      } else {
        str.append(OctagonManager.num_get_float(lower, 0)*-1).append(", ");
      }
      if (OctagonManager.num_infty(upper, 0)) {
        str.append("INFINITY]\n");
      } else {
        str.append(OctagonManager.num_get_float(upper, 0)).append("]\n");
      }
    }
    OctagonManager.num_clear_n(lower, 1);
    OctagonManager.num_clear_n(upper, 1);
    return str.toString();
  }

  public static void get_bounds(Octagon oct, int id, NumArray lower, NumArray upper) {
    if (singleton.library.equals("INT")) {
      OctIntWrapper.J_get_bounds(oct.getOctId(), id, upper.getArray(), lower.getArray());
    } else if (singleton.library.equals("FLOAT")) {
      OctFloatWrapper.J_get_bounds(oct.getOctId(), id, upper.getArray(), lower.getArray());
    }
  }

  public static Pair<Long, Long> getVariableBounds(Octagon oct, int id) {
    // TODO differentiate between ints and floats
    NumArray lower = OctagonManager.init_num_t(1);
    NumArray upper = OctagonManager.init_num_t(1);

    OctagonManager.get_bounds(oct, id, lower, upper);
    Pair<Long, Long> retVal = Pair.of(OctagonManager.num_get_int(lower, 0)*-1,
                                      OctagonManager.num_get_int(upper, 0));

    OctagonManager.num_clear_n(lower, 1);
    OctagonManager.num_clear_n(upper, 1);
    return retVal;
  }
}