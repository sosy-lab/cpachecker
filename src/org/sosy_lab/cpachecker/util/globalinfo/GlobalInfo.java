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

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.apron.ApronCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.ApronManager;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;


public class GlobalInfo {
  private static GlobalInfo instance;
  private CFAInfo cfaInfo;
  private AutomatonInfo automatonInfo = new AutomatonInfo();
  private ConfigurableProgramAnalysis cpa;
  private FormulaManagerView predicateFormulaManagerView;
  private FormulaManagerView assumptionFormulaManagerView;
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager apronLogger;

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
    return Optional.ofNullable(cfaInfo);
  }


  public Optional<ConfigurableProgramAnalysis> getCPA() {
    return Optional.ofNullable(cpa);
  }

  public synchronized void setUpInfoFromCPA(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
    absManager = null;
    apronManager = null;
    apronLogger = null;
    if (cpa != null) {
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)) {
        if (c instanceof ControlAutomatonCPA) {
          ((ControlAutomatonCPA) c).registerInAutomatonInfo(automatonInfo);
        } else if (c instanceof ApronCPA) {
          Preconditions.checkState(apronManager == null && apronLogger == null);
          ApronCPA apron = (ApronCPA) c;
          apronManager = apron.getManager();
          apronLogger = apron.getLogger();
        } else if (c instanceof AssumptionStorageCPA) {
          assumptionFormulaManagerView = ((AssumptionStorageCPA) c).getFormulaManager();
        } else if (c instanceof PredicateCPA) {
          Preconditions.checkState(absManager == null);
          absManager = ((PredicateCPA) c).getAbstractionManager();
          predicateFormulaManagerView = ((PredicateCPA) c).getSolver().getFormulaManager();
        } else if (c instanceof AssumptionStorageCPA) {
          Preconditions.checkState(assumptionFormulaManagerView == null);
          assumptionFormulaManagerView = ((AssumptionStorageCPA) c).getFormulaManager();
        }
      }
    }
  }

  public AutomatonInfo getAutomatonInfo() {
    Preconditions.checkState(automatonInfo != null);
    return automatonInfo;
  }

  public FormulaManagerView getPredicateFormulaManagerView() {
    Preconditions.checkState(predicateFormulaManagerView != null);
    return predicateFormulaManagerView;
  }

  public AbstractionManager getAbstractionManager() {
    Preconditions.checkState(absManager != null);
    return absManager;
  }

  public ApronManager getApronManager() {
    return apronManager;
  }

  public LogManager getApronLogManager() {
    return apronLogger;
  }

  public FormulaManagerView getAssumptionStorageFormulaManager() {
    Preconditions.checkState(assumptionFormulaManagerView != null);
    return assumptionFormulaManagerView;
  }

}
