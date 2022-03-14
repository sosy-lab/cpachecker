// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public interface ExpressionTreeCandidateInvariant extends CandidateInvariant {

  ExpressionTree<Object> asExpressionTree();
}
