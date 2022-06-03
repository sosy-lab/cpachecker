// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.ComparisonChain;

/** a pair f two integers */
public class SMGJoinLevel implements Comparable<SMGJoinLevel> {

  private final int level1;
  private final int level2;

  SMGJoinLevel(int pLevel1, int pLevel2) {
    level1 = pLevel1;
    level2 = pLevel2;
  }

  public static SMGJoinLevel valueOf(int pLevel1, int pLevel2) {
    return new SMGJoinLevel(pLevel1, pLevel2);
  }

  public int getLevel1() {
    return level1;
  }

  public int getLevel2() {
    return level2;
  }

  @Override
  public String toString() {
    return "SMGJoinLevel [level1=" + level1 + ", level2=" + level2 + "]";
  }

  @Override
  public int hashCode() {
    return 31 * level1 + level2;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGJoinLevel)) {
      return false;
    }
    SMGJoinLevel other = (SMGJoinLevel) obj;
    return level1 == other.level1 && level2 == other.level2;
  }

  @Override
  public int compareTo(SMGJoinLevel other) {
    return ComparisonChain.start()
        .compare(level1, other.level1)
        .compare(level2, other.level2)
        .result();
  }
}
