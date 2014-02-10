/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants;

import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.invariants.templates.manager.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.collect.ImmutableList;

// "Formula Manager tester"
public class FMtester {

  private static String testpath = "/home/skieffer/sosy-lab/test_programs/";

  // Choose a test loop:
  private static int loopnum = 12;

  private static String testfile = testpath+"loop"+Integer.toString(loopnum)+".c";

  private static PathFormulaManagerImpl pfmgr;

  public static void main(String[] args) {

    // delcare and construct the basic objects
    //Dialect dialect = Dialect.GNUC;
    Configuration config = Configuration.defaultConfiguration();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    LogManager logger;
    CFACreator cfac;
    FunctionEntryNode root = null;

    // declare FormulaManagers
    TemplateFormulaManager fmgr = null;
    //

    try {
      logger = new BasicLogManager(config);
      cfac = new CFACreator(config, logger, shutdownNotifier);
      CFA cfa = cfac.parseFileAndCreateCFA(ImmutableList.of(testfile));
      root = cfa.getMainFunction();

      // construct FormulaManager, and extended one
      fmgr = new TemplateFormulaManager();
      FormulaManagerView emgr = new FormulaManagerView(fmgr, config, logger);
      //

      pfmgr = new PathFormulaManagerImpl(emgr, config, logger, cfa);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

    List<CFAEdge> edgeSet = getEdgeSet(root);

    for (CFAEdge e : edgeSet) {
      PathFormula pf = makepf(e);
      System.out.println(pf);
    }

  }

  private static List<CFAEdge> getEdgeSet(FunctionEntryNode root) {
    CFATraversal.EdgeCollectingCFAVisitor visitor = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs()
                .ignoreSummaryEdges()
                .traverse(root, visitor);
    return visitor.getVisitedEdges();
  }

  private static PathFormula makepf(CFAEdge edge) {
    PathFormula empty = null;
    PathFormula pf = null;
    try {
      empty = pfmgr.makeEmptyPathFormula();
      pf = pfmgr.makeAnd(empty, edge);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return pf;
  }

}
