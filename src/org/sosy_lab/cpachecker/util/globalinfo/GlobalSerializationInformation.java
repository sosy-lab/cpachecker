// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.globalinfo;

import static com.google.common.base.Preconditions.checkState;

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

public class GlobalSerializationInformation {

  private static final ThreadLocal<GlobalSerializationInformation>
      serializationInformationThreadLocal = ThreadLocal.withInitial(() -> null);
  private CFAInfo cfaInfo;
  private final AutomatonInfo automatonInfo = new AutomatonInfo();
  private FormulaManagerView predicateFormulaManagerView;
  private FormulaManagerView assumptionFormulaManagerView;
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager apronLogger;

  private GlobalSerializationInformation() {}

  private static synchronized GlobalSerializationInformation createOrGetInstance() {
    GlobalSerializationInformation instance = serializationInformationThreadLocal.get();
    if (instance != null) {
      return instance;
    }
    instance = new GlobalSerializationInformation();
    serializationInformationThreadLocal.set(instance);
    return instance;
  }

  public static synchronized Optional<GlobalSerializationInformation> getWrappedInstance() {
    return Optional.ofNullable(serializationInformationThreadLocal.get());
  }

  public static synchronized GlobalSerializationInformation getInstance() {
    GlobalSerializationInformation instance = serializationInformationThreadLocal.get();
    if (instance == null) {
      throw new AssertionError(
          "There is no instance of GlobalSerializationInformation (instance of"
              + " GlobalSerializationInfo is null)");
    }
    return instance;
  }

  public synchronized Optional<CFAInfo> getCFAInfo() {
    return Optional.ofNullable(cfaInfo);
  }

  public static synchronized void writeSerializationInformation(
      ConfigurableProgramAnalysis pCpa, CFA pCFA) {
    GlobalSerializationInformation info = createOrGetInstance();
    checkState(info.cfaInfo == null, "Clear the global state before accessing this method.");
    checkState(
        info.predicateFormulaManagerView == null,
        "Clear the global state before accessing this method.");
    checkState(
        info.assumptionFormulaManagerView == null,
        "Clear the global state before accessing this method.");
    checkState(info.absManager == null, "Clear the global state before accessing this method.");
    checkState(info.apronManager == null, "Clear the global state before accessing this method.");
    checkState(info.apronLogger == null, "Clear the global state before accessing this method.");
    info.cfaInfo = new CFAInfo(pCFA);
    if (pCpa != null) {
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(pCpa)) {
        if (c instanceof ControlAutomatonCPA) {
          ((ControlAutomatonCPA) c).registerInAutomatonInfo(info.automatonInfo);
        } else if (c instanceof ApronCPA) {
          ApronCPA apron = (ApronCPA) c;
          info.apronManager = apron.getManager();
          info.apronLogger = apron.getLogger();
        } else if (c instanceof AssumptionStorageCPA) {
          // override the existing manager
          info.assumptionFormulaManagerView = ((AssumptionStorageCPA) c).getFormulaManager();
        } else if (c instanceof PredicateCPA) {
          info.absManager = ((PredicateCPA) c).getAbstractionManager();
          info.predicateFormulaManagerView = ((PredicateCPA) c).getSolver().getFormulaManager();
        }
      }
    }
  }

  public static synchronized void clear() {
    GlobalSerializationInformation info = getInstance();
    boolean atLeastOneAttributeWasSet =
        info.cfaInfo != null
            || info.predicateFormulaManagerView != null
            || info.assumptionFormulaManagerView != null
            || info.absManager != null
            || info.apronManager != null
            || info.apronLogger != null;
    Preconditions.checkState(
        atLeastOneAttributeWasSet, "Cannot clear an already empty GlobalSerializationInformation");
    info.cfaInfo = null;
    info.predicateFormulaManagerView = null;
    info.assumptionFormulaManagerView = null;
    info.absManager = null;
    info.apronManager = null;
    info.apronLogger = null;
    // TODO: or maybe instance = null;
  }

  public synchronized AutomatonInfo getAutomatonInfo() {
    return automatonInfo;
  }

  public synchronized FormulaManagerView getPredicateFormulaManagerView() {
    checkState(predicateFormulaManagerView != null);
    return predicateFormulaManagerView;
  }

  public synchronized AbstractionManager getAbstractionManager() {
    checkState(absManager != null);
    return absManager;
  }

  public synchronized ApronManager getApronManager() {
    checkState(apronManager != null);
    return apronManager;
  }

  public synchronized LogManager getApronLogManager() {
    checkState(apronLogger != null);
    return apronLogger;
  }

  public synchronized FormulaManagerView getAssumptionStorageFormulaManager() {
    checkState(assumptionFormulaManagerView != null);
    return assumptionFormulaManagerView;
  }
}
