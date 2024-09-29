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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.OutOfScopeToSortedListConverter;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This class is a mixin for {@link CFANode}.
 *
 * <p>Identity information is being serialized to prevent infinite recursion.
 *
 * <p>Type information is being serialized to account for subtype polymorphism.
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
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "typeOfCFANode")
public final class CFANodeMixin {

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private List<CFAEdge> leavingEdges;

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private List<CFAEdge> enteringEdges;

  @SuppressWarnings("unused")
  @JsonSerialize(converter = OutOfScopeToSortedListConverter.class)
  private Set<CSimpleDeclaration> outOfScopeVariables;

  @SuppressWarnings("unused")
  @JsonCreator
  public CFANodeMixin(@JsonProperty("function") AFunctionDeclaration pFunction) {}
}
