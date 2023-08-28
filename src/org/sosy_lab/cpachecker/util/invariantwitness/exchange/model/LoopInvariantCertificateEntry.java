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
import java.util.Objects;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TargetRecord;

@Immutable
public class LoopInvariantCertificateEntry extends AbstractEntry {

  private static final String LOOP_INVARIANT_CERTIFICATE_ENTRY_IDENTIFIER = "loop-invariant_certificate";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("target")
  private final TargetRecord target;

  @JsonProperty("certification")
  private final InformationRecord certification;

  public LoopInvariantCertificateEntry(
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("target") TargetRecord target,
      @JsonProperty("certification") InformationRecord certification) {
    super(LOOP_INVARIANT_CERTIFICATE_ENTRY_IDENTIFIER);
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
    return super.equals(obj)
        && obj instanceof LoopInvariantCertificateEntry other
        && Objects.equals(certification, other.certification)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(target, other.target);
  }
}
