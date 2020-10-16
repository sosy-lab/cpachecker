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
 * This interface is provided as a shortcut, so that other CPAs' strengthen
 * operator can check whether one abstract state represents some kind of
 * "target" or "error" abstract state without needing to know more about the state
 * (especially without knowing its actual type).
 */
public interface Targetable {

  boolean isTarget();

  /**
   * Return more information about the violated property. Example: "assert statement in line X"
   *
   * @return A set of violated properties, may be empty if no information is available.
   * @throws IllegalStateException if {@link #isTarget()} returns false
   */
  @NonNull
  Set<Property> getViolatedProperties() throws IllegalStateException;
}
