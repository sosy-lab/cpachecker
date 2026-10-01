// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class DssCallstackDomainTest {

  private final DssCallstackDomain domain = new DssCallstackDomain();
  private CFANode node;
  private CallstackState stack;
  private DssCallstackState state;

  @Before
  public void setUp() {
    node = CFANode.newDummyCFANode();
    stack = new CallstackState(null, node.getFunctionName(), node);
    state = new DssCallstackState(stack, false);
  }

  @Test
  public void dataEdgesDoNotPreventCoverage() {
    DssCallstackState first =
        state.withTraversedEdge(new BlankEdge("", FileLocation.DUMMY, node, node, "first"));
    DssCallstackState second =
        state.withTraversedEdge(new BlankEdge("", FileLocation.DUMMY, node, node, "second"));
    assertThat(domain.isLessOrEqual(first, second)).isTrue();
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void differentStackModesDoNotCoverEachOther() {
    DssCallstackState unknown = new DssCallstackState(stack, true);
    assertThat(domain.isLessOrEqual(state, unknown)).isFalse();
    assertThat(domain.isLessOrEqual(unknown, state)).isFalse();
    assertThat(state).isNotEqualTo(unknown);
  }

  @Test
  public void independentlyCreatedEqualStacksCoverEachOther() {
    DssCallstackState other =
        new DssCallstackState(new CallstackState(null, node.getFunctionName(), node), false);
    assertThat(domain.isLessOrEqual(state, other)).isTrue();
    assertThat(domain.isLessOrEqual(other, state)).isTrue();
  }

  @Test
  public void differentEffectsPreventCoverageEvenWithTheSameCurrentStack() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f() {} int main() { f(); }");
    FunctionCallEdge call =
        Iterables.getOnlyElement(CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class));
    DssCallstackState withCall = state.withTraversedEdge(call);
    assertThat(domain.isLessOrEqual(state, withCall)).isFalse();
    assertThat(domain.isLessOrEqual(withCall, state)).isFalse();
    assertThat(state).isNotEqualTo(withCall);

    // Local replay belongs to one exploration, not to a precondition reused in another run.
    assertThat(withCall.reset()).isEqualTo(state);
    assertThat(withCall.reset().getReversedTraversedEdges()).isEmpty();
  }

  @Test
  public void effectPreservesCallsiteRejection() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f() {} int main() { f(); }");
    FunctionCallEdge call =
        Iterables.getOnlyElement(CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class));
    CallstackState caller = new CallstackState(null, "main", cfa.getMainFunction());
    CallstackState matching = new CallstackState(caller, "f", call.getPredecessor());
    CallstackState mismatching = new CallstackState(caller, "f", node);
    CallstackTransferRelationBackwards backwards =
        new CallstackTransferRelationBackwards(
            new CallstackOptions(Configuration.defaultConfiguration()),
            LogManager.createTestLogManager());
    DssCallstackEffect effect = DssCallstackEffect.EMPTY.append(call);
    assertThat(effect.accepts(matching, backwards, SingletonPrecision.getInstance())).isTrue();
    assertThat(effect.accepts(mismatching, backwards, SingletonPrecision.getInstance())).isFalse();
  }

  @Test
  public void matchedCallsCancelOnlyWithoutRecursion() throws Exception {
    // paths that call different functions on the way to the same location have to be mergeable
    assertThat(effectAfterCallAndReturn("void f() {} int main() { f(); }"))
        .isEqualTo(DssCallstackEffect.EMPTY);
    // with recursion, the backwards replay still has to see the call to report it
    assertThat(effectAfterCallAndReturn("void f() { f(); } int main() { f(); }"))
        .isNotEqualTo(DssCallstackEffect.EMPTY);
  }

  private static DssCallstackEffect effectAfterCallAndReturn(String pProgram) throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString(pProgram);
    DssCallstackCPA cpa =
        new DssCallstackCPA(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), cfa);
    FunctionCallEdge call =
        CFAUtils.allEdges(cfa)
            .filter(FunctionCallEdge.class)
            .firstMatch(edge -> edge.getPredecessor().getFunctionName().equals("main"))
            .get();
    FunctionReturnEdge returnEdge =
        call.getSuccessor()
            .getExitNode()
            .orElseThrow()
            .getLeavingReturnEdges()
            .filter(FunctionReturnEdge.class)
            .firstMatch(edge -> edge.getSummaryEdge().equals(call.getSummaryEdge()))
            .get();
    TransferRelation transfer = cpa.getTransferRelation();
    AbstractState afterCall =
        Iterables.getOnlyElement(
            transfer.getAbstractSuccessorsForEdge(
                cpa.createState(null, "main", cfa.getMainFunction(), false),
                SingletonPrecision.getInstance(),
                call));
    AbstractState afterReturn =
        Iterables.getOnlyElement(
            transfer.getAbstractSuccessorsForEdge(
                afterCall, SingletonPrecision.getInstance(), returnEdge));
    return ((DssCallstackState) afterReturn).getEffect();
  }

  @Test
  public void configuredPccDomainCannotBypassDssCompatibility() throws Exception {
    DssCallstackCPA cpa =
        new DssCallstackCPA(
            Configuration.builder().setOption("cpa.callstack.domain", "FLATPCC").build(),
            LogManager.createTestLogManager(),
            TestCfaUtils.makeCfaFromString("int main() { return 0; }"));
    AbstractState unknown = new DssCallstackState(stack, true);
    assertThat(
            cpa.getStopOperator()
                .stop(state, ImmutableList.of(unknown), SingletonPrecision.getInstance()))
        .isFalse();
    assertThat(cpa.isCoveredBy(state, unknown)).isFalse();
    assertThat(cpa.isCoveredByRecursiveState(state, unknown)).isFalse();
  }
}
