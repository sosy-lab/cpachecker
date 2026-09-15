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

/**
 * Metadata for SV-Lib CFAs.
 *
 * <p>The metadata contains all the information which is known during parsing, but forgotten during
 * the CFA building which is required to make use of the specifications inside the program or export
 * the witnesses.
 *
 * <p>Analysis developers should never have a need to access this class directly (except for maybe
 * the smtLibCommands in particular cases). This information is only intended for people working on
 * the frontend/changing the specifications being checked or witness export.
 *
 * @param smtLibCommands the SMT-Lib commands parsed from the SV-Lib input file
 * @param tagReferenceToScope mapping from tag references to their scopes, required to resolve
 *     variable declarations from their name in the SMT-LIB encoding during witness export
 * @param functionToProcedureDeclaration mapping from function declarations in the CFA to their
 *     corresponding procedure declarations from the parsing phase. Necessary to get input and local
 *     variables of procedures during witness export.
 * @param nodesToActualProcedureDefinitionEnd mapping from CFA nodes to the procedure declarations
 *     whose definition ends at the respective node. Currently, we split procedure definitions into
 *     function definitions which start with initializing the local and output variables. This map
 *     points the last node to the corresponding procedure declaration. This is necessary to
 *     determine which concrete state contains all the information in the violation witness export.
 * @param nodesToActualHavocStatementEnd mapping from CFA nodes to the havoc statements whose
 *     definition ends at the respective node. Similar to procedure definitions, we split parallel
 *     havoc statements into single havoc function calls which each initialize one variable. In the
 *     same manner as for procedure definitions, we need this to determine the correct concrete
 *     state during violation witness export.
 * @param tagAnnotations mapping from CFA nodes to the tag properties (annotations) associated with
 *     them
 * @param tagReferences mapping from CFA nodes to the tag references associated with them
 */
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
