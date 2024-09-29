// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.CfaEdgeIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.CfaEdgeIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * This class is a mixin for {@link CFAEdge}.
 *
 * <p>Identity information is being serialized to prevent infinite recursion.
 *
 * <p>Type information is being serialized to account for subtype polymorphism.
 */
@JsonIdentityInfo(
    generator = CfaEdgeIdGenerator.class,
    resolver = CfaEdgeIdResolver.class,
    property = "edgeNumber")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "typeOfCFAEdge")
public final class CFAEdgeMixin {}
