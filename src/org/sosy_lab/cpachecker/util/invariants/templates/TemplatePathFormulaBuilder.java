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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.invariants.templates.manager.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.invariants.templates.manager.TemplateFormulaManager.TemplateParseMode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

public class TemplatePathFormulaBuilder {

  private PathFormulaManagerImpl pfmgr;

  @SuppressWarnings("deprecation")
  public TemplatePathFormulaBuilder() {
    // Use this constructor if you only want a default configuration.

    Configuration config = Configuration.defaultConfiguration();
    LogManager logger;

    try {
      logger = new BasicLogManager(config);
      FormulaManager fmgr = new TemplateFormulaManager(TemplateParseMode.PATHFORMULA);
      FormulaManagerView efmgr = new FormulaManagerView(fmgr, config, logger);
      pfmgr = new PathFormulaManagerImpl(efmgr, config, logger, MachineModel.LINUX32);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

  }

  @SuppressWarnings("deprecation")
  public TemplatePathFormulaBuilder(Configuration config,
                    LogManager logger, MachineModel machineModel) {
    // Use this constructor if you have a config and logger already.

    try {
      FormulaManager fmgr = new TemplateFormulaManager(TemplateParseMode.PATHFORMULA);
      FormulaManagerView efmgr = new FormulaManagerView(fmgr, config, logger);
      pfmgr = new PathFormulaManagerImpl(efmgr, config, logger, machineModel);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

  }

  /**
   * Pass any number (or an array) of CFAEdges, and get the path
   * formula for the path following these edges in order.
   */
  public PathFormula buildPathFormula(CFAEdge... E) {
    PathFormula pf = null;
    try {
      pf = pfmgr.makeEmptyPathFormula();
      for (int i = 0; i < E.length; i++) {
        pf = pfmgr.makeAnd(pf, E[i]);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return pf;
  }

  public PathFormula buildPathFormula(List<CFAEdge> L) {
    CFAEdge[] A = new CFAEdge[L.size()];
    for (int i = 0; i < L.size(); i++) {
      A[i] = L.get(i);
    }
    return buildPathFormula(A);
  }

}