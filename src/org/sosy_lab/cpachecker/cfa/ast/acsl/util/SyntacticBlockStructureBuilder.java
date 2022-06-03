// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;

public class SyntacticBlockStructureBuilder {

  private final CFA cfa;
  private final Set<SyntacticBlock> blocks = new HashSet<>();

  public SyntacticBlockStructureBuilder(CFA pCFA) {
    cfa = pCFA;
  }

  public SyntacticBlockStructure build() {
    computeSetsForStatementBlocks();
    return new SyntacticBlockStructure(blocks);
  }

  public void addAll(Collection<SyntacticBlock> pBlocks) {
    blocks.addAll(pBlocks);
  }

  private void computeSetsForStatementBlocks() {
    Set<SyntacticBlock> toRemove = new HashSet<>();
    for (StatementBlock block : FluentIterable.from(blocks).filter(StatementBlock.class)) {
      if (!block.computeSets(cfa.getAllNodes())) {
        toRemove.add(block);
      }
    }
    blocks.removeAll(toRemove);
  }
}
