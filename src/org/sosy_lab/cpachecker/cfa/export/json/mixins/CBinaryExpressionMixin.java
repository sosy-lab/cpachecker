// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is a mixin for {@link CBinaryExpression}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CBinaryExpressionMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CBinaryExpressionMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("type") CType pExpressionType,
      @JsonProperty("calculationType") CType pCalculationType,
      @JsonProperty("operand1") CExpression pOperand1,
      @JsonProperty("operand2") CExpression pOperand2,
      @JsonProperty("operator") BinaryOperator pOperator) {}
}
