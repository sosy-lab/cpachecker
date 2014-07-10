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
package org.sosy_lab.cpachecker.cpa.sign;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithTargetVariable;
import org.sosy_lab.cpachecker.core.interfaces.TargetableWithPredicatedAnalysis;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ImmutableMap;


public class SignState implements AbstractStateWithTargetVariable, TargetableWithPredicatedAnalysis, Serializable {

  private static final long serialVersionUID = -2507059869178203119L;

  private static final boolean DEBUG = false;

  private static SignTargetChecker targetChecker;

  static void init(Configuration config) throws InvalidConfigurationException {
    targetChecker = new SignTargetChecker(config);
  }

  private PersistentMap<String, SIGN> signMap;

  public final static SignState TOP = new SignState();
  private final static SerialProxySign proxy = new SerialProxySign();

  private SignState(PersistentMap<String, SIGN> pSignMap) {
    signMap = pSignMap;
  }

  private SignState() {
    signMap = PathCopyingPersistentTreeMap.of();
  }

  public SignState union(SignState pToJoin) {
    if (pToJoin.equals(this)) { return pToJoin; }
    if (this.equals(TOP) || pToJoin.equals(TOP)) { return TOP; }

    // assure termination of loops do not merge if  pToJoin covers this but return pToJoin
    if (isSubsetOf(pToJoin)) { return pToJoin; }

    SignState result = SignState.TOP;
    PersistentMap<String, SIGN> newMap = PathCopyingPersistentTreeMap.of();
    SIGN combined;
    for (String varIdent : pToJoin.signMap.keySet()) {
      // only add those variables that are contained in both states (otherwise one has value ALL (not saved))
      if (signMap.containsKey(varIdent)) {
        combined = getSignForVariable(varIdent).combineWith(pToJoin.getSignForVariable(varIdent));
        if (!combined.isAll()) {
          newMap = newMap.putAndCopy(varIdent, combined);
        }
      }
    }

    return newMap.size() > 0 ? new SignState(newMap) : result;
  }

  public boolean isSubsetOf(SignState pSuperset) {
    if (pSuperset.equals(this) || pSuperset.equals(TOP)) { return true; }
    if (signMap.size() < pSuperset.signMap.size()) { return false; }
    // is subset if for every variable all sign assumptions are considered in pSuperset
    // check that all variables in superset with SIGN != ALL have no bigger assumptions in subset
    for (String varIdent : pSuperset.signMap.keySet()) {
      if (!getSignForVariable(varIdent).isSubsetOf(pSuperset.getSignForVariable(varIdent))) { return false; }
    }
    return true;
  }

  public SignState enterFunction(ImmutableMap<String, SIGN> pArguments) {
    PersistentMap<String, SIGN> newMap = signMap;

    for (String var : pArguments.keySet()) {
      if (!pArguments.get(var).equals(SIGN.ALL)) {
        newMap = newMap.putAndCopy(var, pArguments.get(var));
      }
    }

    return signMap == newMap ? this : new SignState(newMap);
  }

  public SignState leaveFunction(String pFunctionName) {
    PersistentMap<String, SIGN> newMap = signMap;

    for (String var : signMap.keySet()) {
      if (var.startsWith(pFunctionName + "::")) {
        newMap = newMap.removeAndCopy(var);
      }
    }

    return newMap == signMap ? this : new SignState(newMap);
  }

  public SignState assignSignToVariable(String pVarIdent, SIGN sign) {
    if (sign.isAll()) {
      return signMap.containsKey(pVarIdent) ? new SignState(signMap.removeAndCopy(pVarIdent)) : this;
    }
    return signMap.containsKey(pVarIdent) && getSignForVariable(pVarIdent).equals(sign) ? this
        : new SignState(signMap.putAndCopy(pVarIdent, sign));
  }

  public SignState removeSignAssumptionOfVariable(String pVarIdent) {
    return assignSignToVariable(pVarIdent, SIGN.ALL);
  }

  public SIGN getSignForVariable(String pVarIdent) {
    return signMap.containsKey(pVarIdent) ? signMap.get(pVarIdent) : SIGN.ALL;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    String delim = ", ";
    builder.append("[");
    String loopDelim = "";
    for (String key : signMap.keySet()) {
      if (!DEBUG && (key.matches("\\w*::__CPAchecker_TMP_\\w*") || key.endsWith(SignTransferRelation.FUNC_RET_VAR))) {
        continue;
      }
      builder.append(loopDelim);
      builder.append(key + "->" + getSignForVariable(key));
      loopDelim = delim;
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof SignState)) { return false; }
    return ((SignState) pObj).signMap.equals(this.signMap);
  }

  @Override
  public int hashCode() {
    return signMap.hashCode();
  }

  @Override
  public boolean isTarget() {
    return targetChecker == null ? false : targetChecker.isTarget(this);
  }

  @Override
  public ViolatedProperty getViolatedProperty() throws IllegalStateException {
    if (isTarget()) { return ViolatedProperty.OTHER; }
    return null;
  }

  @Override
  public BooleanFormula getErrorCondition(FormulaManagerView pFmgr) {
    return targetChecker == null ? pFmgr.getBooleanFormulaManager().makeBoolean(false) : targetChecker
        .getErrorCondition(this, pFmgr);
  }

  @Override
  public String getTargetVariableName() {
    return targetChecker == null ? "" : targetChecker.getErrorVariableName();
  }

  private Object writeReplace() throws ObjectStreamException {
    if (this == TOP) {
      return proxy;
    } else {
      return this;
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

  private static class SerialProxySign implements Serializable {

    private static final long serialVersionUID = 2843708585446089623L;

    public SerialProxySign() {}

    private Object readResolve() throws ObjectStreamException {
      return TOP;
    }
  }

}
