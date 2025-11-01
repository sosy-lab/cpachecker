// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSetMultimap;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.SMTLibCommand;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record K3CfaMetadata(
    List<SMTLibCommand> smtLibCommands,
    ImmutableSetMultimap<CFANode, K3TagProperty> tagAnnotations,
    boolean exportWitness) {

  public K3CfaMetadata {
    checkNotNull(smtLibCommands);
    checkNotNull(tagAnnotations);
  }
}
