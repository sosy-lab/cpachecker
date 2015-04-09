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
package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;

public interface Algorithm {

  /**
   * Run the algorithm on the given set of abstract states and the given waitlist.
   *
   * @param reachedSet Input.
   * @return False if the analysis was unsound (this is not the analysis result!).
   * @throws CPAException
   * @throws InterruptedException
   */
  AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException, PredicatedAnalysisPropertyViolationException;


  class AlgorithmStatus {
    private final boolean isPrecise;
    private final boolean isSound;

    public static final AlgorithmStatus SOUND_AND_COMPLETE = new AlgorithmStatus(true, true);

    public AlgorithmStatus(boolean pIsPrecise, boolean pIsSound) {
      isPrecise = pIsPrecise;
      isSound = pIsSound;
    }

    public AlgorithmStatus update(AlgorithmStatus other) {
      return new AlgorithmStatus(
          isPrecise && other.isPrecise,
          isSound && other.isSound
      );
    }

    public AlgorithmStatus updateSoundness(boolean pIsSound) {
      return new AlgorithmStatus(isPrecise, pIsSound);
    }

    public static AlgorithmStatus ofPrecise(boolean isSound) {
      return new AlgorithmStatus(true, isSound);
    }

    public boolean isSound() {
      return isSound;
    }

    public boolean isPrecise() {
      return isPrecise;
    }
  }
}
