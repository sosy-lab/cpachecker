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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;

public record SvLibCfaMetadata(
    ImmutableList<SmtLibCommand> smtLibCommands,
    ImmutableMap<SvLibTagReference, SvLibScope> tagReferenceToScope,
    ImmutableMap<SvLibFunctionDeclaration, SvLibProcedureDeclaration>
        functionToProcedureDeclaration,
    ImmutableMap<CFANode, SvLibProcedureDeclaration> nodesToActualProcedureDefinitionEnd,
    ImmutableMap<CFANode, SvLibHavocStatement> nodesToActualHavocStatementEnd,
    ImmutableSetMultimap<CFANode, SvLibTagProperty> tagAnnotations,
    ImmutableSetMultimap<CFANode, SvLibTagReference> tagReferences) {

  public SvLibCfaMetadata {
    checkNotNull(smtLibCommands);
    checkNotNull(tagReferenceToScope);
    checkNotNull(functionToProcedureDeclaration);
    checkNotNull(nodesToActualProcedureDefinitionEnd);
    checkNotNull(nodesToActualHavocStatementEnd);
    checkNotNull(tagAnnotations);
    checkNotNull(tagReferences);
  }
}
