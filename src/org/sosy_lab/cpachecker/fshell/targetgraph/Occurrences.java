/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.targetgraph;

import java.util.HashMap;
import java.util.Map;

public class Occurrences {

  Map<Edge, Integer> mOccurrences = new HashMap<Edge, Integer>();

  public Occurrences() {

  }

  public void decrement(Edge pEdge) {
    if (mOccurrences.containsKey(pEdge)) {
      int lCurrentValue = mOccurrences.get(pEdge);

      lCurrentValue--;

      if (lCurrentValue < 0) {
        lCurrentValue = 0;
      }

      mOccurrences.put(pEdge, lCurrentValue);
    }
    else {
      throw new RuntimeException();
    }
  }

  public int increment(Edge pEdge) {
    int lCurrentValue = 0;

    if (mOccurrences.containsKey(pEdge)) {
      lCurrentValue = mOccurrences.get(pEdge);
    }

    lCurrentValue++;

    mOccurrences.put(pEdge, lCurrentValue);

    return lCurrentValue;
  }

}
