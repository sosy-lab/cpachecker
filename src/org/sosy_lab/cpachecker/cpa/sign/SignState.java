// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sign;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SignState
    implements Serializable, LatticeAbstractState<SignState>, AbstractQueryableState, Graphable {

  @Serial private static final long serialVersionUID = -2507059869178203119L;

  private static final boolean DEBUG = false;

  private static final Splitter propertySplitter = Splitter.on("<=").trimResults();

  private PersistentMap<String, Sign> signMap;

  public static final SignState TOP = new SignState();
  private static final SerialProxySign proxy = new SerialProxySign();

  private SignState(PersistentMap<String, Sign> pSignMap) {
    signMap = pSignMap;
  }

  private SignState() {
    signMap = PathCopyingPersistentTreeMap.of();
  }

  @Override
  public SignState join(SignState pToJoin) {
    if (pToJoin.equals(this)) {
      return pToJoin;
    }
    if (equals(TOP) || pToJoin.equals(TOP)) {
      return TOP;
    }

    // assure termination of loops do not merge if  pToJoin covers this but return pToJoin
    if (isLessOrEqual(pToJoin)) {
      return pToJoin;
    }

    SignState result = SignState.TOP;
    PersistentMap<String, Sign> newMap = PathCopyingPersistentTreeMap.of();
    Sign combined;
    for (String varIdent : pToJoin.signMap.keySet()) {
      // only add those variables that are contained in both states (otherwise one has value ALL
      // (not saved))
      if (signMap.containsKey(varIdent)) {
        combined = getSignForVariable(varIdent).combineWith(pToJoin.getSignForVariable(varIdent));
        if (!combined.isAll()) {
          newMap = newMap.putAndCopy(varIdent, combined);
        }
      }
    }

    return newMap.size() > 0 ? new SignState(newMap) : result;
  }

  @Override
  public boolean isLessOrEqual(SignState pSuperset) {
    if (pSuperset.equals(this) || pSuperset.equals(TOP)) {
      return true;
    }
    if (signMap.size() < pSuperset.signMap.size()) {
      return false;
    }
    // is subset if for every variable all sign assumptions are considered in pSuperset
    // check that all variables in superset with SIGN != ALL have no bigger assumptions in subset
    for (String varIdent : pSuperset.signMap.keySet()) {
      if (!getSignForVariable(varIdent).isSubsetOf(pSuperset.getSignForVariable(varIdent))) {
        return false;
      }
    }
    return true;
  }

  public SignState enterFunction(ImmutableMap<String, Sign> pArguments) {
    PersistentMap<String, Sign> newMap = signMap;

    for (Map.Entry<String, Sign> entry : pArguments.entrySet()) {
      String var = entry.getKey();
      if (!entry.getValue().equals(Sign.ALL)) {
        newMap = newMap.putAndCopy(var, entry.getValue());
      }
    }

    return signMap == newMap ? this : new SignState(newMap);
  }

  public SignState leaveFunction(String pFunctionName) {
    PersistentMap<String, Sign> newMap = signMap;

    for (String var : signMap.keySet()) {
      if (var.startsWith(pFunctionName + "::")) {
        newMap = newMap.removeAndCopy(var);
      }
    }

    return newMap == signMap ? this : new SignState(newMap);
  }

  public SignState assignSignToVariable(String pVarIdent, Sign sign) {
    if (sign.isAll()) {
      return signMap.containsKey(pVarIdent)
          ? new SignState(signMap.removeAndCopy(pVarIdent))
          : this;
    }
    return signMap.containsKey(pVarIdent) && getSignForVariable(pVarIdent).equals(sign)
        ? this
        : new SignState(signMap.putAndCopy(pVarIdent, sign));
  }

  public SignState removeSignAssumptionOfVariable(String pVarIdent) {
    return assignSignToVariable(pVarIdent, Sign.ALL);
  }

  public Sign getSignForVariable(String pVarIdent) {
    return signMap.containsKey(pVarIdent) ? signMap.get(pVarIdent) : Sign.ALL;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    String delim = ", ";
    builder.append("[");
    String loopDelim = "";
    for (String key : signMap.keySet()) {
      if (!DEBUG
          && (key.matches("\\w*::__CPAchecker_TMP_\\w*")
              || key.endsWith(SignTransferRelation.FUNC_RET_VAR))) {
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
    return pObj instanceof SignState other && other.signMap.equals(signMap);
  }

  @Override
  public int hashCode() {
    return signMap.hashCode();
  }

  @Serial
  private Object writeReplace() {
    if (equals(TOP)) {
      return proxy;
    } else {
      return this;
    }
  }

  @Serial
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

  private static class SerialProxySign implements Serializable {

    @Serial private static final long serialVersionUID = 2843708585446089623L;

    SerialProxySign() {}

    @Serial
    private Object readResolve() {
      return TOP;
    }
  }

  @Override
  public String getCPAName() {
    return "SignAnalysis";
  }

  private static boolean isSIGN(String s) {
    try {
      Sign.valueOf(s);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return true;
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    List<String> parts = propertySplitter.splitToList(pProperty);

    if (parts.size() == 2) {

      if (isSIGN(parts.get(0))) {
        // pProperty = value <= varName
        Sign value = Sign.valueOf(parts.get(0));
        Sign varName = getSignForVariable(parts.get(1));
        return varName.covers(value);

      } else if (isSIGN(parts.get(1))) {
        // pProperty = varName <= value
        Sign varName = getSignForVariable(parts.get(0));
        Sign value = Sign.valueOf(parts.get(1));
        return value.covers(varName);

      } else {
        // pProperty = varName1 <= varName2
        Sign varName1 = getSignForVariable(parts.get(0));
        Sign varName2 = getSignForVariable(parts.get(1));
        return varName2.covers(varName1);
      }
    }

    return false;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, signMap);
    sb.append("}");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public Map<String, Sign> getSignMapView() {
    return Collections.unmodifiableMap(signMap);
  }
}
