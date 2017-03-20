/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.cfa.model.CFANode;


public class LineInfo implements Comparable<LineInfo> {
  private final int line;
  private final CFANode node;

  public LineInfo(int l, CFANode n){
    line = l;
    node = n;
  }

  @Override
  public String toString() {
    return "#"+Integer.toString(line)+"#";
  }

  public int getLine() {
    return line;
  }

  public CFANode getNode() {
    return node;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + line;
    result = prime * result + ((node == null) ? 0 : node.getNodeNumber());
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
    LineInfo other = (LineInfo) obj;
    if (line != other.line) {
      return false;
    }
    if (node == null) {
      if (other.node != null) {
        return false;
      }
    } else if (node.getNodeNumber() != other.node.getNodeNumber()) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(LineInfo pO) {
    int result = line - pO.line;
    if (result != 0) {
      return result;
    }
    //Some nodes can be from one line, but different nodes
    result = node.getNodeNumber() - pO.node.getNodeNumber();
    if (result != 0) {
      return result;
    }
    return 0;
  }

}
