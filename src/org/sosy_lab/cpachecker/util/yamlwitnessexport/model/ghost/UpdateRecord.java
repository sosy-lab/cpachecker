// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;

public record UpdateRecord(
    @JsonProperty("variable") String variable,
    @JsonProperty("value") int value,
    @JsonProperty("format") @JsonInclude(JsonInclude.Include.NON_NULL)
        YAMLWitnessExpressionType format) {}
