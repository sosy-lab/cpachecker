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
package org.sosy_lab.cpachecker.cpa.wp.segkro;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.wp.segkro.interfaces.Rule;
import org.sosy_lab.cpachecker.cpa.wp.segkro.rules.RulesetFactory;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Lists;


public class ExtractNewPredsTest {

  private FormulaManager formulaManager;
  private FormulaManagerView fmgr;
  private Solver solver;

  private ExtractNewPreds enp;

  @Before
  public void setUp() throws Exception {
    Configuration.defaultConfiguration();

    Configuration config = Configuration
        .builder()
        .setOption("cpa.predicate.solver", "Z3")
        .build();

    FormulaManagerFactory factory = new FormulaManagerFactory(config, TestLogManager.getInstance(), ShutdownNotifier.create());
    formulaManager = factory.getFormulaManager();
    fmgr = new FormulaManagerView(formulaManager, config, TestLogManager.getInstance());
    solver = new Solver(fmgr, factory);

    List<Rule> rules = Lists.newArrayList();
    rules = RulesetFactory.createRuleset();

    enp = new ExtractNewPreds(rules);
  }

  @Test
  public void test() {
    // enp.extractNewPreds(pInputFormula)
  }

}
