// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CFACloneOriginalTest {

  @Test
  public void originalElementsReturnThemselves() {
    CFANode node = CFANode.newDummyCFANode();
    CFAEdge edge = new BlankEdge("", FileLocation.DUMMY, node, node, "");

    assertThat(node.getOriginal()).isSameInstanceAs(node);
    assertThat(edge.getOriginal()).isSameInstanceAs(edge);
    assertThat(node.getCloneThreadId()).isEqualTo(Optional.empty());
  }

  @Test
  public void clonedElementsReturnTheirOriginals() {
    CFANode originalPredecessor = CFANode.newDummyCFANode();
    CFANode originalSuccessor = CFANode.newDummyCFANode();
    CFANode clonedPredecessor = CFANode.newDummyCFANode();
    CFANode clonedSuccessor = CFANode.newDummyCFANode();
    CFAEdge originalEdge =
        new BlankEdge("", FileLocation.DUMMY, originalPredecessor, originalSuccessor, "");
    BlankEdge clonedEdge =
        new BlankEdge("", FileLocation.DUMMY, clonedPredecessor, clonedSuccessor, "");

    clonedPredecessor.setOriginal(originalPredecessor, new ThreadId(3));
    clonedSuccessor.setOriginal(originalSuccessor, new ThreadId(3));
    clonedEdge.setOriginal(originalEdge);

    assertThat(clonedPredecessor.getOriginal()).isSameInstanceAs(originalPredecessor);
    assertThat(clonedEdge.getOriginal()).isSameInstanceAs(originalEdge);
    assertThat(clonedPredecessor.getCloneThreadId()).isEqualTo(Optional.of(new ThreadId(3)));
    assertThat(originalPredecessor.getCloneThreadId()).isEqualTo(Optional.empty());
  }
}
