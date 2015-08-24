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
package org.sosy_lab.cpachecker.util.globalinfo;

import java.io.Serializable;
import java.util.ArrayList;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.apron.ApronManager;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.solver.api.FormulaManager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


public class GlobalInfo {
  private static GlobalInfo instance;
  private CFAInfo cfaInfo;
  private AutomatonInfo automatonInfo = new AutomatonInfo();
  private ConfigurableProgramAnalysis cpa;
  private FormulaManager formulaManager;
  private FormulaManagerView formulaManagerView;
  private ArrayList<Serializable> helperStorages = new ArrayList<>();
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager logger;

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

  public void storeAutomaton(Automaton automaton, ControlAutomatonCPA automatonCPA) {
    automatonInfo.register(automaton, automatonCPA);
  }

  public AutomatonInfo getAutomatonInfo() {
    Preconditions.checkState(automatonInfo != null);
    return automatonInfo;
  }

  public void storeCPA(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
  }

  public Optional<ConfigurableProgramAnalysis> getCPA() {
    return Optional.fromNullable(cpa);
  }

  public void storeFormulaManager(FormulaManager pFormulaManager) {
    formulaManager = pFormulaManager;
  }

  public void storeFormulaManagerView(FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  public void storeAbstractionManager(AbstractionManager absManager) {
    this.absManager = absManager;
  }

  public void storeApronManager(ApronManager pApronManager) {
    apronManager = pApronManager;
  }

  public void storeLogManager(LogManager pLogManager) {
    logger = pLogManager;
  }

  public FormulaManager getFormulaManager() {
    Preconditions.checkState(formulaManager != null);
    return formulaManager;
  }

  public FormulaManagerView getFormulaManagerView() {
    Preconditions.checkState(formulaManagerView != null);
    return formulaManagerView;
  }

  public AbstractionManager getAbstractionManager() {
    Preconditions.checkState(absManager != null);
    return absManager;
  }

  public ApronManager getApronManager() {
    return apronManager;
  }

  public LogManager getLogManager() {
    return logger;
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
