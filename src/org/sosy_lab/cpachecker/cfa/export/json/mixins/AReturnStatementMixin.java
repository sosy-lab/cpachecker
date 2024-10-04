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
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;

/**
 * This class is a mixin for {@link AReturnStatement}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AbstractReturnStatement.class, name = "AbstractReturnStatement"),
})
public final class AReturnStatementMixin {}
