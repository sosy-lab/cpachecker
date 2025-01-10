// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import java.util.List;

public class GhostUpdatesEntry {

  private final List<GhostUpdateRecord> updates;

  public GhostUpdatesEntry(List<GhostUpdateRecord> pUpdates) {
    updates = pUpdates;
  }

  public List<GhostUpdateRecord> getUpdates() {
    return updates;
  }
}
