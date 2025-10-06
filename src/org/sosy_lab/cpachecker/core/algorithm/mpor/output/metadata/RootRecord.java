// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record RootRecord(
    @JsonProperty("metadata") MetadataRecord pMetadata,
    @JsonProperty("algorithm_options") Map<String, Object> pAlgorithmOptions) {}
