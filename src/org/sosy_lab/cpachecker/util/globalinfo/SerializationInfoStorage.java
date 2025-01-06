// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.globalinfo;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.apron.ApronCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.ApronManager;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializationInfoStorage {

  private static final ThreadLocal<SerializationInfoStorage> serializationInformationThreadLocal =
      ThreadLocal.withInitial(() -> null);
  private CFAInfo cfaInfo;
  private final AutomatonInfo automatonInfo = new AutomatonInfo();
  private FormulaManagerView predicateFormulaManagerView;
  private FormulaManagerView assumptionFormulaManagerView;
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager apronLogger;

  private SerializationInfoStorage() {}

  public static synchronized SerializationInfoStorage getInstance() {
    SerializationInfoStorage instance = serializationInformationThreadLocal.get();
    if (instance == null) {
      throw new AssertionError(
          "The serialization information was not set. storeSerializationInformation() needs to be"
              + " called before (de-)serialization is started.");
    }
    return instance;
  }

  public synchronized Optional<CFAInfo> getCFAInfo() {
    return Optional.ofNullable(cfaInfo);
  }

  public static synchronized void storeSerializationInformation(
      ConfigurableProgramAnalysis pCpa, CFA pCFA) {
    checkState(
        serializationInformationThreadLocal.get() == null,
        "Clear the global state before accessing this method.");
    SerializationInfoStorage info = new SerializationInfoStorage();
    serializationInformationThreadLocal.set(info);
    info.cfaInfo = new CFAInfo(pCFA);
    if (pCpa != null) {
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(pCpa)) {
        if (c instanceof ControlAutomatonCPA controlAutomatonCPA) {
          controlAutomatonCPA.registerInAutomatonInfo(info.automatonInfo);
        } else if (c instanceof ApronCPA apron) {
          info.apronManager = apron.getManager();
          info.apronLogger = apron.getLogger();
        } else if (c instanceof AssumptionStorageCPA assumptionStorageCPA) {
          // override the existing manager
          info.assumptionFormulaManagerView = assumptionStorageCPA.getFormulaManager();
        } else if (c instanceof PredicateCPA predicateCPA) {
          info.absManager = predicateCPA.getAbstractionManager();
          info.predicateFormulaManagerView = predicateCPA.getSolver().getFormulaManager();
        } else if (c instanceof LocationCPA locationCPA) {
          info.cfaInfo.storeLocationStateFactory(locationCPA.getStateFactory());
        } else if (c instanceof LocationCPABackwards locationCPA) {
          info.cfaInfo.storeLocationStateFactory(locationCPA.getStateFactory());
        }
      }
    }
  }

  public static synchronized void clear() {
    checkState(
        serializationInformationThreadLocal.get() != null,
        "Cannot clear a non-existing SerializationInfoStorage.");
    serializationInformationThreadLocal.set(null);
  }

  public static synchronized boolean isSet() {
    return serializationInformationThreadLocal.get() != null;
  }

  public AutomatonInfo getAutomatonInfo() {
    return automatonInfo;
  }

  public FormulaManagerView getPredicateFormulaManagerView() {
    return checkNotNull(predicateFormulaManagerView);
  }

  public AbstractionManager getAbstractionManager() {
    return checkNotNull(absManager);
  }

  public ApronManager getApronManager() {
    return checkNotNull(apronManager);
  }

  public LogManager getApronLogManager() {
    return checkNotNull(apronLogger);
  }

  public FormulaManagerView getAssumptionStorageFormulaManager() {
    return checkNotNull(assumptionFormulaManagerView);
  }
}
