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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement;

/**
 * This class is a mixin for {@link CFunctionCallStatement}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonSubTypes({
  @Type(value = CThreadOperationStatement.class, name = "CThreadOperationStatement"),
})
public final class CFunctionCallStatementMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CFunctionCallStatementMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("functionCall") CFunctionCallExpression pFunctionCall) {}
}
