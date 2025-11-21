// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;

public record SvLibCfaMetadata(
    ImmutableList<SmtLibCommand> smtLibCommands,
    ImmutableSetMultimap<CFANode, SvLibTagProperty> tagAnnotations,
    ImmutableSetMultimap<CFANode, SvLibTagReference> tagReferences) {

  public SvLibCfaMetadata {
    checkNotNull(smtLibCommands);
    checkNotNull(tagAnnotations);
    checkNotNull(tagReferences);
  }
}
