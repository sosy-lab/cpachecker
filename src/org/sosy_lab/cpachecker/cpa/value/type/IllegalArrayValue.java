// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

/**
 * This class is created for illegal array values, e.g. int a[] = new int[-1];
 */
public class IllegalArrayValue extends ArrayValue {

  private static final long serialVersionUID = 150716599436683060L;
  public IllegalArrayValue(JArrayType pType) {
    super(pType, 0);
  }
}
