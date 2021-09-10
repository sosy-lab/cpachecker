package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.HelperMethods;
import org.sosy_lab.cpachecker.cpa.string.utils.JVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StringState implements LatticeAbstractState<StringState> {

  private StringOptions options;
  private ImmutableMap<JVariableIdentifier, ValueAndAspects> stringsAndAspects;

  public StringState(Map<JVariableIdentifier, String> pStringMap, StringOptions pOptions) {
    options = pOptions;
    Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
    for (JVariableIdentifier jid : pStringMap.keySet()) {
      if (HelperMethods.isString(jid.getType())) { // Doublecheck to be 100% sure
        String value = pStringMap.get(jid);
        ValueAndAspects vaa = new ValueAndAspects(value, storeAllAspects(value));
        builder.put(jid, vaa);
        // stringsAndAspects.put(jid, vaa);
      }
    }
    stringsAndAspects = ImmutableMap.copyOf(builder.build());
  }

  private StringState(
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

  public StringState updateVariable(JVariableIdentifier jid, ValueAndAspects updateValue) {
    return updateVariable(jid, updateValue.getValue());
  }

  public StringState updateVariable(JVariableIdentifier pJid, String updateValue) {
    // StringState state = copyOf(this);
    ValueAndAspects svaa = new ValueAndAspects(updateValue, storeAllAspects(updateValue));
    Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
    for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
      if (!jid.equals(pJid)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }

    // state.stringsAndAspects.remove(jid);
    builder.put(pJid, svaa);
    // state.stringsAndAspects.put(pJid, svaa);
    return new StringState(builder.build(), options);
  }

  public StringState addVariable(JVariableIdentifier jid) {
    return updateVariable(jid, "");
  }

  public StringState addVariable(JVariableIdentifier jid, String pValue) {
    return updateVariable(jid, pValue);
  }

  private List<Aspect> storeAllAspects(String pVal) {
    ArrayList<Aspect> aspects = new ArrayList<>(options.getDomains().size());
    for (AbstractStringDomain a : options.getDomains()) {
      aspects.add(a.toAdd(pVal));
    }
    return aspects;
  }

  public Map<JVariableIdentifier, ValueAndAspects> getStringsAndAspects() {
    return stringsAndAspects;
  }

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
    // HashMap<JVariableIdentifier, ValueAndAspects> result =
    // (HashMap<JVariableIdentifier, ValueAndAspects>) this.stringsAndAspects;
    Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
    builder.putAll(stringsAndAspects);
    for (JVariableIdentifier jid : pStringMap.keySet()) {
      if (!stringsAndAspects.containsKey(jid)) {
        builder.put(jid, pStringMap.get(jid));
      }
    }
    return builder.build();
  }

  @Override
  public boolean isLessOrEqual(StringState pOther) throws CPAException, InterruptedException {
    if (!(this.stringsAndAspects.size() != pOther.stringsAndAspects.size())) {
      return false;
    } else {
      for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
        if (!pOther.stringsAndAspects.containsKey(jid)) {
          return false;
        }
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
    Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
    for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
      if (!jid.getMemLoc().isOnFunctionStack(funcname)) {
        // state.stringsAndAspects.remove(jid);
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  public StringState clearAllLocalVariables() {
    // StringState state = copyOf(this);
    Builder<JVariableIdentifier, ValueAndAspects> builder = new Builder<>();
    for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
      if (!jid.isGlobal()) {
        // state.stringsAndAspects.remove(jid);
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("String State: {");
    for (JVariableIdentifier jid : stringsAndAspects.keySet()) {
      builder.append("[" + jid.toString() + stringsAndAspects.get(jid).toString() + "]");
    }
    builder.append("} ");
    return builder.toString();
  }
}
