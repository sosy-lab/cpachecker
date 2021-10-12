// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;

public class GenericDelta implements Delta<CompatibleNode> {

  private final static GenericDelta instance = new GenericDelta();

  private GenericDelta() {
  }

  @Override
  public CompatibleNode apply(CompatibleNode pState) {
    return pState;
  }

  @Override
  public boolean covers(Delta<CompatibleNode> pDelta) {
    return pDelta == instance;
  }

  @Override
  public boolean equals(Object pDelta) {
    return pDelta == instance;
  }

  public static GenericDelta getInstance() {
    return instance;
  }

  @Override
  public Delta<CompatibleNode> add(Delta<CompatibleNode> pDelta) {
    return instance;
  }

  @Override
  public int hashCode() {
    return 31;
  }

}
