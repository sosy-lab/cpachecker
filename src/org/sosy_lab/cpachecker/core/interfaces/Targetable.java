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
package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;

import javax.annotation.Nonnull;

/**
 * This interface is provided as a shortcut, so that other CPAs' strengthen
 * operator can check whether one abstract state represents some kind of
 * "target" or "error" abstract state without needing to know more about the state
 * (especially without knowing its actual type).
 */
public interface Targetable {

  public boolean isTarget();

  /**
   * Return a human-readable description of the violated property.
   * Example: "assert statement in line X"
   * @return A non-null String, may be empty if no description is available.
   * @throws IllegalStateException if {@link #isTarget()} returns false
   */
  public @Nonnull Set<Property> getViolatedProperties() throws IllegalStateException;
}
