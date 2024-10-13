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
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;

/**
 * This class is a mixin for {@link CStatement}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = CAssignment.class, name = "CAssignment"),
  @Type(value = CExpressionAssignmentStatement.class, name = "CExpressionAssignmentStatement"),
  @Type(value = CExpressionStatement.class, name = "CExpressionStatement"),
  @Type(value = CFunctionCall.class, name = "CFunctionCall"),
  @Type(value = CFunctionCallAssignmentStatement.class, name = "CFunctionCallAssignmentStatement"),
  @Type(value = CFunctionCallStatement.class, name = "CFunctionCallStatement")
})
public final class CStatementMixin {}
