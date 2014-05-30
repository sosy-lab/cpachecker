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

import org.sosy_lab.cpachecker.cpa.octagon.values.OctInterval;
import org.sosy_lab.cpachecker.util.NativeLibraries;

import com.google.common.collect.BiMap;


public class OctagonFloatManager extends OctagonManager {

  public OctagonFloatManager() {
    super();
    if (!libraryLoaded) {
      libraryLoaded = true;
      NativeLibraries.loadLibrary("JOct_float");
    }
    if (!libraryInitialized) {
      libraryInitialized = true;
      J_init();
    }
  }

  @Override
  public String print(Octagon oct, BiMap<Integer, String> map) {
    StringBuilder str = new StringBuilder();
    int dimension = dimension(oct);
    long pointer = oct.getOctId();
    str.append("Octagon (id: " + pointer + ") (dimension: " + dimension + ")\n");
    if (isEmpty(oct)) {
      str.append("[Empty]\n");
      return str.toString();
    }

    NumArray lower = init_num_t(1);
    NumArray upper = init_num_t(1);

    for (int i = 0; i < map.size(); i++) {
      str.append(" ").append(map.get(i)).append(" -> [");
      J_get_bounds(oct.getOctId(), i, upper.getArray(), lower.getArray());
      if (J_num_infty(lower.getArray(), 0)) {
        str.append("-INFINITY, ");
      } else {
        str.append(J_num_get_float(lower.getArray(), 0) * -1).append(", ");
      }
      if (J_num_infty(upper.getArray(), 0)) {
        str.append("INFINITY]\n");
      } else {
        str.append(J_num_get_float(upper.getArray(), 0)).append("]\n");
      }
    }
    J_num_clear_n(lower.getArray(), 1);
    J_num_clear_n(upper.getArray(), 1);
    return str.toString();
  }

  @Override
  public OctInterval getVariableBounds(Octagon oct, int id) {
    NumArray lower = init_num_t(1);
    NumArray upper = init_num_t(1);
    assert id < dimension(oct);
    J_get_bounds(oct.getOctId(), id, upper.getArray(), lower.getArray());
    OctInterval retVal = new OctInterval(J_num_get_float(lower.getArray(), 0) * -1,
                                         J_num_get_float(upper.getArray(), 0));
    J_num_clear_n(lower.getArray(), 1);
    J_num_clear_n(upper.getArray(), 1);
    return retVal;
  }
}
