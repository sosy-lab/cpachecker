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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

public class SMGUseFieldEdge implements SMGUseGraphEdge<SMGUseFieldVertice> {

  private final SMGUseFieldVertice source;
  private final SMGUseFieldVertice target;

  public SMGUseFieldEdge(SMGUseFieldVertice pSource, SMGUseFieldVertice pTarget) {
    super();
    source = pSource;
    target = pTarget;
  }

  @Override
  public SMGUseFieldVertice getSource() {
    return source;
  }

  @Override
  public SMGUseFieldVertice getTarget() {
    return target;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result = prime * result + ((target == null) ? 0 : target.hashCode());
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
    SMGUseFieldEdge other = (SMGUseFieldEdge) obj;
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    if (target == null) {
      if (other.target != null) {
        return false;
      }
    } else if (!target.equals(other.target)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SMGUseFieldEdge [source=" + source + ", target=" + target + "]";
  }
}