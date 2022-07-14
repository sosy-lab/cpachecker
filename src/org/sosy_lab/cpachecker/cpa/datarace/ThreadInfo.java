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
  private final int currentIndex;

  ThreadInfo(
      @Nullable ThreadInfo pParent,
      String pName,
      int pEpoch,
      int pCreationEpoch,
      int pCurrentIndex) {
    parent = pParent;
    name = pName;
    epoch = pEpoch;
    creationEpoch = pCreationEpoch;
    currentIndex = pCurrentIndex;
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

  int getCurrentIndex() {
    return currentIndex;
  }

  ThreadInfo increaseEpoch() {
    return new ThreadInfo(parent, name, epoch + 1, creationEpoch, currentIndex);
  }

  ThreadInfo increaseIndex() {
    return new ThreadInfo(parent, name, epoch, creationEpoch, currentIndex + 1);
  }
}
