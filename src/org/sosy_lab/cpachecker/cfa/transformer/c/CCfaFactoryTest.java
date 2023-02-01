// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/** Tests for {@link CCfaFactory}. */
public final class CCfaFactoryTest {

  /** Returns a new dummy CFA that contains a single node. */
  private static CFA createSingleNodeDummyCfa() {
    FunctionExitNode mainFunctionExitNode = new FunctionExitNode(CFunctionDeclaration.DUMMY);
    FunctionEntryNode mainFunctionEntryNode =
        new CFunctionEntryNode(
            FileLocation.DUMMY, CFunctionDeclaration.DUMMY, mainFunctionExitNode, Optional.empty());
    CfaMetadata cfaMetadata =
        CfaMetadata.forMandatoryAttributes(
            MachineModel.ARM,
            Language.C,
            ImmutableList.of(),
            mainFunctionEntryNode,
            CfaConnectedness.SUPERGRAPH);
    NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>();
    TreeMultimap<String, CFANode> nodes = TreeMultimap.create();
    functions.put(mainFunctionEntryNode.getFunction().getQualifiedName(), mainFunctionEntryNode);
    nodes.put(mainFunctionEntryNode.getFunction().getQualifiedName(), mainFunctionEntryNode);

    return new MutableCFA(functions, nodes, cfaMetadata);
  }

  @Test
  public void testCCfaFactoryStepOrder() {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFA singleNodeCfa = createSingleNodeDummyCfa();

    // CFA post-processors should be executed in the order they are specified
    CfaFactory cfaFactory =
        CCfaFactory.executePostProcessor(new DummyChainAppendingPostProcessor("step 1"))
            .executePostProcessor(new DummyChainAppendingPostProcessor("step 2"))
            .executePostProcessor(new DummyChainAppendingPostProcessor("step 3"));
    CFA nodeChainCfa =
        cfaFactory.createCfa(
            CfaNetwork.wrap(singleNodeCfa), singleNodeCfa.getMetadata(), logger, shutdownNotifier);

    // We expect the following chain of nodes:
    // [ ] --- "step 1" ---> [ ] --- "step 2" ---> [ ] --- "step 3" ---> [ ]
    CfaNetwork nodeChainNetwork = CfaNetwork.wrap(nodeChainCfa);
    List<CFAEdge> edges = new ArrayList<>();
    CFANode currentNode = nodeChainCfa.getMainFunction();
    while (nodeChainNetwork.successors(currentNode).size() == 1) {
      edges.add(Iterables.getOnlyElement(nodeChainNetwork.outEdges(currentNode)));
      currentNode = Iterables.getOnlyElement(nodeChainNetwork.successors(currentNode));
    }

    assertThat(edges.get(0).getDescription()).isEqualTo("step 1");
    assertThat(edges.get(1).getDescription()).isEqualTo("step 2");
    assertThat(edges.get(2).getDescription()).isEqualTo("step 3");
  }

  /**
   * A CFA post-processor that appends to the end of a dummy node chain CFA, a new node and new
   * blank edge with the specified description.
   */
  private static final class DummyChainAppendingPostProcessor implements CfaPostProcessor {

    private final String description;

    private DummyChainAppendingPostProcessor(String pDescription) {
      description = pDescription;
    }

    @Override
    public MutableCFA execute(
        MutableCFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
      CFANode currentNode = pCfa.getMetadata().getMainFunctionEntry();
      MutableCfaNetwork nodeChainNetwork = MutableCfaNetwork.wrap(pCfa);

      while (nodeChainNetwork.successors(currentNode).size() == 1) {
        currentNode = Iterables.getOnlyElement(nodeChainNetwork.successors(currentNode));
      }

      String functionName = currentNode.getFunction().getQualifiedName();
      nodeChainNetwork.addEdge(
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              currentNode,
              CFANode.newDummyCFANode(functionName),
              description));

      return pCfa;
    }
  }
}
