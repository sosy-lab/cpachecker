/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.join.SMGLevelMapping.SMGJoinLevel;

import java.util.HashMap;

public class SMGLevelMapping extends HashMap<SMGJoinLevel, Integer> {

  private static final long serialVersionUID = 744358511538485682L;

  public static SMGLevelMapping createDefaultLevelMap() {
    SMGLevelMapping result = new SMGLevelMapping();
    result.put(new SMGJoinLevel(0, 0), 0);
    return result;
  }

  public static class SMGJoinLevel {

    private final int level1;
    private final int level2;

    private SMGJoinLevel(int pLevel1, int pLevel2) {
      super();
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
      final int prime = 31;
      int result = 1;
      result = prime * result + level1;
      result = prime * result + level2;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SMGJoinLevel other = (SMGJoinLevel) obj;
      if (level1 != other.level1) {
        return false;
      }
      if (level2 != other.level2) {
        return false;
      }
      return true;
    }
  }
}