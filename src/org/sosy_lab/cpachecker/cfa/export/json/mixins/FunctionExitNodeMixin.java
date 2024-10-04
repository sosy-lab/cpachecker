// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * This class is a mixin for {@link FunctionExitNode}.
 *
 * <p>It serializes its {@link FunctionEntryNode} field as number to prevent infinite recursion:
 * {@link FunctionExitNode} -> {@link FunctionEntryNode} -> {@link FunctionExitNode} -> ... This
 * Mixin assumes that all {@link CFANode}s of the CFA are serialized, and therefore the {@link
 * FunctionEntryNode} as well.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class FunctionExitNodeMixin {

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private FunctionEntryNode entryNode;

  @SuppressWarnings("unused")
  @JsonCreator
  public FunctionExitNodeMixin(@JsonProperty("function") AFunctionDeclaration pFunction) {}
}
