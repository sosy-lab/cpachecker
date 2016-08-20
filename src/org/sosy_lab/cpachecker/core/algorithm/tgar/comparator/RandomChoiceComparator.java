/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.tgar.comparator;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Comparator;
import java.util.Random;

import javax.annotation.Nullable;

public class RandomChoiceComparator implements Comparator<ARGState> {

  private final Random randomGenerator = new Random(31);

  /**
   * @param pLhs
   * @param pRhs

   * @return  1 if rhs should be before lhs
   * @return -1 if lhs should be before rhs
   * @return  0 otherwise
   */
  @Override
  public int compare(ARGState pLhs, ARGState pRhs) {
    final int rnd = randomGenerator.nextInt(3); // 0,1,2
    return rnd-1;
  }

}
