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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

/**
 * This class is a mixin for {@link CFunctionSummaryEdge}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CFunctionSummaryEdgeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CFunctionSummaryEdgeMixin(
      @JsonProperty("rawStatement") String pRawStatement,
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("predecessor") CFANode pPredecessor,
      @JsonProperty("successor") CFANode pSuccessor,
      @JsonProperty("expression") CFunctionCall pExpression,
      @JsonProperty("functionEntry") CFunctionEntryNode pFunctionEntry) {}
}
