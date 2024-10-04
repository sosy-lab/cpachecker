// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * This class is a mixin for {@link AbstractCFAEdge}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>Nodes are serialized as IDs.
 */
@JsonSubTypes({
  @Type(value = ADeclarationEdge.class, name = "ADeclaration"),
  @Type(value = AReturnStatementEdge.class, name = "AReturnStatement"),
  @Type(value = AssumeEdge.class, name = "Assume"),
  @Type(value = AStatementEdge.class, name = "AStatement"),
  @Type(value = BlankEdge.class, name = "Blank"),
  @Type(value = FunctionCallEdge.class, name = "FunctionCall"),
  @Type(value = FunctionReturnEdge.class, name = "FunctionReturn"),
  @Type(value = FunctionSummaryEdge.class, name = "FunctionSummary")
})
public final class AbstractCFAEdgeMixin {

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private CFANode predecessor;

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private CFANode successor;
}
