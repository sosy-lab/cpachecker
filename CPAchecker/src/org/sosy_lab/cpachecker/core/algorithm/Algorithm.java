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

import javax.annotation.CheckReturnValue;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;

public interface Algorithm {

  /**
   * Run the algorithm on the given set of abstract states and the given waitlist.
   *
   * @param reachedSet Input.
   * @return information about how reliable the result is
   * @throws CPAException
   * @throws InterruptedException
   */
  AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException;

  /**
   * This class serves as an indication how a result produced by an {@link Algorithm}
   * should be interpreted. It is defined as:
   * - if SOUND is false, any proof should be interpreted as potentially flawed and ignored
   * - if PRECISE is false, any counterexample found should be interpreted as potentially flawed and ignored
   *
   * If SOUND and PRECISE are true, this means that the algorithm instance
   * to its best knowledge produces correct proofs and counterexamples.
   * However, this should not be interpreted as a 100% guarantee,
   * as there may be further reasons for unsoundness or imprecision
   * that are out-of-control of the algorithm.
   * For example, PRECISE does not necessarily mean that a counterexample
   * has been cross-checked by concrete interpretation.
   */
  final class AlgorithmStatus {
    private final boolean isPrecise;
    private final boolean isSound;

    public static final AlgorithmStatus SOUND_AND_PRECISE = new AlgorithmStatus(true, true);
    public static final AlgorithmStatus UNSOUND_AND_PRECISE = new AlgorithmStatus(true, false);

    private AlgorithmStatus(boolean pIsPrecise, boolean pIsSound) {
      isPrecise = pIsPrecise;
      isSound = pIsSound;
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where both SOUND and PRECISE
     * are a *conjunction* of this instance's and the other's fields.
     */
    @CheckReturnValue
    public AlgorithmStatus update(AlgorithmStatus other) {
      return new AlgorithmStatus(
          isPrecise && other.isPrecise,
          isSound && other.isSound
      );
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where SOUND is as given,
     * and PRECISE is as in this instance.
     */
    @CheckReturnValue
    public AlgorithmStatus withSound(boolean pIsSound) {
      return new AlgorithmStatus(isPrecise, pIsSound);
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where PRECISE is as given,
     * and SOUND is as in this instance.
     */
    @CheckReturnValue
    public AlgorithmStatus withPrecise(boolean pIsPrecise) {
      return new AlgorithmStatus(pIsPrecise, isSound);
    }

    public boolean isSound() {
      return isSound;
    }

    public boolean isPrecise() {
      return isPrecise;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (isPrecise ? 1231 : 1237);
      result = prime * result + (isSound ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof AlgorithmStatus)) {
        return false;
      }
      AlgorithmStatus other = (AlgorithmStatus) obj;
      return isPrecise == other.isPrecise
          && isSound == other.isSound;
    }
  }
}
