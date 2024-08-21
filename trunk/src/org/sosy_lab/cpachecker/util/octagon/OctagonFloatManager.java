// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_get_bounds;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_clear_n;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_get_float;
import static org.sosy_lab.cpachecker.util.octagon.OctWrapper.J_num_infty;

import com.google.common.collect.BiMap;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class OctagonFloatManager extends OctagonManager {

  public OctagonFloatManager() {
    super("JOct_float");
  }

  @Override
  public String print(Octagon oct, BiMap<Integer, MemoryLocation> map) {
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
  public OctagonInterval getVariableBounds(Octagon oct, int id) {
    NumArray lower = init_num_t(1);
    NumArray upper = init_num_t(1);
    assert id < dimension(oct);
    J_get_bounds(oct.getOctId(), id, upper.getArray(), lower.getArray());
    boolean lowerInfinite = J_num_infty(lower.getArray(), 0);
    boolean upperInfinite = J_num_infty(upper.getArray(), 0);

    OctagonInterval retVal;
    if (lowerInfinite && upperInfinite) {
      retVal = new OctagonInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    } else if (lowerInfinite) {
      retVal = new OctagonInterval(Double.NEGATIVE_INFINITY, J_num_get_float(upper.getArray(), 0));
    } else if (upperInfinite) {
      retVal =
          new OctagonInterval(J_num_get_float(lower.getArray(), 0) * -1, Double.POSITIVE_INFINITY);
    } else {
      retVal =
          new OctagonInterval(
              J_num_get_float(lower.getArray(), 0) * -1, J_num_get_float(upper.getArray(), 0));
    }

    J_num_clear_n(lower.getArray(), 1);
    J_num_clear_n(upper.getArray(), 1);
    return retVal;
  }
}
