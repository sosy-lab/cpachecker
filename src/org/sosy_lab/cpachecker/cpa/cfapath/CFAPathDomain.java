// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class CFAPathDomain extends FlatLatticeDomain {

  private static final CFAPathDomain sDomainInstance = new CFAPathDomain();

  public static CFAPathDomain getInstance() {
    return sDomainInstance;
  }

  public CFAPathDomain() {
    super(CFAPathTopState.getInstance());
  }
}
