// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

/**
 * Interface for all statements that contain an assignment. Only sub-classes of {@link AStatement}
 * may implement this interface.
 */
public interface AAssignment extends AAstNode {

  ALeftHandSide getLeftHandSide();

  ARightHandSide getRightHandSide();
}
