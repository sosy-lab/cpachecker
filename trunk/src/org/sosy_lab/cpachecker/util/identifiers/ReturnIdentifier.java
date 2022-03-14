// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class ReturnIdentifier extends VariableIdentifier implements GeneralIdentifier {

  private static Map<Integer, ReturnIdentifier> instances;

  private ReturnIdentifier(String pNm, CType pTp, int pDereference) {
    super(pNm, pTp, pDereference);
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public String toLog() {
    return "r";
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return null;
  }

  public static ReturnIdentifier getInstance(int d) {
    if (instances == null) {
      instances = new HashMap<>();
    }
    if (instances.containsKey(d)) {
      return instances.get(d);
    } else {
      ReturnIdentifier id = new ReturnIdentifier("__returnId", null, d);
      instances.put(d, id);
      return id;
    }
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO == this) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public AbstractIdentifier cloneWithDereference(int pDereference) {
    return getInstance(pDereference);
  }
}
