// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.CfaEdgeIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.CfaEdgeIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * This class is a mixin for {@link CFAEdge}.
 *
 * <p>Identity information is serialized to prevent infinite recursion.
 *
 * <p>Since this class has subtypes, Jackson needs to know which type to instantiate when it
 * encounters a {@link CFAEdge} object during deserialization. This additional information is
 * serialized as a property "edgeType" in the JSON representation of the object.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonIdentityInfo(
    generator = CfaEdgeIdGenerator.class,
    resolver = CfaEdgeIdResolver.class,
    scope = CFAEdge.class,
    property = "edgeNumber")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "edgeType")
@JsonSubTypes({
  @Type(value = AbstractCFAEdge.class, name = "Abstract"),
  @Type(value = DummyCFAEdge.class, name = "Dummy")
})
public final class CFAEdgeMixin {}
