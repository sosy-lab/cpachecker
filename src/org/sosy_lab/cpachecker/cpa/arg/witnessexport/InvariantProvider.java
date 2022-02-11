// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

import java.util.Optional;

public interface InvariantProvider {

  ExpressionTree<Object> provideInvariantFor(
      CFAEdge pCFAEdge, Optional<? extends Collection<? extends ARGState>> pStates);

  static enum TrueInvariantProvider implements InvariantProvider {
    INSTANCE;

    @Override
    public ExpressionTree<Object> provideInvariantFor(
        CFAEdge pCFAEdge, Optional<? extends Collection<? extends ARGState>> pStates) {
      return ExpressionTrees.getTrue();
    }
  }
}
