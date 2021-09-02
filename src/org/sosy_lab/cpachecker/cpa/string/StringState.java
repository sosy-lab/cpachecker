package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.HashMap;
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
  private Map<JVariableIdentifier, ValueAndAspects> stringsAndAspects;

  public StringState(Map<JVariableIdentifier, String> pStringMap, StringOptions pOptions) {
    options = pOptions;
    stringsAndAspects = new HashMap<>();
    for (JVariableIdentifier jid : pStringMap.keySet()) {
      if (HelperMethods.isString(jid.getType())) { // Doublecheck to be 100% sure
        String value = pStringMap.get(jid);
        ValueAndAspects vaa = new ValueAndAspects(value, storeAllAspects(value));
        stringsAndAspects.put(jid, vaa);
      }
    }
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

  public StringState updateVariable(JVariableIdentifier jid, String updateValue) {
    StringState state = copyOf(this);
    ValueAndAspects svaa = new ValueAndAspects(updateValue, storeAllAspects(updateValue));
    if (stringsAndAspects.containsKey(jid)) {
      state.stringsAndAspects.remove(jid);
    }
    state.stringsAndAspects.put(jid, svaa);
    return state;
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

  private HashMap<JVariableIdentifier, ValueAndAspects>
      joinMapsNoDups(Map<JVariableIdentifier, ValueAndAspects> pStringMap) {
    HashMap<JVariableIdentifier, ValueAndAspects> result =
        (HashMap<JVariableIdentifier, ValueAndAspects>) this.stringsAndAspects;
    for (JVariableIdentifier jid : pStringMap.keySet()) {
      if (!result.containsKey(jid)) {
        result.put(jid, pStringMap.get(jid));
      }
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(StringState pOther) throws CPAException, InterruptedException {
    if (!Objects.equal(this, pOther)) {
      return false;
    }
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

  public StringState clearAVariable(JVariableIdentifier jid) {
    StringState state = copyOf(this);
    if (state.stringsAndAspects.containsKey(jid)) {
      state.stringsAndAspects.remove(jid);
    }
    return state;
  }

  public StringState clearAFunction(String funcname) {
    StringState state = copyOf(this);
    for (JVariableIdentifier jid : state.stringsAndAspects.keySet()) {
      if (jid.getMemLoc().isOnFunctionStack(funcname)) {
        state.stringsAndAspects.remove(jid);
      }
    }
    return state;
  }
  public StringState clearAllLocalVariables() {
    StringState state = copyOf(this);
    for (JVariableIdentifier jid : state.stringsAndAspects.keySet()) {
      if (!jid.isGlobal()) {
        state.stringsAndAspects.remove(jid);
      }
    }
    return state;
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
