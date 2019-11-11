/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.pixelexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

class DummyNode {
  private Set<DummyNode> children = new LinkedHashSet<>();

  public void addChild(DummyNode n) {
    children.add(n);
  }

  public Iterable<DummyNode> getChildren() {
    return children;
  }

}

public class GraphToPixelsWriterTest extends GraphToPixelsWriter<DummyNode> {

  public GraphToPixelsWriterTest() throws InvalidConfigurationException {
    super(Configuration.defaultConfiguration());
  }

  @Override
  public SimpleGraphLevel.Builder<DummyNode> getLevelBuilder() {
    return new SimpleGraphLevel.Builder<>();
  }

  @Override
  public Iterable<DummyNode> getChildren(DummyNode pParent) {
    return pParent.getChildren();
  }

  /*
   * Test if the level structure for a simple self-loop
   * graph is computerd correctly
   */
  @Test
  public void getStructureTest1() {
    //                                      ___
    DummyNode root = new DummyNode(); //    v  |
    root.addChild(root); //               root-'

    // actual computation:
    GraphStructure result = getStructure(root);

    int nLevels = Iterators.size(result.iterator());
    assertThat(nLevels).isEqualTo(1);
    assertThat(result.iterator().next().getWidth()).isEqualTo(1);
  }

  /*
   * Test if the level structure for a representative example
   * is computed correctly
   */
  @Test
  public void getStructureTest2() {
    DummyNode root = new DummyNode();
    root.addChild(root);
    DummyNode nodes[] = new DummyNode[6];
    for (int i = 0; i < 6; i++) {
      nodes[i] = new DummyNode();
    }
    root.addChild(nodes[0]); //          ___
    root.addChild(nodes[0]); //          v  |
    root.addChild(nodes[1]); //        root-'
    nodes[0].addChild(nodes[2]); //    /   \
    nodes[1].addChild(nodes[2]); //   0     1
    nodes[2].addChild(nodes[3]); //    \   /
    nodes[2].addChild(nodes[4]); //      2
    nodes[2].addChild(nodes[5]); //    / | \
    //                                3  4  5

    // actual computation:
    GraphStructure result = getStructure(root);

    int nLevels = Iterators.size(result.iterator());
    assertThat(nLevels).isEqualTo(4);

    int expectedNodesPerLayer[] = {1, 2, 1, 3};
    Iterator<GraphLevel> actualLevels = result.iterator();
    for (int i = 0; i < expectedNodesPerLayer.length; i++) {
      assertThat(actualLevels.next().getWidth()).isEqualTo(expectedNodesPerLayer[i]);
    }
  }
}


