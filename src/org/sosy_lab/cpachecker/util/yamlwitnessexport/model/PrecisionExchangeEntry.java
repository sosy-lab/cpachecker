// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;

public record PrecisionExchangeEntry(
    @JsonProperty("format") YAMLWitnessExpressionType format,
    @JsonProperty("scope") PrecisionScope scope,
    @JsonProperty("type") PrecisionType type,
    @JsonProperty("values") List<String> values) {}
