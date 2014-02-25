/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.util.Iterator;
import java.util.Vector;

public class EliminationAnswer {

  private Vector<EAPair> pairs;
  private boolean truthValue = true; // for use when Redlog says 'false'.

  public EliminationAnswer() {
    pairs = new Vector<>();
  }

  public EliminationAnswer(boolean value) {
    this.truthValue = value;
  }

  public boolean getTruthValue() {
    return truthValue;
  }

  void setTruthValue(boolean b) {
    truthValue = b;
  }

  public void addPair(EAPair pair) {
    pairs.add(pair);
  }

  public int getNumPairs() {
    return pairs.size();
  }

  public EAPair getPair(int i) {
    return pairs.get(i);
  }

  public Iterator<EAPair> iterator() {
    return pairs.iterator();
  }

}
