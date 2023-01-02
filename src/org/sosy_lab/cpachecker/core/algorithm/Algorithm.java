// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface Algorithm {

  /**
   * Run the algorithm on the given set of abstract states and the given waitlist.
   *
   * @param reachedSet Input.
   * @return information about how reliable the result is
   * @throws CPAException may be thrown by implementors
   * @throws InterruptedException may be thrown by implementors
   */
  AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException;

  /**
   * This class serves as an indication how a result produced by an {@link Algorithm} should be
   * interpreted. It is defined as:
   *
   * <ul>
   *   <li>if PROPERTY_CHECKED is false, answering TRUE/FALSE does not make sense
   *   <li>if SOUND is false, any proof should be interpreted as potentially flawed and ignored
   *   <li>if PRECISE is false, any counterexample found should be interpreted as potentially flawed
   *       and ignored
   * </ul>
   *
   * If SOUND and PRECISE are true, this means that the algorithm instance to its best knowledge
   * produces correct proofs and counterexamples. However, this should not be interpreted as a 100%
   * guarantee, as there may be further reasons for unsoundness or imprecision that are
   * out-of-control of the algorithm. For example, PRECISE does not necessarily mean that a
   * counterexample has been cross-checked by concrete interpretation.
   */
  final class AlgorithmStatus {
    private final boolean propertyChecked;
    private final boolean isSound;
    private final boolean isPrecise;

    public static final AlgorithmStatus NO_PROPERTY_CHECKED =
        new AlgorithmStatus(false, false, false);
    public static final AlgorithmStatus SOUND_AND_PRECISE = new AlgorithmStatus(true, true, true);
    public static final AlgorithmStatus UNSOUND_AND_PRECISE =
        new AlgorithmStatus(true, false, true);
    public static final AlgorithmStatus SOUND_AND_IMPRECISE =
        new AlgorithmStatus(true, true, false);
    public static final AlgorithmStatus UNSOUND_AND_IMPRECISE =
        new AlgorithmStatus(true, false, false);

    private AlgorithmStatus(boolean pPropertyChecked, boolean pIsSound, boolean pIsPrecise) {
      propertyChecked = pPropertyChecked;
      isSound = pIsSound && pPropertyChecked;
      isPrecise = pIsPrecise && pPropertyChecked;
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where both SOUND and PRECISE are a
     * *conjunction* of this instance's and the other's fields.
     */
    @CheckReturnValue
    public AlgorithmStatus update(AlgorithmStatus other) {
      return new AlgorithmStatus(
          propertyChecked && other.propertyChecked,
          isSound && other.isSound,
          isPrecise && other.isPrecise);
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where SOUND is as given, and PRECISE is as
     * in this instance.
     */
    @CheckReturnValue
    public AlgorithmStatus withSound(boolean pIsSound) {
      return new AlgorithmStatus(propertyChecked, pIsSound, isPrecise);
    }

    /**
     * Create a new instance of {@link AlgorithmStatus} where PRECISE is as given, and SOUND is as
     * in this instance.
     */
    @CheckReturnValue
    public AlgorithmStatus withPrecise(boolean pIsPrecise) {
      return new AlgorithmStatus(propertyChecked, isSound, pIsPrecise);
    }

    public boolean wasPropertyChecked() {
      return propertyChecked;
    }

    public boolean isSound() {
      return isSound;
    }

    public boolean isPrecise() {
      return isPrecise;
    }

    @Override
    public int hashCode() {
      return Objects.hash(propertyChecked, isSound, isPrecise);
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
      return propertyChecked == other.propertyChecked
          && isSound == other.isSound
          && isPrecise == other.isPrecise;
    }

    @Override
    public String toString() {
      if (!propertyChecked) {
        return "AlgorithmStatus [no property checked]";
      }
      return "AlgorithmStatus [isSound=" + isSound + ", isPrecise=" + isPrecise + "]";
    }
  }

  interface AlgorithmFactory {
    Algorithm newInstance();
  }
}
