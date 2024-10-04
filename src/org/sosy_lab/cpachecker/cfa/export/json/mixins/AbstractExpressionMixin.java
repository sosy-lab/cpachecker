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
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;

/**
 * This class is a mixin for {@link AbstractExpression}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = ABinaryExpression.class, name = "ABinaryExpression"),
  @Type(value = ALiteralExpression.class, name = "ALiteralExpression"),
  @Type(value = AUnaryExpression.class, name = "AUnaryExpression"),
  @Type(value = AbstractLeftHandSide.class, name = "AbstractLeftHandSide"),
  @Type(value = CAddressOfLabelExpression.class, name = "CAddressOfLabelExpression"),
  @Type(value = CComplexCastExpression.class, name = "CComplexCastExpression"),
  @Type(value = CFieldReference.class, name = "CFieldReference"),
  @Type(value = CTypeIdExpression.class, name = "CTypeIdExpression"),
})
public final class AbstractExpressionMixin {}
