// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

public enum YAMLWitnessVersion {
  V2,
  V2d1;

  @Override
  public String toString() {
    return switch (this) {
      case V2 -> "2.0";
      case V2d1 -> "2.1";
    };
  }

  public static YAMLWitnessVersion fromString(String pVersion)
      throws InvalidConfigurationException {
    return switch (pVersion) {
      case "2.0" -> V2;
      case "2.1" -> V2d1;
      default -> throw new InvalidConfigurationException("The version is not recognized.");
    };
  }
}
