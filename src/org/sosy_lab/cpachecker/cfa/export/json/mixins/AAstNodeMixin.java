// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;

/**
 * This class is a mixin for {@link AAstNode}.
 *
 * <p>Since this class has subtypes, Jackson needs to know which type to instantiate when it
 * encounters a {@link AAstNode} object during deserialization. This additional information is
 * serialized as a property "astNodeType" in the JSON representation of the object.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "astNodeType")
@JsonSubTypes({
  @Type(value = AAssignment.class, name = "AAssignment"),
  @Type(value = AInitializer.class, name = "AInitializer"),
  @Type(value = AReturnStatement.class, name = "AReturnStatement"),
  @Type(value = ARightHandSide.class, name = "ARightHandSide"),
  @Type(value = ASimpleDeclaration.class, name = "ASimpleDeclaration"),
  @Type(value = AStatement.class, name = "AStatement"),
  @Type(value = AbstractAstNode.class, name = "Abstract"),
  @Type(value = CAstNode.class, name = "C"),
})
public final class AAstNodeMixin {}
