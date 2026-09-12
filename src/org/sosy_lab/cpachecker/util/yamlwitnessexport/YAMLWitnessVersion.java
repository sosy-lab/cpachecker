// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

public enum YAMLWitnessVersion {
  V2,
  V2d1,
  V2d2;

  @Override
  public String toString() {
    return switch (this) {
      case V2 -> "2.0";
      case V2d1 -> "2.1";
      case V2d2 -> "2.2";
    };
  }

  /**
   * The kinds of information which the invariant set of a correctness witness in this format
   * version may contain.
   *
   * <p>Version 2.0 only knows loop and location invariants, transition invariants and function
   * contracts were added in version 2.1.
   *
   * @return the kinds this version supports, in the declaration order of {@link
   *     WitnessInvariantKind}
   */
  public ImmutableSet<WitnessInvariantKind> supportedInvariantKinds() {
    return switch (this) {
      case V2 ->
          Sets.immutableEnumSet(
              WitnessInvariantKind.LOOP_INVARIANT, WitnessInvariantKind.LOCATION_INVARIANT);
      case V2d1, V2d2 ->
          Sets.immutableEnumSet(
              WitnessInvariantKind.LOOP_INVARIANT,
              WitnessInvariantKind.LOCATION_INVARIANT,
              WitnessInvariantKind.LOOP_TRANSITION_INVARIANT,
              WitnessInvariantKind.LOCATION_TRANSITION_INVARIANT,
              WitnessInvariantKind.FUNCTION_CONTRACT);
    };
  }

  public static YAMLWitnessVersion fromString(String pVersion)
      throws InvalidConfigurationException {
    return switch (pVersion) {
      case "2.0" -> V2;
      case "2.1" -> V2d1;
      case "2.2" -> V2d2;
      default -> throw new InvalidConfigurationException("The version is not recognized.");
    };
  }
}
