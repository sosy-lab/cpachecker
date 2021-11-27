package org.sosy_lab.cpachecker.cpa.string;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.string.utils.JVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StringState implements LatticeAbstractState<StringState> {

  private StringOptions options;
  private ImmutableMap<JVariableIdentifier, ValueAndAspects> stringsAndAspects;
  // private ImmutableList<JVariableIdentifier> stringVars;
  // public StringState(Map<JVariableIdentifier, String> pStringMap, StringOptions pOptions) {
  // options = pOptions;
  // Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
  // for (JVariableIdentifier jid : pStringMap.keySet()) {
  // if (HelperMethods.isString(jid.getType())) { // Doublecheck to be 100% sure
  // String value = pStringMap.get(jid);
  // ValueAndAspects vaa = new ValueAndAspects(value, storeAllAspects(value));
  // builder.put(jid, vaa);
  // }
  // }
  // stringsAndAspects = ImmutableMap.copyOf(builder.build());
  // }

  // public StringState(List<JVariableIdentifier> pStringVars, StringOptions pOptions) {
  // options = pOptions;
  // stringVars = ImmutableList.copyOf(pStringVars);
  // }

  public StringState(
      ImmutableMap<JVariableIdentifier, ValueAndAspects> pStringMap,
      StringOptions pOptions) {
    options = pOptions;
    stringsAndAspects = ImmutableMap.copyOf(pStringMap);
  }

  private StringState(StringState state) {
    stringsAndAspects = state.stringsAndAspects;
    options = state.options;
  }

  public static StringState copyOf(StringState state) {
    return new StringState(state);
  }

  // public StringState updateVariable(JVariableIdentifier jid, ValueAndAspects updateValue) {
  // return updateVariable(jid, updateValue.getValue());
  // }

  // Update doesn't create a new state
  public StringState updateVariable(JVariableIdentifier pJid, ValueAndAspects vaa) {
    // ValueAndAspects svaa = new ValueAndAspects(updateValue, storeAllAspects(updateValue));
    com.google.common.collect.ImmutableMap.Builder<JVariableIdentifier, ValueAndAspects> builder =
        new com.google.common.collect.ImmutableMap.Builder<>();
    for (Map.Entry<JVariableIdentifier, ValueAndAspects> entry : stringsAndAspects.entrySet()) {
      JVariableIdentifier jid = entry.getKey();
      if (!jid.equals(pJid)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    stringsAndAspects = ImmutableMap.copyOf(builder.put(pJid, vaa).build());
    // return new StringState(builder.build(), options);
    return this;
  }

  public StringState addVariable(JVariableIdentifier jid) {
    return addVariable(jid, null);
  }

  public StringState addVariable(JVariableIdentifier jid, ValueAndAspects vaa) {
    // return updateVariable(jid, vaa);
    com.google.common.collect.ImmutableMap.Builder<JVariableIdentifier, ValueAndAspects> builder =
        new com.google.common.collect.ImmutableMap.Builder<>();
    builder.putAll(stringsAndAspects);
    builder.put(jid, vaa);
    return new StringState(builder.build(), options);
  }

  // private List<Aspect> storeAllAspects(String pVal) {
  // ArrayList<Aspect> aspects = new ArrayList<>(options.getDomains().size());
  // for (AbstractStringDomain a : options.getDomains()) {
  // aspects.add(a.toAdd(pVal));
  // }
  // return aspects;
  // }

  public Map<JVariableIdentifier, ValueAndAspects> getStringsAndAspects() {
    return stringsAndAspects;
  }

  /**
   * (non-Javadoc)
   *
   * @see org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState#join(org.sosy_lab.cpachecker.core.
   *      defaults.LatticeAbstractState)
   */
  @Override
  public StringState join(StringState pOther) throws CPAException, InterruptedException {
    if (isLessOrEqual(pOther)) {
      return pOther;
    }
    if (pOther.isLessOrEqual(this)) {
      return this;
    }
    stringsAndAspects = joinMapsNoDups(pOther.stringsAndAspects);
    return this;
  }

  private ImmutableMap<JVariableIdentifier, ValueAndAspects>
      joinMapsNoDups(Map<JVariableIdentifier, ValueAndAspects> pStringMap) {
    com.google.common.collect.ImmutableMap.Builder<JVariableIdentifier, ValueAndAspects> builder =
        new com.google.common.collect.ImmutableMap.Builder<>();
    builder.putAll(stringsAndAspects);
    for (Map.Entry<JVariableIdentifier, ValueAndAspects> entry : stringsAndAspects.entrySet()) {
      JVariableIdentifier jid = entry.getKey();
      if (!stringsAndAspects.containsKey(jid)) {
        builder.put(jid, pStringMap.get(jid));
      }
    }
    return builder.build();
  }

  /**
   * (non-Javadoc)
   *
   * @see org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState#isLessOrEqual(org.sosy_lab.
   *      cpachecker.core.defaults.LatticeAbstractState)
   */
  @Override
  public boolean isLessOrEqual(StringState pOther) throws CPAException, InterruptedException {
    if (this.stringsAndAspects.size() < pOther.stringsAndAspects.size()) {
      return false;
    }
    for (java.util.Map.Entry<JVariableIdentifier, ValueAndAspects> otherEntry : pOther.stringsAndAspects
        .entrySet()) {
      ValueAndAspects otherVaa = otherEntry.getValue();
      JVariableIdentifier jid = otherEntry.getKey();
      ValueAndAspects vaa = this.stringsAndAspects.get(jid);
      if (vaa == null || !vaa.isLessOrEqual(otherVaa)) {
        return false;
      }
    }
    return true;
  }

  public Optional<JVariableIdentifier> isVariableInMap(String pVar) {
    for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
      if (jid.getIdentifier().equals(pVar)) {
        return Optional.of(jid);
      }
    }
    return Optional.empty();
  }

  public ValueAndAspects getVaa(JVariableIdentifier jid) {
    return checkNotNull(stringsAndAspects.get(jid));
  }

  public boolean contains(JVariableIdentifier jid) {
    return stringsAndAspects.containsKey(jid);
  }

  public boolean contains(ValueAndAspects vaa) {
    return stringsAndAspects.containsValue(vaa);
  }
  // Not needed atm
  // public StringState clearAVariable(JVariableIdentifier jid) {
  //// StringState state = copyOf(this);
  // Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
  // if (stringsAndAspects.containsKey(jid)) {
  //
  //// state.stringsAndAspects.remove(jid);
  // }
  // return state;
  // }

  public StringState clearAFunction(String funcname) {
    com.google.common.collect.ImmutableMap.Builder<JVariableIdentifier, ValueAndAspects> builder =
        new com.google.common.collect.ImmutableMap.Builder<>();
    for (Map.Entry<JVariableIdentifier, ValueAndAspects> entry : stringsAndAspects.entrySet()) {
      JVariableIdentifier jid = entry.getKey();
      if (!jid.getMemLoc().isOnFunctionStack(funcname)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  public StringState clearAllLocalVariables() {
    com.google.common.collect.ImmutableMap.Builder<JVariableIdentifier, ValueAndAspects> builder =
        new com.google.common.collect.ImmutableMap.Builder<>();
    for (Map.Entry<JVariableIdentifier, ValueAndAspects> entry : stringsAndAspects.entrySet()) {
      JVariableIdentifier jid = entry.getKey();
      if (!jid.isGlobal()) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("String State: {");
    for (Map.Entry<JVariableIdentifier, ValueAndAspects> entry : stringsAndAspects.entrySet()) {
      JVariableIdentifier jid = entry.getKey();
      builder.append("[" + jid + stringsAndAspects.get(jid) + "]");
    }
    builder.append("} ");
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StringState) {
      // imprecise equals, doesn't care about contents of strings
      return this.stringsAndAspects.size() == ((StringState) obj).stringsAndAspects.size();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
