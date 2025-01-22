// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhostVariableRecord(
    @JsonProperty("name") String name,
    // variable type e.g. 'int', not to be confused with invariant type
    @JsonProperty("type") String type,
    @JsonProperty("scope") String scope,
    @JsonProperty("initial") InitialRecord initial) {}
