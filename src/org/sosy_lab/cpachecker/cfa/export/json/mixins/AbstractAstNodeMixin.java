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
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;

/**
 * This class is a mixin for {@link AbstractAstNode}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AbstractInitializer.class, name = "AbstractInitializer"),
  @Type(value = AbstractReturnStatement.class, name = "AbstractReturnStatement"),
  @Type(value = AbstractRightHandSide.class, name = "AbstractRightHandSide"),
  @Type(value = AbstractSimpleDeclaration.class, name = "AbstractSimpleDeclaration"),
  @Type(value = AbstractStatement.class, name = "AbstractStatement"),
  @Type(value = CDesignator.class, name = "CDesignator"),
})
public final class AbstractAstNodeMixin {}
