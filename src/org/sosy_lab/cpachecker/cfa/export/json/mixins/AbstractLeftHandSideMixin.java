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
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractLeftHandSide;

/**
 * This class is a mixin for {@link AbstractLeftHandSide}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AArraySubscriptExpression.class, name = "AArraySubscriptExpression"),
  @Type(value = ACastExpression.class, name = "ACastExpression"),
  @Type(value = AIdExpression.class, name = "AIdExpression"),
  @Type(value = APointerExpression.class, name = "APointerExpression")
})
public final class AbstractLeftHandSideMixin {}
