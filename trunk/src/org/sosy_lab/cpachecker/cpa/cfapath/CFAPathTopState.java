// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cfapath;

import java.util.Collections;
import java.util.Set;

public class CFAPathTopState implements CFAPathState {

  private static final CFAPathTopState sInstance = new CFAPathTopState();
  private static final Set<CFAPathTopState> sSingleton = Collections.singleton(sInstance);

  public static CFAPathTopState getInstance() {
    return sInstance;
  }

  public static Set<CFAPathTopState> getSingleton() {
    return sSingleton;
  }

  private CFAPathTopState() {}
}
