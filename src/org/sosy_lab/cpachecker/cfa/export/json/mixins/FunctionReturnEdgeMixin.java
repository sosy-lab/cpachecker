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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

/**
 * This class is a mixin for {@link FunctionReturnEdge}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonSubTypes({@Type(value = CFunctionReturnEdge.class, name = "CFunctionReturn")})
public final class FunctionReturnEdgeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  protected FunctionReturnEdgeMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("predecessor") FunctionExitNode pPredecessor,
      @JsonProperty("successor") CFANode pSuccessor,
      @JsonProperty("summaryEdge") FunctionSummaryEdge pSummaryEdge) {}
}
