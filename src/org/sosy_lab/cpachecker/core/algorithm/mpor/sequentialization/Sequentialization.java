// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  private final Configuration config;

  private final MutableNetwork<CFANode, CFAEdge> mutableNetwork;

  private final CFAToCTranslator cfaToCTranslator;

  public Sequentialization(Configuration pConfig) throws InvalidConfigurationException {
    config = pConfig;
    // TODO allowSelfLoops? should be false based on my current unterstanding
    mutableNetwork = NetworkBuilder.directed().allowsSelfLoops(true).build();
    cfaToCTranslator = new CFAToCTranslator(config);
  }

  public String createCProgram() {
    return null;
    // TODO how do we get from a MutableNetwork to a CFA?
    //  MutableCFA only contains functions for adding nodes, not for adding edges
    //  CfaMutableNetwork takes only a MutableNetwork as a parameter
    //  CCfaTransformer.createCFA takes a CfaMutableNetwork and also other parameters
    //  return cfaToCTranslator.translateCfa(cfa...);
  }

  /**
   * Tries to add pNode to the {@link Sequentialization#mutableNetwork}.
   *
   * @param pNode CFANode to be added
   * @return true if {@link Sequentialization#mutableNetwork} did not contain pNode already
   */
  public boolean addNode(CFANode pNode) {
    return mutableNetwork.addNode(pNode);
  }

  /**
   * Tries to add pEdge to the {@link Sequentialization#mutableNetwork}.
   *
   * @param pPredecessor CFANode whose leaving CFAEdges contains pEdge
   * @param pSuccessor CFANode whose entering CFAEdges contains pEdge
   * @param pEdge CFAEdge to be added
   * @return true if {@link Sequentialization#mutableNetwork} did not contain a CFAEdge from
   *     pPredecessor to pSuccessor already
   */
  public boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pEdge) {
    return mutableNetwork.addEdge(pPredecessor, pSuccessor, pEdge);
  }
}
