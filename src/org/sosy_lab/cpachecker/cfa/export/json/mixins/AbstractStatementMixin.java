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
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AbstractStatement;

/**
 * This class is a mixin for {@link AbstractStatement}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AExpressionAssignmentStatement.class, name = "AExpressionAssignmentStatement"),
  @Type(value = AExpressionStatement.class, name = "AExpressionStatement"),
  @Type(value = AFunctionCallAssignmentStatement.class, name = "AFunctionCallAssignmentStatement"),
  @Type(value = AFunctionCallStatement.class, name = "AFunctionCallStatement")
})
public final class AbstractStatementMixin {}
