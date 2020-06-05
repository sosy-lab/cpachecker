// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.pixelexport.GraphLevel;
import org.sosy_lab.cpachecker.util.pixelexport.GraphToPixelsWriter;
import org.sosy_lab.cpachecker.util.pixelexport.SimpleGraphLevel;

public class CFAToPixelsWriter extends GraphToPixelsWriter<CFANode> {

  public CFAToPixelsWriter(Configuration pConfig) throws InvalidConfigurationException {
    super(pConfig);
  }

  @Override
  public GraphLevel.Builder<CFANode> getLevelBuilder() {
    return new SimpleGraphLevel.Builder<>();
  }

  @Override
  public Iterable<CFANode> getChildren(final CFANode pParent) {
    return CFAUtils.allSuccessorsOf(pParent);
  }
}
