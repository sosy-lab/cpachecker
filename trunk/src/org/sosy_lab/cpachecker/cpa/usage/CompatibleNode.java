// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

public interface CompatibleNode extends CompatibleState {

  boolean cover(CompatibleNode node);

  default boolean hasEmptyLockSet() {
    return true;
  }

  @Override
  default CompatibleNode getCompatibleNode() {
    return this;
  }
}
