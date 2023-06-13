// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import java.util.HashMap;

@SuppressWarnings("checkstyle:IllegalType") // TODO: use composition instead of inheritance
public class SMGLevelMapping extends HashMap<SMGJoinLevel, Integer> {

  private static final long serialVersionUID = 744358511538485682L;

  public static SMGLevelMapping createDefaultLevelMap() {
    SMGLevelMapping result = new SMGLevelMapping();
    result.put(new SMGJoinLevel(0, 0), 0);
    return result;
  }
}
