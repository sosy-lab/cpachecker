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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;

/**
 * This class is a mixin for {@link CFunctionEntryNode}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CFunctionEntryNodeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CFunctionEntryNodeMixin(
      @JsonProperty("location") FileLocation pFileLocation,
      @JsonProperty("functionDefinition") CFunctionDeclaration pFunctionDefinition,
      @JsonProperty("exitNode") FunctionExitNode pExitNode,
      @JsonProperty("returnVariable") Optional<CVariableDeclaration> pReturnVariable) {}
}
