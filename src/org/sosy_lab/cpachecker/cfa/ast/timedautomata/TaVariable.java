// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import java.io.Serializable;

public class TaVariable implements Serializable {

  private static final long serialVersionUID = 7684139857206868141L;
  private final String name;
  private final String automatonName;
  private final boolean isLocal;

  public TaVariable(String pName, String pAutomatonNane, boolean pIsLocal) {
    name = pName;
    automatonName = pAutomatonNane;
    isLocal = pIsLocal;
  }

  public String getName() {
    if (isLocal) {
      return automatonName + "#" + name;
    }
    return name;
  }

  public String getShortName() {
    if (isLocal) {
      return "L_" + name;
    }
    return name;
  }
}
