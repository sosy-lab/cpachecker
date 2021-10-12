// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class GlobalVariableIdentifier extends VariableIdentifier {

  public GlobalVariableIdentifier(String nm, CType t, int dereference) {
    super(nm, t, dereference);
  }

  @Override
  public GlobalVariableIdentifier cloneWithDereference(int pDereference) {
    return new GlobalVariableIdentifier(name, type, pDereference);
  }

  @Override
  public boolean isGlobal() {
    return true;
  }

  @Override
  public String toLog() {
    return "g;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralGlobalVariableIdentifier(name, dereference);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof GlobalVariableIdentifier) {
      return super.compareTo(pO);
    } else {
      return 1;
    }
  }
}
