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


public class SMGHeapAbstractionThreshold {

  private final int equalThreshold;
  private final int entailThreshold;
  private final int incombarableThreshold;

  public SMGHeapAbstractionThreshold(int pEqualThreshold, int pEntailThreshold,
      int pIncombarableThreshold) {
    super();
    equalThreshold = pEqualThreshold;
    entailThreshold = pEntailThreshold;
    incombarableThreshold = pIncombarableThreshold;
  }

  public int getEqualThreshold() {
    return equalThreshold;
  }

  public int getEntailThreshold() {
    return entailThreshold;
  }

  public int getIncombarableThreshold() {
    return incombarableThreshold;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entailThreshold;
    result = prime * result + equalThreshold;
    result = prime * result + incombarableThreshold;
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
    SMGHeapAbstractionThreshold other = (SMGHeapAbstractionThreshold) obj;
    if (entailThreshold != other.entailThreshold) {
      return false;
    }
    if (equalThreshold != other.equalThreshold) {
      return false;
    }
    if (incombarableThreshold != other.incombarableThreshold) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SMGHeapAbstractionThreshold [equalThreshold=" + equalThreshold + ", entailThreshold="
        + entailThreshold + ", incombarableThreshold=" + incombarableThreshold + "]";
  }
}