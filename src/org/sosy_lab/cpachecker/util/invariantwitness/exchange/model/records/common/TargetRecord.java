// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class TargetRecord {
  @JsonProperty("uuid")
  private final String uuid;

  @JsonProperty("type")
  private final String type;

  @JsonProperty("file_hash")
  private final String fileHash;

  public TargetRecord(
      @JsonProperty("uuid") String pUuid,
      @JsonProperty("type") String pType,
      @JsonProperty("file_hash") String pFileHash) {
    uuid = pUuid;
    fileHash = pFileHash;
    type = pType;
  }

  public String getUuid() {
    return uuid;
  }

  public String getType() {
    return type;
  }

  public String getFileHash() {
    return fileHash;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fileHash == null) ? 0 : fileHash.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof TargetRecord)) {
      return false;
    }
    TargetRecord other = (TargetRecord) obj;
    if (fileHash == null) {
      if (other.fileHash != null) {
        return false;
      }
    } else if (!fileHash.equals(other.fileHash)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    if (uuid == null) {
      if (other.uuid != null) {
        return false;
      }
    } else if (!uuid.equals(other.uuid)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "TargetRecord{"
        + " uuid='"
        + getUuid()
        + "'"
        + ", type='"
        + getType()
        + "'"
        + ", file_hash='"
        + getFileHash()
        + "'"
        + "}";
  }
}
