// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TargetRecord;

@Immutable
public class LoopInvariantCertificateEntry extends AbstractEntry {

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("target")
  private final TargetRecord target;

  @JsonProperty("certification")
  private final InformationRecord certification;

  public LoopInvariantCertificateEntry(
      @JsonProperty("entry_type") String entryType,
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("target") TargetRecord target,
      @JsonProperty("certification") InformationRecord certification) {
    super(entryType);
    this.metadata = metadata;
    this.target = target;
    this.certification = certification;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public TargetRecord getTarget() {
    return target;
  }

  public InformationRecord getCertification() {
    return certification;
  }

  @Override
  public String toString() {
    return "InvariantStoreCertificateEntry{"
        + " entry_type='"
        + getEntryType()
        + "'"
        + ", metadata='"
        + getMetadata()
        + "'"
        + ", target='"
        + getTarget()
        + "'"
        + ", certification='"
        + getCertification()
        + "'"
        + "}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((certification == null) ? 0 : certification.hashCode());
    result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
    result = prime * result + ((target == null) ? 0 : target.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof LoopInvariantCertificateEntry)) {
      return false;
    }
    LoopInvariantCertificateEntry other = (LoopInvariantCertificateEntry) obj;
    if (certification == null) {
      if (other.certification != null) {
        return false;
      }
    } else if (!certification.equals(other.certification)) {
      return false;
    }
    if (metadata == null) {
      if (other.metadata != null) {
        return false;
      }
    } else if (!metadata.equals(other.metadata)) {
      return false;
    }
    if (target == null) {
      if (other.target != null) {
        return false;
      }
    } else if (!target.equals(other.target)) {
      return false;
    }
    return true;
  }
}
