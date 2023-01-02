// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This interface is provided as a shortcut, so that other CPAs' strengthen operator can check
 * whether one abstract state represents some kind of "target" or "error" abstract state without
 * needing to know more about the state (especially without knowing its actual type).
 */
public interface Targetable {

  boolean isTarget();

  /**
   * Return more information about why this state is a target state, e.g., the violated property.
   * Currently this information is used to present a summary of the result to users of CPachecker
   * and should thus be a human-readable string like "assert statement in line X".
   *
   * @return A set of {@link TargetInformation} instances, may be empty if no information is
   *     available.
   * @throws IllegalStateException if {@link #isTarget()} returns false
   */
  @NonNull
  Set<TargetInformation> getTargetInformation() throws IllegalStateException;

  /**
   * This interface represents information about target states.
   *
   * <p>Instances of this interface...
   *
   * <ul>
   *   <li>MUST override the {@link #toString()} method to provide a human-readable description!
   *   <li>MIGHT override the {@link Object#equals(Object)} method! (and of course {@link
   *       Object#hashCode()})
   */
  interface TargetInformation {

    /** Return the textual description for humans. */
    @Override
    String toString();
  }
}
