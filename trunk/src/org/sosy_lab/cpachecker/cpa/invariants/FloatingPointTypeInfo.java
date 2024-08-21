// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

public enum FloatingPointTypeInfo implements TypeInfo {
  FLOAT {

    @Override
    public boolean isSigned() {
      return true;
    }

    @Override
    public Float getMinValue() {
      return Float.NEGATIVE_INFINITY;
    }

    @Override
    public Float getMaxValue() {
      return Float.POSITIVE_INFINITY;
    }

    @Override
    public String abbrev() {
      return "float";
    }
  },

  DOUBLE {

    @Override
    public boolean isSigned() {
      return true;
    }

    @Override
    public Double getMinValue() {
      return Double.NEGATIVE_INFINITY;
    }

    @Override
    public Double getMaxValue() {
      return Double.POSITIVE_INFINITY;
    }

    @Override
    public String abbrev() {
      return "double";
    }
  }
}
