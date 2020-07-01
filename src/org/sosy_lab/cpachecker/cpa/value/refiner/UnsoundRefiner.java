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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public interface UnsoundRefiner extends Refiner {

  /**
   * Any unsound refiner, like, e.g., the {@link ValueAnalysisImpactRefiner}
   * whose refinement procedure leaves the coverage relation in an inconsistent
   * state, must ensure that a complete re-exploration of the state-space must
   * be performed before finishing the analysis.
   *
   * To this end, all states except the root state must be removed from the
   * reached set, and a valid precision must be put in place, e.g. by calling
   * the respective {@link ARGReachedSet#removeSubtree(ARGState)} method.
   *
   */
  void forceRestart(ReachedSet reached) throws InterruptedException;
}
