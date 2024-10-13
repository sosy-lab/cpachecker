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
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;

/**
 * This class is a mixin for {@link ALiteralExpression}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = ACharLiteralExpression.class, name = "ACharLiteralExpression"),
  @Type(value = AFloatLiteralExpression.class, name = "AFloatLiteralExpression"),
  @Type(value = AIntegerLiteralExpression.class, name = "AIntegerLiteralExpression"),
  @Type(value = AStringLiteralExpression.class, name = "AStringLiteralExpression"),
  @Type(value = CImaginaryLiteralExpression.class, name = "CImaginaryLiteralExpression")
})
public final class ALiteralExpressionMixin {}
