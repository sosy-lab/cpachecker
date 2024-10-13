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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;

/**
 * This class is a mixin for {@link AStatement}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AFunctionCall.class, name = "AFunctionCall"),
  @Type(value = AbstractStatement.class, name = "AbstractStatement"),
  @Type(value = CStatement.class, name = "CStatement")
})
public final class AStatementMixin {}
