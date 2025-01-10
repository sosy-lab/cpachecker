// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class GhostUpdateRecord {

  @JsonProperty("location")
  private final LocationRecord location;

  @JsonProperty("update")
  private final UpdateRecord update;

  public GhostUpdateRecord(
      @JsonProperty("location") LocationRecord pLocation,
      @JsonProperty("update") UpdateRecord pUpdate) {
    location = pLocation;
    update = pUpdate;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public UpdateRecord getUpdate() {
    return update;
  }
}
