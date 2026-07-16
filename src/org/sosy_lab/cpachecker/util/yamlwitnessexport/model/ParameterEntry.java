// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.Immutable;

/**
 * E.g.:
 *
 * <pre>{@code
 * - name: "start"
 *   type: "struct sll *"
 * }</pre>
 */
@Immutable
public class ParameterEntry {

  @JsonProperty("name")
  private final String name;

  @JsonProperty("type")
  private final String type;

  // TODO: look into how we export this! Here or from its sequence?
  public ParameterEntry(@JsonProperty("name") String pName, @JsonProperty("type") String pType) {
    checkArgument(!checkNotNull(pName).isEmpty(), "Empty parameter names make no sense");
    name = pName;
    checkArgument(!checkNotNull(pType).isEmpty(), "Empty parameter type Strings make no sense");
    type = pType;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof ParameterEntry pOtherParameterEntry
        && type.equals(pOtherParameterEntry.type)
        && name.equals(pOtherParameterEntry.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, type);
  }

  @Override
  public String toString() {
    return "ParameterRecord [name=" + name + ", type=" + type + "]";
  }
}
