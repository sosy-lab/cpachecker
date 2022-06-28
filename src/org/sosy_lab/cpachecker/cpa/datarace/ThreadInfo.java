// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import org.checkerframework.checker.nullness.qual.Nullable;

class ThreadInfo {

  private final @Nullable ThreadInfo parent;
  private final String name;
  private final int epoch;
  private final int creationEpoch;

  ThreadInfo(@Nullable ThreadInfo pParent, String pName, int pEpoch, int pCreationEpoch) {
    parent = pParent;
    name = pName;
    epoch = pEpoch;
    creationEpoch = pCreationEpoch;
  }

  @Nullable ThreadInfo getParent() {
    return parent;
  }

  String getName() {
    return name;
  }

  int getEpoch() {
    return epoch;
  }

  int getCreationEpoch() {
    return creationEpoch;
  }

  ThreadInfo increaseEpoch() {
    return new ThreadInfo(parent, name, epoch + 1, creationEpoch);
  }
}
