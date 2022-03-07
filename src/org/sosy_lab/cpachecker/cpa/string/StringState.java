// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectSet;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StringState implements LatticeAbstractState<StringState> {

  private final StringOptions options;

  // Stores all strings of the program, along with the aspects
  private ImmutableMap<JStringVariableIdentifier, AspectSet> stringsAndAspects;

  public StringState(
      ImmutableMap<JStringVariableIdentifier, AspectSet> pStringMap, StringOptions pOptions) {
    options = pOptions;
    stringsAndAspects = pStringMap;
  }

  private StringState(StringState state) {
    stringsAndAspects = state.stringsAndAspects;
    options = state.options;
  }

  public static StringState copyOf(StringState state) {
    return new StringState(state);
  }

  public StringState updateVariable(JStringVariableIdentifier pJid, AspectSet vaa) {
    ImmutableMap.Builder<JStringVariableIdentifier, AspectSet> builder =
        new ImmutableMap.Builder<>();
    StringState state = copyOf(this);
    for (Map.Entry<JStringVariableIdentifier, AspectSet> entry : stringsAndAspects.entrySet()) {
      JStringVariableIdentifier jid = entry.getKey();
      if (!jid.equals(pJid)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    state.stringsAndAspects = builder.put(pJid, vaa).build();
    return state;
  }

  public StringState addVariable(JStringVariableIdentifier jid) {
    return addVariable(jid, null);
  }

  public StringState addVariable(JStringVariableIdentifier jid, AspectSet vaa) {
    ImmutableMap.Builder<JStringVariableIdentifier, AspectSet> builder =
        new ImmutableMap.Builder<>();
    builder.putAll(stringsAndAspects);
    builder.put(jid, vaa);
    return new StringState(builder.build(), options);
  }

  public Map<JStringVariableIdentifier, AspectSet> getStringsAndAspects() {
    return stringsAndAspects;
  }

  /**
   * (non-Javadoc)
   *
   * @see
   *     org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState#join(org.sosy_lab.cpachecker.core.
   *     defaults.LatticeAbstractState)
   */
  @Override
  public StringState join(StringState pOther) throws CPAException, InterruptedException {
    if (isLessOrEqual(pOther)) {
      return pOther;
    }
    if (pOther.isLessOrEqual(this)) {
      return this;
    }
    StringState state = copyOf(this);
    state.stringsAndAspects = joinMapsNoDuplicates(pOther.stringsAndAspects);
    return state;
  }

  private ImmutableMap<JStringVariableIdentifier, AspectSet> joinMapsNoDuplicates(
      Map<JStringVariableIdentifier, AspectSet> pStringMap) {
    ImmutableMap.Builder<JStringVariableIdentifier, AspectSet> builder =
        new ImmutableMap.Builder<>();
    for (Map.Entry<JStringVariableIdentifier, AspectSet> otherEntry : pStringMap.entrySet()) {
      JStringVariableIdentifier jid = otherEntry.getKey();
      AspectSet aspectSet = otherEntry.getValue();
      if (Objects.equals(aspectSet, stringsAndAspects.get(jid))) {
        builder.put(jid, pStringMap.get(jid));
      }
    }
    return builder.build();
  }

  /**
   * (non-Javadoc)
   *
   * @see org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState#isLessOrEqual(org.sosy_lab.
   *     cpachecker.core.defaults.LatticeAbstractState)
   */
  @Override
  public boolean isLessOrEqual(StringState pOther) throws CPAException, InterruptedException {
    if (stringsAndAspects.size() < pOther.stringsAndAspects.size()) {
      return false;
    }

    for (Map.Entry<JStringVariableIdentifier, AspectSet> otherEntry :
        pOther.stringsAndAspects.entrySet()) {
      AspectSet otherVaa = otherEntry.getValue();
      JStringVariableIdentifier jid = otherEntry.getKey();
      AspectSet vaa = this.stringsAndAspects.get(jid);
      if (vaa == null || !vaa.isLessOrEqual(otherVaa)) {
        return false;
      }
    }
    return true;
  }

  public Optional<JStringVariableIdentifier> isVariableInMap(String pVar) {
    for (JStringVariableIdentifier jid : stringsAndAspects.keySet()) {
      if (jid.getMemLoc().getIdentifier().equals(pVar)) {
        return Optional.of(jid);
      }
    }
    return Optional.empty();
  }

  public AspectSet getAspectList(JStringVariableIdentifier jid) {
    for (Map.Entry<JStringVariableIdentifier, AspectSet> entry : stringsAndAspects.entrySet()) {
      if (entry.getKey().equals(jid)) {
        return entry.getValue();
      }
    }
    // Variable not in program
    return null;
  }

  public StringOptions getOptions() {
    return options;
  }

  public boolean contains(JStringVariableIdentifier jid) {
    return stringsAndAspects.containsKey(jid);
  }

  public boolean contains(AspectSet vaa) {
    return stringsAndAspects.containsValue(vaa);
  }

  public StringState clearLocalVariables(String funcname) {
    ImmutableMap.Builder<JStringVariableIdentifier, AspectSet> builder =
        new ImmutableMap.Builder<>();
    for (Map.Entry<JStringVariableIdentifier, AspectSet> entry : stringsAndAspects.entrySet()) {
      JStringVariableIdentifier jid = entry.getKey();
      if (!jid.getMemLoc().isOnFunctionStack(funcname)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("String State: {");
    for (Map.Entry<JStringVariableIdentifier, AspectSet> entry : stringsAndAspects.entrySet()) {
      JStringVariableIdentifier jid = entry.getKey();
      builder.append("[" + jid + stringsAndAspects.get(jid) + "]");
    }
    builder.append("} ");

    return builder.toString();
  }
}
