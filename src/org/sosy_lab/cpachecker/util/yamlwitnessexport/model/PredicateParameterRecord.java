// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Parameter for memory-safety predicate definition. E.g.
 *
 * <pre>{@code
 * parameters:
 *   - name: "start"
 *     type: "struct sll *"
 *   - name: "end"
 *     type: "struct sll *"
 * }</pre>
 */
public class PredicateParameterRecord {

  @JsonProperty("name")
  private final String name;

  /** The parameter's {@code type} string, for example {@code struct sll *}. */
  @JsonProperty("type")
  private final String type;

  public PredicateParameterRecord(
      @JsonProperty("name") String pName, @JsonProperty("type") String pType) {
    name = pName;
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
    return pOther instanceof PredicateParameterRecord pOtherPredicateParameterRecord
        && name.equals(pOtherPredicateParameterRecord.getName())
        && type.equals(pOtherPredicateParameterRecord.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, type);
  }

  @Override
  public String toString() {
    return "(" + name + ", " + type + ")";
  }
}
