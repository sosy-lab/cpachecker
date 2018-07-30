/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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