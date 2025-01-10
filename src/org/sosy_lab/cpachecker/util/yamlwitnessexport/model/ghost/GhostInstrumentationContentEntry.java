// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class GhostInstrumentationContentEntry {

  @JsonProperty("ghost_variables")
  private final GhostVariablesEntry variables;

  @JsonProperty("ghost_updates")
  private final GhostUpdatesEntry updates;

  public GhostInstrumentationContentEntry(
      @JsonProperty("ghost_variables") GhostVariablesEntry pVariables,
      @JsonProperty("ghost_updates") GhostUpdatesEntry pUpdates) {
    variables = pVariables;
    updates = pUpdates;
  }

  public GhostVariablesEntry getVariables() {
    return variables;
  }

  public GhostUpdatesEntry getUpdates() {
    return updates;
  }
}
