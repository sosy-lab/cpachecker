// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
  private FormulaManagerView predicateFormulaManagerView;
  private FormulaManagerView assumptionFormulaManagerView;
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager apronLogger;

  private GlobalInfo() {}

  public static synchronized GlobalInfo getInstance() {
    if (instance == null) {
      instance = new GlobalInfo();
    }
    return instance;
  }

  public synchronized void storeCFA(CFA cfa) {
    cfaInfo = new CFAInfo(cfa);
  }

  public synchronized Optional<CFAInfo> getCFAInfo() {
    return Optional.ofNullable(cfaInfo);
  }

  public synchronized void setUpInfoFromCPA(ConfigurableProgramAnalysis pCpa) {
    absManager = null;
    apronManager = null;
    apronLogger = null;
    if (pCpa != null) {
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(pCpa)) {
        if (c instanceof ControlAutomatonCPA) {
          ((ControlAutomatonCPA) c).registerInAutomatonInfo(automatonInfo);
        } else if (c instanceof ApronCPA) {
          Preconditions.checkState(apronManager == null && apronLogger == null);
          ApronCPA apron = (ApronCPA) c;
          apronManager = apron.getManager();
          apronLogger = apron.getLogger();
        } else if (c instanceof AssumptionStorageCPA) {
          // override the existing manager
          assumptionFormulaManagerView = ((AssumptionStorageCPA) c).getFormulaManager();
        } else if (c instanceof PredicateCPA) {
          Preconditions.checkState(absManager == null);
          absManager = ((PredicateCPA) c).getAbstractionManager();
          predicateFormulaManagerView = ((PredicateCPA) c).getSolver().getFormulaManager();
        }
      }
    }
  }

  public synchronized AutomatonInfo getAutomatonInfo() {
    Preconditions.checkState(automatonInfo != null);
    return automatonInfo;
  }

  public synchronized FormulaManagerView getPredicateFormulaManagerView() {
    Preconditions.checkState(predicateFormulaManagerView != null);
    return predicateFormulaManagerView;
  }

  public synchronized AbstractionManager getAbstractionManager() {
    Preconditions.checkState(absManager != null);
    return absManager;
  }

  public synchronized ApronManager getApronManager() {
    return apronManager;
  }

  public synchronized LogManager getApronLogManager() {
    return apronLogger;
  }

  public synchronized FormulaManagerView getAssumptionStorageFormulaManager() {
    Preconditions.checkState(assumptionFormulaManagerView != null);
    return assumptionFormulaManagerView;
  }
}
