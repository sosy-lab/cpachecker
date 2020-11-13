// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class GenericDelta implements Delta<CompatibleState> {

  private final static GenericDelta instance = new GenericDelta();

  private GenericDelta() {
  }

  @Override
  public CompatibleState apply(CompatibleState pState) {
    return pState;
  }

  @Override
  public boolean covers(Delta<CompatibleState> pDelta) {
    return pDelta == instance;
  }

  @Override
  public boolean equals(Delta<CompatibleState> pDelta) {
    return pDelta == instance;
  }

  public static GenericDelta getInstance() {
    return instance;
  }

  @Override
  public Delta<CompatibleState> add(Delta<CompatibleState> pDelta) {
    return instance;
  }

}
