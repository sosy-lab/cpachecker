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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.List;
import java.util.Stack;
import java.util.Vector;


public class AssumptionStack {

  private Stack<Frame> frames;

  public AssumptionStack() {
    frames = new Stack<>();
  }

  void addNewFrame(AssumptionSet as, List<Matrix> ml, Assumption a) {
    // First make a fresh COPY of the passed data.
    as = new AssumptionSet(as);
    ml = copyMatrices(ml);
    // And create the negation of the assumption.
    // This is to be applied to both as and ml at the time that this frame is
    // popped off the stack.
    a = a.not();
    // Create the new frame and add it to the stack.
    Frame f = new Frame(as, ml, a);
    frames.add(f);
  }

  /*
   * Pop a frame off the top of the stack.
   * Return null if the stack is empty.
   */
  Frame popFrame() {
    if (frames.empty()) {
      return null;
    } else {
      return frames.pop();
    }
  }

  private List<Matrix> copyMatrices(List<Matrix> ml) {
    List<Matrix> n = new Vector<>(ml.size());
    for (Matrix m : ml) {
      n.add(m.copy());
    }
    return n;
  }

  public class Frame {

    // A copy of the matrices, at the time the frame was created.
    private List<Matrix> matrices;
    // A copy of the assumption set at the time the frame was created.
    private AssumptionSet aset;
    // The assumption that should be applied to both aset and matrices when the frame is
    // popped off the stack.
    private Assumption a;

    public Frame(AssumptionSet as, List<Matrix> ml, Assumption an) {
      aset = as;
      matrices = ml;
      a = an;
    }

    public List<Matrix> getMatrices() {
      return matrices;
    }

    public AssumptionSet getAssumptionSet() {
      return aset;
    }

    public Assumption getAssumption() {
      return a;
    }

  }

}
