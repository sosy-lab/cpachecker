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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.IntSequenceGenerator;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is a mixin for {@link Type}.
 *
 * <p>Identity information is serialized to prevent infinite recursion.
 *
 * <p>Since this class has subtypes, Jackson needs to know which type to instantiate when it
 * encounters a {@link Type} object during deserialization. This additional information is
 * serialized as a property "typeType" in the JSON representation of the object.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonIdentityInfo(
    generator = IntSequenceGenerator.class,
    scope = Type.class,
    property = "typeNumber")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "typeType")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AArrayType.class, name = "AArray"),
  @JsonSubTypes.Type(value = AFunctionType.class, name = "AFunction"),
  @JsonSubTypes.Type(value = CType.class, name = "C"),
})
public final class TypeMixin {}
