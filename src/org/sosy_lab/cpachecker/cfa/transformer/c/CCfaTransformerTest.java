// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Tests CFA transformations of C programs. */
@RunWith(Parameterized.class)
public final class CCfaTransformerTest {

  @Parameter public String sourceFilePath;

  @Parameters
  public static Iterable<? extends Object> data() {
    // A couple of programs that a somewhat big and (hopefully) contain a wide variety of CFA
    // nodes/edges and connections between them.
    return ImmutableList.of(
        "test/programs/benchmarks/ldv-linux-3.0/module_get_put-drivers-net-ppp_generic.ko.cil.out.i",
        "test/programs/benchmarks/ldv-linux-3.0/module_get_put-drivers-staging-et131x-et131x.ko.cil.out.i",
        "test/programs/benchmarks/ldv-linux-3.0/module_get_put-drivers-net-atl1c-atl1c.ko.cil.out.i");
  }

  private static void assertCfaNodeEquivalence(
      CFANode pSomeNode, CFANode pOtherNode, Equivalence<CFANode> pNodeEquivalence) {
    assertWithMessage("Expected CFA nodes to be equivalent: %s and %s", pSomeNode, pOtherNode)
        .that(pNodeEquivalence.equivalent(pSomeNode, pOtherNode))
        .isTrue();
  }

  private static void assertCfaEdgeEquivalence(
      CFAEdge pSomeEdge, CFAEdge pOtherEdge, Equivalence<CFAEdge> pEdgeEquivalence) {
    assertWithMessage("Expected CFA edges to be equivalent: %s and %s", pSomeEdge, pOtherEdge)
        .that(pEdgeEquivalence.equivalent(pSomeEdge, pOtherEdge))
        .isTrue();
  }

  private static void assertCfaEquivalence(
      CFA pSomeCfa,
      CFA pOtherCfa,
      Equivalence<CFANode> pNodeEquivalence,
      Equivalence<CFAEdge> pEdgeEquivalence) {

    Set<CFANode> someWaitlisted = new HashSet<>(ImmutableList.of(pSomeCfa.getMainFunction()));
    Deque<CFANode> someWaitlist = new ArrayDeque<>(someWaitlisted);
    Set<CFANode> otherWaitlisted = new HashSet<>(ImmutableList.of(pOtherCfa.getMainFunction()));
    Deque<CFANode> otherWaitlist = new ArrayDeque<>(otherWaitlisted);

    while (!someWaitlist.isEmpty()) {

      CFANode someNode = someWaitlist.remove();
      CFANode otherNode = otherWaitlist.remove();
      assertCfaNodeEquivalence(someNode, otherNode, pNodeEquivalence);

      CFAEdge[] someLeavingEdges = CFAUtils.allLeavingEdges(someNode).toArray(CFAEdge.class);
      Arrays.sort(someLeavingEdges, new MultipleLeavingEdgesOrder());
      CFAEdge[] otherLeavingEdges = CFAUtils.allLeavingEdges(otherNode).toArray(CFAEdge.class);
      Arrays.sort(otherLeavingEdges, new MultipleLeavingEdgesOrder());

      assertWithMessage(
              "Expected CFA nodes to have the same number of leaving edges: %s and %s",
              someLeavingEdges.length, otherLeavingEdges.length)
          .that(someLeavingEdges.length == otherLeavingEdges.length)
          .isTrue();

      for (int index = 0; index < someLeavingEdges.length; index++) {

        CFAEdge someLeavingEdge = someLeavingEdges[index];
        CFAEdge otherLeavingEdge = otherLeavingEdges[index];
        assertCfaEdgeEquivalence(someLeavingEdge, otherLeavingEdge, pEdgeEquivalence);

        CFANode someSuccessor = someLeavingEdge.getSuccessor();
        CFANode otherSuccessor = otherLeavingEdge.getSuccessor();

        assertWithMessage(
                "Expected both CFA nodes to be either waitlisted or not: %s and %s",
                someWaitlisted.contains(someSuccessor), otherWaitlisted.contains(otherSuccessor))
            .that(
                someWaitlisted.contains(someSuccessor) == otherWaitlisted.contains(otherSuccessor))
            .isTrue();

        if (someWaitlisted.add(someSuccessor)) {
          someWaitlist.add(someSuccessor);
        }
        if (otherWaitlisted.add(otherSuccessor)) {
          otherWaitlist.add(otherSuccessor);
        }
      }
    }
  }

  @Test
  public void testCloningCCfaTransformer()
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    Configuration config = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA originalCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(sourceFilePath));

    CFA cloneCfa = new CloningCCfaTransformer().transform(originalCfa, logger, shutdownNotifier);

    assertCfaEquivalence(
        originalCfa, cloneCfa, new CfaNodeCloneEquivalence(), new CfaEdgeCloneEquivalence());
  }

  /** A simple CFA transformer that just clones a CFA without performing any changes. */
  private static final class CloningCCfaTransformer implements CfaTransformer {

    @Override
    public CFA transform(
        CfaNetwork pCfaNetwork,
        CfaMetadata pCfaMetadata,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier) {

      return CCfaFactory.CLONER.createCfa(pCfaNetwork, pCfaMetadata, pLogger, pShutdownNotifier);
    }
  }

  /** Defines the equivalence for comparing CFA nodes with their clones. */
  private static final class CfaNodeCloneEquivalence extends Equivalence<CFANode> {

    @Override
    protected boolean doEquivalent(CFANode pSomeNode, CFANode pOtherNode) {
      return pSomeNode.getClass().equals(pOtherNode.getClass())
          && pSomeNode.getFunction().equals(pOtherNode.getFunction())
          && pSomeNode.getNumEnteringEdges() == pOtherNode.getNumEnteringEdges()
          && pSomeNode.getNumLeavingEdges() == pOtherNode.getNumLeavingEdges()
          && (pSomeNode.getEnteringSummaryEdge() == null)
              == (pOtherNode.getEnteringSummaryEdge() == null)
          && (pSomeNode.getLeavingSummaryEdge() == null)
              == (pOtherNode.getLeavingSummaryEdge() == null);
    }

    @Override
    protected int doHash(CFANode pNode) {
      return Objects.hash(
          pNode.getClass(),
          pNode.getFunction(),
          pNode.getNumEnteringEdges(),
          pNode.getNumLeavingEdges(),
          pNode.getEnteringSummaryEdge() == null,
          pNode.getLeavingSummaryEdge() == null);
    }
  }

  /** Defines the equivalence for comparing CFA edges with their clones. */
  private static final class CfaEdgeCloneEquivalence extends Equivalence<CFAEdge> {

    @Override
    protected boolean doEquivalent(CFAEdge pSomeEdge, CFAEdge pOtherEdge) {
      return pSomeEdge.getClass().equals(pOtherEdge.getClass())
          && pSomeEdge.getRawAST().equals(pOtherEdge.getRawAST());
    }

    @Override
    protected int doHash(CFAEdge pEdge) {
      return Objects.hash(pEdge.getClass(), pEdge.getRawAST());
    }
  }

  /** Defines a order of CFA edges for the case that a node has multiple leaving edges. */
  private static final class MultipleLeavingEdgesOrder implements Comparator<CFAEdge> {

    @Override
    // Regular assert statements are used for things that shouldn't be tested by
    // `CCfaTransformerTest`. Fix the test if any of those assertion fails.
    @SuppressWarnings("UseCorrectAssertInTests")
    public int compare(CFAEdge pSomeEdge, CFAEdge pOtherEdge) {

      // summary edge first
      if (pSomeEdge instanceof FunctionSummaryEdge) {
        assert !(pOtherEdge instanceof FunctionSummaryEdge);
        return -1;
      }
      if (pOtherEdge instanceof FunctionSummaryEdge) {
        assert !(pSomeEdge instanceof FunctionSummaryEdge);
        return 1;
      }

      // true assume edge first
      if (pSomeEdge instanceof AssumeEdge && pOtherEdge instanceof AssumeEdge) {
        AssumeEdge someAssumeEdge = (AssumeEdge) pSomeEdge;
        AssumeEdge otherAssumeEdge = (AssumeEdge) pOtherEdge;
        if (someAssumeEdge.getTruthAssumption()) {
          assert !otherAssumeEdge.getTruthAssumption();
          return -1;
        }
        if (otherAssumeEdge.getTruthAssumption()) {
          assert !someAssumeEdge.getTruthAssumption();
          return 1;
        }
      }

      // compare function return edges by their file location
      if (pSomeEdge instanceof FunctionReturnEdge && pOtherEdge instanceof FunctionReturnEdge) {
        int comparisonResult = pSomeEdge.getFileLocation().compareTo(pOtherEdge.getFileLocation());
        assert comparisonResult != 0;
      }

      throw new AssertionError("Unable to order edges: " + pSomeEdge + " and " + pOtherEdge);
    }
  }
}
