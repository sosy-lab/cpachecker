// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class DssCallstackEffectTest {

  private static FunctionReturnEdge returnFor(CFA pCfa, FunctionCallEdge pCall) {
    return Iterables.getOnlyElement(
        CFAUtils.allEdges(pCfa)
            .filter(FunctionReturnEdge.class)
            .filter(edge -> edge.getSummaryEdge() == pCall.getSummaryEdge()));
  }

  private static DssCallstackEffect effect(Iterable<? extends CFAEdge> pPath) {
    DssCallstackEffect effect = DssCallstackEffect.EMPTY;
    for (CFAEdge edge : pPath) {
      effect = effect.append(edge);
    }
    return effect;
  }

  private static CallstackTransferRelationBackwards backwards(int pBound, boolean pSkip)
      throws Exception {
    return new CallstackTransferRelationBackwards(
        new CallstackOptions(
            Configuration.builder()
                .setOption("cpa.callstack.depth", Integer.toString(pBound))
                .setOption("cpa.callstack.skipRecursion", Boolean.toString(pSkip))
                .build()),
        LogManager.createTestLogManager());
  }

  private static boolean replay(
      List<CFAEdge> pPath, CallstackState pEnd, CallstackTransferRelationBackwards pBackwards)
      throws Exception {
    AbstractState current = pEnd;
    for (CFAEdge edge : pPath.reversed()) {
      var predecessors =
          pBackwards.getAbstractSuccessorsForEdge(current, SingletonPrecision.getInstance(), edge);
      if (predecessors.isEmpty()) {
        return false;
      }
      current = Iterables.getOnlyElement(predecessors);
    }
    return true;
  }

  @Test
  public void balancedCallsAtDifferentSitesHaveTheSameGuard() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f() {} int main() { f(); f(); }");
    var calls = CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class).toList();
    DssCallstackEffect first = effect(ImmutableList.of(calls.get(0), returnFor(cfa, calls.get(0))));
    DssCallstackEffect second =
        effect(ImmutableList.of(calls.get(1), returnFor(cfa, calls.get(1))));
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first).isNotEqualTo(DssCallstackEffect.EMPTY);
    for (int i = 0; i < 100; i++) {
      first = first.append(calls.get(0)).append(returnFor(cfa, calls.get(0)));
    }
    assertThat(first).isEqualTo(second);
  }

  @Test
  public void normalizationRetainsRecursionRejectionAndUnsupportedErrors() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f() {} int main() { f(); }");
    FunctionCallEdge call =
        Iterables.getOnlyElement(CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class));
    var path = ImmutableList.<CFAEdge>of(call, returnFor(cfa, call));
    DssCallstackEffect effect = effect(path);
    CallstackState end = new CallstackState(null, "f", call.getPredecessor());
    assertThat(effect.accepts(end, backwards(0, true), SingletonPrecision.getInstance())).isFalse();
    CallstackTransferRelationBackwards rejecting = backwards(0, false);
    assertThrows(
        UnsupportedCodeException.class,
        () -> effect.accepts(end, rejecting, SingletonPrecision.getInstance()));
    assertThat(effect.accepts(end, backwards(1, true), SingletonPrecision.getInstance())).isTrue();
  }

  @Test
  public void nestedAndSequentialGuardsMatchUncompressedReplay() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void g() {} void f() { g(); } int main() { f(); }");
    var calls = CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class).toList();
    FunctionCallEdge f =
        Iterables.getOnlyElement(
            calls.stream().filter(c -> c.getSuccessor().getFunctionName().equals("f")).toList());
    FunctionCallEdge g =
        Iterables.getOnlyElement(
            calls.stream().filter(c -> c.getSuccessor().getFunctionName().equals("g")).toList());
    List<CFAEdge> path = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      path.addAll(ImmutableList.of(f, g, returnFor(cfa, g), returnFor(cfa, f)));
    }
    for (int bound = 0; bound < 3; bound++) {
      for (String function : ImmutableList.of("main", "f", "g")) {
        CallstackState stack = new CallstackState(null, function, cfa.getMainFunction());
        for (int depth = 1; depth <= 3; depth++) {
          assertThat(
                  effect(path)
                      .accepts(stack, backwards(bound, true), SingletonPrecision.getInstance()))
              .isEqualTo(replay(path, stack, backwards(bound, true)));
          stack = new CallstackState(stack, function, cfa.getMainFunction());
        }
      }
    }
  }

  @Test
  public void recursiveNestingRetainsTheMaximumTemporaryDepth() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString("void f(int x) { if (x) f(x-1); } int main() { f(1); }");
    var calls = CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class).toList();
    FunctionCallEdge outer =
        Iterables.getOnlyElement(
            calls.stream()
                .filter(c -> c.getPredecessor().getFunctionName().equals("main"))
                .toList());
    FunctionCallEdge inner =
        Iterables.getOnlyElement(
            calls.stream().filter(c -> c.getPredecessor().getFunctionName().equals("f")).toList());
    var path =
        ImmutableList.<CFAEdge>of(outer, inner, returnFor(cfa, inner), returnFor(cfa, outer));
    for (int bound = 0; bound < 4; bound++) {
      CallstackState end = new CallstackState(null, "main", cfa.getMainFunction());
      assertThat(
              effect(path).accepts(end, backwards(bound, true), SingletonPrecision.getInstance()))
          .isEqualTo(replay(path, end, backwards(bound, true)));
    }
  }

  @Test
  public void endpointEqualityDoesNotConflateDifferentCallStatements() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            "extern void a(); extern void b(); int main() { a(); b(); }");
    var statements = CFAUtils.allEdges(cfa).filter(CStatementEdge.class).toList();
    CStatementEdge first = statements.get(0);
    CStatementEdge other =
        new CStatementEdge(
            "b()",
            statements.get(1).getStatement(),
            FileLocation.DUMMY,
            first.getPredecessor(),
            first.getSuccessor());
    assertThat(first).isEqualTo(other); // CFAEdge equality only compares endpoints.
    assertThat(DssCallstackEffect.EMPTY.append(first))
        .isNotEqualTo(DssCallstackEffect.EMPTY.append(other));
  }
}
