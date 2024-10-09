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
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;

/**
 * This class is a mixin for {@link AReturnStatementEdge}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonSubTypes({@Type(value = CReturnStatementEdge.class, name = "CReturnStatement")})
public final class AReturnStatementEdgeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  private AReturnStatementEdgeMixin(
      @JsonProperty("rawStatement") String pRawStatement,
      @JsonProperty("returnStatement") AReturnStatement pReturnStatement,
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("predecessor") CFANode pPredecessor,
      @JsonProperty("successor") FunctionExitNode pSuccessor) {}
}
