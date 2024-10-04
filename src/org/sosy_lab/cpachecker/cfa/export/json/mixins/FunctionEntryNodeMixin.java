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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;

/**
 * This class is a mixin for {@link FunctionEntryNode}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It serializes its {@link FunctionExitNode} field as number.
 */
@JsonSubTypes({@Type(value = CFunctionEntryNode.class, name = "CFunctionEntry")})
public final class FunctionEntryNodeMixin {

  @SuppressWarnings("unused")
  @JsonIdentityReference(alwaysAsId = true)
  private FunctionExitNode exitNode;
}
