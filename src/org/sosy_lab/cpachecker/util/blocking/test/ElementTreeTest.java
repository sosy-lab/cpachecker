/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.blocking.container.ItemTree;

@SuppressWarnings("unused")
public class ElementTreeTest {

  @Test
  public void testContainsLeaf() {
    ItemTree<String, CFANode> tree = new ItemTree<String, CFANode>();

    CFANode n1 = new CFANode(100, "TEST");
    CFANode n2 = new CFANode(101, "TEST2");
    tree.put("L1").put("L2").addLeaf(n1);
    tree.put(new String[]{"X1", "X2"}).addLeaf(n1);
    tree.put(new String[]{"X1", "X3"}).addLeaf(n2, true);

    assertTrue(!tree.containsLeaf(new String[]{}, n1));
    assertTrue(!tree.containsLeaf(new String[]{"FOO"}, n1));
    assertTrue(!tree.containsLeaf(new String[]{"FOO", "BAR"}, n1));
    assertTrue(!tree.containsLeaf(new String[]{"L1"}, n1));
    assertTrue(!tree.containsLeaf(new String[]{"L1", "L2"}, null));
    assertTrue(tree.containsLeaf(new String[]{"L1", "L2"}, n1));
    assertTrue(!tree.containsLeaf(new String[]{"X1", "X2"}, null));
    assertTrue(tree.containsLeaf(new String[]{"X1", "X2"}, n1));

    assertTrue(!tree.containsLeaf(new String[]{}, n1));
    assertTrue(tree.containsLeaf(new String[]{}, n2));
    assertTrue(!tree.containsLeaf(new String[]{}, null));
  }

}
