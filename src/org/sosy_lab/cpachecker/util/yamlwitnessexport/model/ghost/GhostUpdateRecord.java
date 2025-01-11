// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class GhostUpdateRecord {

  @JsonProperty("location")
  private final LocationRecord location;

  @JsonProperty("updates")
  private final List<UpdatesRecord> updates;

  public GhostUpdateRecord(
      @JsonProperty("location") LocationRecord pLocation,
      @JsonProperty("updates") List<UpdatesRecord> pUpdates) {
    location = pLocation;
    updates = pUpdates;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public List<UpdatesRecord> getUpdates() {
    return updates;
  }
}
