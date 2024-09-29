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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.BracketRemoverConverter;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * This class is a mixin for {@link CAssumeEdge}.
 *
 * <p>rawStatement is deserialized using {@link BracketRemoverConverter}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CAssumeEdgeMixin {

  @SuppressWarnings("unused")
  @JsonDeserialize(converter = BracketRemoverConverter.class)
  private String rawStatement;

  @SuppressWarnings("unused")
  @JsonCreator
  public CAssumeEdgeMixin(
      @JsonProperty("rawStatement") String pRawStatement,
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("predecessor") CFANode pPredecessor,
      @JsonProperty("successor") CFANode pSuccessor,
      @JsonProperty("expression") CExpression pExpression,
      @JsonProperty("truthAssumption") boolean pTruthAssumption,
      @JsonProperty("swapped") boolean pSwapped,
      @JsonProperty("artificialIntermediate") boolean pArtificial) {}
}
