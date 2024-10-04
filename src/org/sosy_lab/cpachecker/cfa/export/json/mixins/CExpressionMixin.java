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
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;

/**
 * This class is a mixin for {@link CExpression}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = CAddressOfLabelExpression.class, name = "CAddressOfLabelExpression"),
  @Type(value = CBinaryExpression.class, name = "CBinaryExpression"),
  @Type(value = CCastExpression.class, name = "CCastExpression"),
  @Type(value = CLeftHandSide.class, name = "CLeftHandSide"),
  @Type(value = CLiteralExpression.class, name = "CLiteralExpression"),
  @Type(value = CTypeIdExpression.class, name = "CTypeIdExpression"),
  @Type(value = CUnaryExpression.class, name = "CUnaryExpression"),
})
public final class CExpressionMixin {}
