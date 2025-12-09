// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.visitors;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class ModifiedVariablesCollectorVisitor
    implements SvLibCfaEdgeStatementVisitor<Set<SvLibSimpleDeclaration>, NoException> {
  @Override
  public Set<SvLibSimpleDeclaration> accept(
      SvLibTermAssignmentCfaStatement pSvLibTermAssignmentCfaStatement) throws NoException {
    return ImmutableSet.of(pSvLibTermAssignmentCfaStatement.getLeftHandSide().getDeclaration());
  }

  @Override
  public Set<SvLibSimpleDeclaration> accept(
      SvLibFunctionCallAssignmentStatement pSvLibFunctionCallAssignmentStatement)
      throws NoException {
    return transformedImmutableSetCopy(
        pSvLibFunctionCallAssignmentStatement.getLeftHandSide().getIdTerms(),
        SvLibIdTerm::getDeclaration);
  }
}
