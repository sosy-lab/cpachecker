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
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Defines a (potentially empty) array (we use a list here) of parameters consisting of names and
 * types. For example:
 *
 * <pre>{@code
 * parameters:
 *   - name: start
 *     type: "struct sll *"
 *   - name: end
 *     type: "struct sll *"
 * }</pre>
 */
public class ParameterSequence extends AbstractEntry {

  private static final String PARAMETER_SEQUENCE_ENTRY_IDENTIFIER = "parameters";

  @JsonProperty("parameters")
  private final List<ParameterEntry> parameters;

  protected ParameterSequence(@JsonProperty("parameters") List<ParameterEntry> pParameters) {
    super(PARAMETER_SEQUENCE_ENTRY_IDENTIFIER);
    parameters = pParameters;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof ParameterSequence pOtherParameterSequence
        && parameters.equals(pOtherParameterSequence.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(parameters);
  }

  @Override
  public String toString() {
    return "ParameterSequence ["
        + String.join(
            ", ",
            parameters.stream()
                .map(ParameterEntry::toString)
                .collect(ImmutableList.toImmutableList()))
        + "]";
  }
}
