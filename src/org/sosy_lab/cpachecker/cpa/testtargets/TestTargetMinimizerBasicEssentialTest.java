// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TestTargetMinimizerBasicEssentialTest {
  @Test
  public void noReductionTest() {

    TestTargetMinimizerBasicEssential uut = new TestTargetMinimizerBasicEssential();
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    CFAEdge a = new BlankEdge("a", FileLocation.DUMMY, u, x, "a");
    CFAEdge b = new BlankEdge("b", FileLocation.DUMMY, v, x, "b");
    CFAEdge c = new BlankEdge("c", FileLocation.DUMMY, x, y, "c");
    CFAEdge d = new BlankEdge("d", FileLocation.DUMMY, y, z, "d");
    CFAEdge e = new BlankEdge("e", FileLocation.DUMMY, y, w, "e");
    CFAEdge f = new BlankEdge("f", FileLocation.DUMMY, u, z, "f");
    u.addLeavingEdge(a);
    u.addLeavingEdge(f);
    v.addLeavingEdge(b);
    x.addLeavingEdge(c);
    y.addLeavingEdge(d);
    y.addLeavingEdge(e);
    x.addEnteringEdge(a);
    x.addEnteringEdge(b);
    y.addEnteringEdge(c);
    z.addEnteringEdge(d);
    w.addEnteringEdge(e);
    z.addEnteringEdge(f);

    testTargets.add(c);
    testTargets.add(f);
    Set<CFAEdge> result = uut.reduceTargets(testTargets);
    assertThat(result.contains(c)).isTrue();
    assertThat(result.contains(f)).isTrue();
  }

  @Test
  public void Rule1ReductionTest() {

    TestTargetMinimizerBasicEssential uut = new TestTargetMinimizerBasicEssential();
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    CFAEdge a = new BlankEdge("a", FileLocation.DUMMY, u, x, "a");
    CFAEdge b = new BlankEdge("b", FileLocation.DUMMY, v, x, "b");
    CFAEdge c = new BlankEdge("c", FileLocation.DUMMY, x, y, "c");
    CFAEdge d = new BlankEdge("d", FileLocation.DUMMY, y, z, "d");
    CFAEdge e = new BlankEdge("e", FileLocation.DUMMY, y, w, "e");
    CFAEdge f = new BlankEdge("f", FileLocation.DUMMY, z, y, "f");
    u.addLeavingEdge(a);
    v.addLeavingEdge(b);
    x.addLeavingEdge(c);
    y.addLeavingEdge(d);
    y.addLeavingEdge(e);
    z.addLeavingEdge(f);
    x.addEnteringEdge(a);
    x.addEnteringEdge(b);
    y.addEnteringEdge(c);
    z.addEnteringEdge(d);
    w.addEnteringEdge(e);
    y.addEnteringEdge(f);

    testTargets.add(c);
    testTargets.add(b);
    Set<CFAEdge> result = uut.reduceTargets(testTargets);
    assertThat(result.contains(c)).isFalse();
    assertThat(result.contains(b)).isTrue();
  }

  @Test
  public void Rule1LongReductionTest() {

    TestTargetMinimizerBasicEssential uut = new TestTargetMinimizerBasicEssential();
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode t = CFANode.newDummyCFANode("t");
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    CFAEdge a = new BlankEdge("a", FileLocation.DUMMY, u, x, "a");
    CFAEdge b = new BlankEdge("b", FileLocation.DUMMY, v, x, "b");
    CFAEdge c = new BlankEdge("c", FileLocation.DUMMY, t, y, "c");
    CFAEdge d = new BlankEdge("d", FileLocation.DUMMY, y, z, "d");
    CFAEdge e = new BlankEdge("e", FileLocation.DUMMY, y, w, "e");
    CFAEdge f = new BlankEdge("f", FileLocation.DUMMY, z, y, "f");
    CFAEdge g = new BlankEdge("g", FileLocation.DUMMY, x, t, "g");
    u.addLeavingEdge(a);
    v.addLeavingEdge(b);
    t.addLeavingEdge(c);
    y.addLeavingEdge(d);
    y.addLeavingEdge(e);
    z.addLeavingEdge(f);
    x.addLeavingEdge(g);
    x.addEnteringEdge(a);
    x.addEnteringEdge(b);
    y.addEnteringEdge(c);
    z.addEnteringEdge(d);
    w.addEnteringEdge(e);
    y.addEnteringEdge(f);
    t.addEnteringEdge(g);
    testTargets.add(c);
    testTargets.add(b);
    Set<CFAEdge> result = uut.reduceTargets(testTargets);
    assertThat(result.contains(c)).isFalse();
    assertThat(result.contains(b)).isTrue();
  }

  @Test
  public void Rule2ReductionTest() {
    TestTargetMinimizerBasicEssential uut = new TestTargetMinimizerBasicEssential();
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    CFAEdge a = new BlankEdge("a", FileLocation.DUMMY, u, x, "a");
    CFAEdge b = new BlankEdge("b", FileLocation.DUMMY, v, x, "b");
    CFAEdge c = new BlankEdge("c", FileLocation.DUMMY, x, y, "c");
    CFAEdge d = new BlankEdge("d", FileLocation.DUMMY, y, z, "d");
    CFAEdge e = new BlankEdge("e", FileLocation.DUMMY, y, w, "e");
    CFAEdge f = new BlankEdge("f", FileLocation.DUMMY, x, z, "f");
    u.addLeavingEdge(a);
    v.addLeavingEdge(b);
    x.addLeavingEdge(c);
    y.addLeavingEdge(d);
    y.addLeavingEdge(e);
    x.addLeavingEdge(f);
    x.addEnteringEdge(a);
    x.addEnteringEdge(b);
    y.addEnteringEdge(c);
    z.addEnteringEdge(d);
    w.addEnteringEdge(e);
    z.addEnteringEdge(f);

    testTargets.add(c);
    testTargets.add(d);
    Set<CFAEdge> result = uut.reduceTargets(testTargets);
    assertThat(result.contains(c)).isFalse();
    assertThat(result.contains(d)).isTrue();
  }

  @Test
  public void Rule2LongReductionTest() {
    TestTargetMinimizerBasicEssential uut = new TestTargetMinimizerBasicEssential();
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode t = CFANode.newDummyCFANode("t");
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    t.setReversePostorderId(4);
    u.setReversePostorderId(0);
    x.setReversePostorderId(1);
    v.setReversePostorderId(2);
    y.setReversePostorderId(3);
    z.setReversePostorderId(5);
    w.setReversePostorderId(6);

    CFAEdge a = new BlankEdge("a", FileLocation.DUMMY, u, x, "a");
    CFAEdge b = new BlankEdge("b", FileLocation.DUMMY, v, x, "b");
    CFAEdge c = new BlankEdge("c", FileLocation.DUMMY, x, y, "c");
    CFAEdge d = new BlankEdge("d", FileLocation.DUMMY, t, z, "d");
    CFAEdge e = new BlankEdge("e", FileLocation.DUMMY, t, w, "e");
    CFAEdge f = new BlankEdge("f", FileLocation.DUMMY, x, z, "f");
    CFAEdge g = new BlankEdge("g", FileLocation.DUMMY, y, t, "g");

    u.addLeavingEdge(a);
    v.addLeavingEdge(b);
    x.addLeavingEdge(c);
    t.addLeavingEdge(d);
    t.addLeavingEdge(e);
    x.addLeavingEdge(f);
    y.addLeavingEdge(g);
    x.addEnteringEdge(a);
    x.addEnteringEdge(b);
    y.addEnteringEdge(c);
    z.addEnteringEdge(d);
    w.addEnteringEdge(e);
    z.addEnteringEdge(f);
    t.addEnteringEdge(g);

    testTargets.add(c);
    testTargets.add(d);
    Set<CFAEdge> result = uut.reduceTargets(testTargets);
    assertThat(result.contains(c)).isFalse();
    assertThat(result.contains(d)).isTrue();
  }
}
