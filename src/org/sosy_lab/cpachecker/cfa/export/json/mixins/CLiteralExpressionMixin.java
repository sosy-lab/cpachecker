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
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;

/**
 * This class is a mixin for {@link CLiteralExpression}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = CCharLiteralExpression.class, name = "CCharLiteralExpression"),
  @Type(value = CFloatLiteralExpression.class, name = "CFloatLiteralExpression"),
  @Type(value = CImaginaryLiteralExpression.class, name = "CImaginaryLiteralExpression"),
  @Type(value = CIntegerLiteralExpression.class, name = "CIntegerLiteralExpression"),
  @Type(value = CStringLiteralExpression.class, name = "CStringLiteralExpression"),
})
public final class CLiteralExpressionMixin {}
