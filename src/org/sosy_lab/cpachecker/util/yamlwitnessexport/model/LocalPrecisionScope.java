// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

@JsonDeserialize(using = JsonDeserializer.None.class)
public final class LocalPrecisionScope extends PrecisionScope {

  public static final String LOCATION_TYPE_IDENTIFIER = "location";

  @JsonProperty("location")
  private final LocationRecord location;

  public LocalPrecisionScope(@JsonProperty("location") LocationRecord pLocation) {
    super(LOCATION_TYPE_IDENTIFIER);
    location = pLocation;
  }

  public LocationRecord getLocation() {
    return location;
  }

  /**
   * Returns the nodes at which the statement at the location starts. A location without a column
   * describes the first statement at its line.
   */
  public ImmutableSet<CFANode> startNodesIn(AstCfaRelation pAstCfaRelation) {
    return pAstCfaRelation
        .getTightestStatementForStarting(location.getLine(), location.getColumn())
        .map(ASTElement::startNodes)
        .orElse(ImmutableSet.of());
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof LocalPrecisionScope other && location.equals(other.location);
  }

  @Override
  public int hashCode() {
    return location.hashCode();
  }
}
