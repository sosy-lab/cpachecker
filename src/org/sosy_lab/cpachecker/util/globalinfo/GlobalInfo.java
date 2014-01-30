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
package org.sosy_lab.cpachecker.util.globalinfo;

import java.io.Serializable;
import java.util.ArrayList;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


public class GlobalInfo {
  private static GlobalInfo instance;
  private CFAInfo cfaInfo;
  private AutomatonInfo automatonInfo = new AutomatonInfo();
  private FormulaManagerView formulaManager;
  private ArrayList<Serializable> helperStorages = new ArrayList<>();

  private GlobalInfo() {

  }

  public static GlobalInfo getInstance() {
    if (instance == null) {
      instance = new GlobalInfo();
    }
    return instance;
  }

  public void storeCFA(CFA cfa) {
    cfaInfo = new CFAInfo(cfa);
  }

  public Optional<CFAInfo> getCFAInfo() {
    return Optional.fromNullable(cfaInfo);
  }

  public void storeAutomaton(Automaton automaton) {
    automatonInfo.register(automaton);
  }

  public AutomatonInfo getAutomatonInfo() {
    Preconditions.checkState(automatonInfo != null);
    return automatonInfo;
  }

  public void storeFormulaManager(FormulaManagerView formulaManager) {
    this.formulaManager = formulaManager;
  }

  public FormulaManagerView getFormulaManager() {
    Preconditions.checkState(formulaManager != null);
    return formulaManager;
  }

  public int addHelperStorage(Serializable e) {
    helperStorages.add(e);
    return helperStorages.size() - 1;
  }

  public Serializable getHelperStorage(int index) {
    return helperStorages.get(index);
  }

  public int getNumberOfHelperStorages() {
    return helperStorages.size();
  }
}
