/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.multigoal;

public class PathState {
  int index;
  boolean pathFound;

  public PathState(int pIndex, boolean pPathFound) {
    index = pIndex;
    pathFound = pPathFound;
  }

  @Override
  public int hashCode() {
    return 5 * index + (pathFound ? 1 : 0);
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }

    if (pObj instanceof PathState) {
      PathState other = (PathState) pObj;
      if (other.index == this.index && other.pathFound == this.pathFound) {
        return true;
      }
    }
    return false;
  }

  public int getIndex() {
    return index;
  }

  public boolean isPathFound() {
    return pathFound;
  }
  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return "Index: " + index + "; Path found: " + pathFound;
  }

}
