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
package org.sosy_lab.cpachecker.cpa.predicate.blocking;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.interfaces.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

@Options(prefix="cpa.predicate.blk.label")
public class LabelBlockOperator extends AbstractBlockOperator implements BlockOperator {

  @Option(description="Name of the label that indicates abstraction an abstraction point.")
  private String abstractionLabelName = "ABSTRACT";

  @Option(description="Delimiter between the first part of the label name an its suffix.")
  private String labelSuffixDelim = "_";

  @Option(description="List of suffixes (separated by comma) that valid abstraction labels should have.")
  private String abstractionLabelSuffixes = "";
  private final Set<String> abstractionLabelNames;

  public LabelBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCFA);
    pConfig.inject(this);

    this.abstractionLabelNames = new HashSet<String>();
    this.abstractionLabelNames.add(this.abstractionLabelName);
    for (String suffix: abstractionLabelSuffixes.split(",")) {
      this.abstractionLabelNames.add((this.abstractionLabelName + labelSuffixDelim + suffix.trim()).toLowerCase());
    }
  }

  @Override
  public boolean isBlockEnd(AbstractElement pElement, CFAEdge pCfaEdge, PathFormula pPf) {
    CFANode succNode = pCfaEdge.getSuccessor();
    if (succNode instanceof CFALabelNode) {
      String label = ((CFALabelNode) succNode).getLabel().toLowerCase();
      if (this.abstractionLabelNames.contains(label)) {
        return true;
      }

    }
    return false;
  }

}
