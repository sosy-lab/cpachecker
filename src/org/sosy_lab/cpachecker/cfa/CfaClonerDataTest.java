// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;

import java.util.OptionalInt;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CfaClonerDataTest {

  @Test
  public void originalElementsReturnThemselves() {
    CFANode node = CFANode.newDummyCFANode();
    CFAEdge edge = new BlankEdge("", FileLocation.DUMMY, node, node, "");

    assertThat(node.getOriginal()).isSameInstanceAs(node);
    assertThat(edge.getOriginal()).isSameInstanceAs(edge);
    assertThat(node.getCloneThreadId()).isEqualTo(OptionalInt.empty());
  }

  @Test
  public void clonedElementsResolveAndCacheTheirOriginals() {
    CFANode originalPredecessor = CFANode.newDummyCFANode();
    CFANode originalSuccessor = CFANode.newDummyCFANode();
    CFANode clonedPredecessor = CFANode.newDummyCFANode();
    CFANode clonedSuccessor = CFANode.newDummyCFANode();
    CFAEdge originalEdge =
        new BlankEdge("", FileLocation.DUMMY, originalPredecessor, originalSuccessor, "");
    CFAEdge clonedEdge =
        new BlankEdge("", FileLocation.DUMMY, clonedPredecessor, clonedSuccessor, "");

    CfaClonerData clonerData = new CfaClonerData();
    clonerData.registerNode(clonedPredecessor, originalPredecessor, 3);
    clonerData.registerNode(clonedSuccessor, originalSuccessor, 3);
    clonedPredecessor.setClonerData(clonerData);
    clonedSuccessor.setClonerData(clonerData);
    clonerData.registerEdge(clonedEdge, originalEdge);
    ((org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge) clonedEdge).setClonerData(clonerData);

    assertThat(clonedPredecessor.getOriginal()).isSameInstanceAs(originalPredecessor);
    assertThat(clonedPredecessor.getOriginal()).isSameInstanceAs(originalPredecessor);
    assertThat(clonedEdge.getOriginal()).isSameInstanceAs(originalEdge);
    assertThat(clonedEdge.getOriginal()).isSameInstanceAs(originalEdge);
    assertThat(clonedPredecessor.getCloneThreadId()).isEqualTo(OptionalInt.of(3));
  }
}
