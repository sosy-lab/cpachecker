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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * This class is a mixin for {@link CStatementEdge}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonSubTypes({
  @Type(value = CFunctionSummaryStatementEdge.class, name = "CFunctionSummaryStatement")
})
public final class CStatementEdgeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CStatementEdgeMixin(
      @JsonProperty("rawStatement") String pRawStatement,
      @JsonProperty("statement") CStatement pStatement,
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("predecessor") CFANode pPredecessor,
      @JsonProperty("successor") CFANode pSuccessor) {}
}
