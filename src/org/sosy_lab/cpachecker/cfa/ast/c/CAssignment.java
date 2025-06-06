// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;

/**
 * Interface for all statements that contain an assignment. Only subclasses of {@link CStatement}
 * may implement this interface.
 */
public sealed interface CAssignment extends AAssignment, CStatement
    permits CExpressionAssignmentStatement, CFunctionCallAssignmentStatement {

  @Override
  CLeftHandSide getLeftHandSide();

  @Override
  CRightHandSide getRightHandSide();
}
