// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.CSimpleDeclarationSetToSortedListConverter;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * This class is a mixin for {@link CFANode}.
 *
 * <p>Identity information is being serialized to prevent infinite recursion.
 *
 * <p>Since this class has subtypes, Jackson needs to know which type to instantiate when it
 * encounters a {@link CFANode} object during deserialization. This additional information is
 * serialized as a property "nodeType" in the JSON representation of the object.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>Edges are serialized as IDs.
 *
 * <p>outOfScopeVariables are sorted to ensure deterministic serialization.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonIdentityInfo(
    generator = PropertyGenerator.class,
    scope = CFANode.class,
    property = "nodeNumber")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "nodeType")
@JsonSubTypes({
  @Type(value = CFALabelNode.class, name = "Label"),
  @Type(value = CFANode.class, name = "Basic"),
  @Type(value = CFATerminationNode.class, name = "Termination"),
  @Type(value = FunctionEntryNode.class, name = "FunctionEntry"),
  @Type(value = FunctionExitNode.class, name = "FunctionExit")
})
public final class CFANodeMixin {

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private List<CFAEdge> leavingEdges;

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private List<CFAEdge> enteringEdges;

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private FunctionSummaryEdge leavingSummaryEdge;

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private FunctionSummaryEdge enteringSummaryEdge;

  /* The conversion is required to ensure deterministic serialization. */
  @SuppressWarnings("unused")
  @JsonSerialize(converter = CSimpleDeclarationSetToSortedListConverter.class)
  private Set<CSimpleDeclaration> outOfScopeVariables;

  @SuppressWarnings("unused")
  @JsonCreator
  public CFANodeMixin(@JsonProperty("function") AFunctionDeclaration pFunction) {}
}
