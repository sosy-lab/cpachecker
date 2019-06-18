/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sl;

import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class SLHeap {

  private class SLMemoryLocation implements Comparable<SLMemoryLocation> {
    private long address; // initial memory location. e.g. x = 5 -> address of x where 5 is stored.
    private long offset;
    private String pointerName;
    private BooleanFormula value;

    private SLMemoryLocation(String pPointerName) {
      address = 1000;
      offset = 0;
      pointerName = pPointerName;
      value = null;
    }

    @Override
    public int compareTo(SLMemoryLocation pML) {
      return Long.compare(address, pML.address);
    }


  }

  private final long VALUE_UNDEFINED = 42;

  private Set<SLMemoryLocation> heap;

  public SLHeap() {
    heap = new TreeSet<>();
  }

  public void addMemoryLocation(String pPointerName) {
    SLMemoryLocation ml = new SLMemoryLocation(pPointerName);
    heap.add(ml);

  }
}
