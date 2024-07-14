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
public class Sequentializer {

  private final Configuration config;

  private final MutableNetwork<CFANode, CFAEdge> mutableNetwork;

  private final CFAToCTranslator cfaToCTranslator;

  public Sequentializer(Configuration pConfig) throws InvalidConfigurationException {
    config = pConfig;
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
}
