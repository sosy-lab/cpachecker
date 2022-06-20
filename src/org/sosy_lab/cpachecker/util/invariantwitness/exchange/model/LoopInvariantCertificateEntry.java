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
}
