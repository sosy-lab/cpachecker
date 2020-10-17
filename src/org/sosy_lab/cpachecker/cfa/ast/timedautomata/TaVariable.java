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

  public TaVariable(String pName, String pAutomatonName, boolean pIsLocal) {
    name = pName;
    automatonName = pAutomatonName;
    isLocal = pIsLocal;
  }

  /**
   * This method does the same as the constructor. However, creating instances of this class should
   * be kept in this package, thus, this method exists to abstract from the constructor.
   */
  public static TaVariable createDummyVariable(
      String pName, String pAutomatonName, boolean pIsLocal) {
    return new TaVariable(pName, pAutomatonName, pIsLocal);
  }

  public String getName() {
    if (isLocal) {
      return automatonName + "#" + name;
    }
    return name;
  }

  public String getShortName() {
    if (isLocal) {
      return "[" + name + "]";
    }
    return name;
  }

  public boolean isLocal() {
    return isLocal;
  }
}
