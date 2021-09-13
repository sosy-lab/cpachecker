// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public interface CCfaNodeVisitor<R, X extends Exception> {

  R visit(CFANode pCfaNode) throws X;

  R visit(CFATerminationNode pCfaTerminationNode) throws X;

  R visit(FunctionExitNode pFunctionExitNode) throws X;

  R visit(CFunctionEntryNode pCFunctionEntryNode) throws X;

  R visit(CLabelNode pCLabelNode) throws X;
}
