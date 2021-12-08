package org.sosy_lab.cpachecker.cpa.string;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectList;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StringState implements LatticeAbstractState<StringState> {

  private StringOptions options;

  // Stores all strings of the program, along with the aspects
  private ImmutableMap<JStringVariableIdentifier, AspectList> stringsAndAspects;

  public StringState(
      ImmutableMap<JStringVariableIdentifier, AspectList> pStringMap,
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

  // Update doesn't create a new state
  public StringState updateVariable(JStringVariableIdentifier pJid, AspectList vaa) {

    ImmutableMap.Builder<JStringVariableIdentifier, AspectList> builder =
        new ImmutableMap.Builder<>();

    for (Map.Entry<JStringVariableIdentifier, AspectList> entry : stringsAndAspects.entrySet()) {

      JStringVariableIdentifier jid = entry.getKey();

      if (!jid.equals(pJid)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }

    }

    stringsAndAspects = ImmutableMap.copyOf(builder.put(pJid, vaa).build());

    return this;
  }

  public StringState addVariable(JStringVariableIdentifier jid) {
    return addVariable(jid, null);
  }

  public StringState addVariable(JStringVariableIdentifier jid, AspectList vaa) {

    ImmutableMap.Builder<JStringVariableIdentifier, AspectList> builder =
        new ImmutableMap.Builder<>();

    builder.putAll(stringsAndAspects);
    builder.put(jid, vaa);

    return new StringState(builder.build(), options);
  }

  public Map<JStringVariableIdentifier, AspectList> getStringsAndAspects() {
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
    stringsAndAspects = joinMapsNoDuplicates(pOther.stringsAndAspects);
    return this;
  }

  private ImmutableMap<JStringVariableIdentifier, AspectList>
      joinMapsNoDuplicates(Map<JStringVariableIdentifier, AspectList> pStringMap) {

    ImmutableMap.Builder<JStringVariableIdentifier, AspectList> builder =
        new ImmutableMap.Builder<>();

    builder.putAll(stringsAndAspects);

    for (Map.Entry<JStringVariableIdentifier, AspectList> entry : stringsAndAspects.entrySet()) {

      JStringVariableIdentifier jid = entry.getKey();

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

    for (Map.Entry<JStringVariableIdentifier, AspectList> otherEntry : pOther.stringsAndAspects
        .entrySet()) {

      AspectList otherVaa = otherEntry.getValue();
      JStringVariableIdentifier jid = otherEntry.getKey();
      AspectList vaa = this.stringsAndAspects.get(jid);

      if (vaa == null || !vaa.isLessOrEqual(otherVaa)) {
        return false;
      }

    }

    return true;
  }

  public Optional<JStringVariableIdentifier> isVariableInMap(String pVar) {

    for (JStringVariableIdentifier jid : stringsAndAspects.keySet()) {

      if (jid.getIdentifier().equals(pVar)) {
        return Optional.of(jid);
      }

    }

    return Optional.empty();
  }

  public AspectList getVaa(JStringVariableIdentifier jid) {
    return checkNotNull(stringsAndAspects.get(jid));
  }

  public boolean contains(JStringVariableIdentifier jid) {
    return stringsAndAspects.containsKey(jid);
  }

  public boolean contains(AspectList vaa) {
    return stringsAndAspects.containsValue(vaa);
  }

  public StringState clearLocalVariables(String funcname) {

    ImmutableMap.Builder<JStringVariableIdentifier, AspectList> builder =
        new ImmutableMap.Builder<>();

    for (Map.Entry<JStringVariableIdentifier, AspectList> entry : stringsAndAspects.entrySet()) {

      JStringVariableIdentifier jid = entry.getKey();

      if (!jid.getMemLoc().isOnFunctionStack(funcname)) {
        builder.put(jid, stringsAndAspects.get(jid));
      }

    }

    return new StringState(builder.build(), options);
  }

  public StringState clearAllLocalVariables() {

    ImmutableMap.Builder<JStringVariableIdentifier, AspectList> builder =
        new ImmutableMap.Builder<>();

    for (Map.Entry<JStringVariableIdentifier, AspectList> entry : stringsAndAspects.entrySet()) {

      JStringVariableIdentifier jid = entry.getKey();

      if (!jid.isGlobal()) {
        builder.put(jid, stringsAndAspects.get(jid));
      }
    }
    return new StringState(builder.build(), options);
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder("String State: {");

    for (Map.Entry<JStringVariableIdentifier, AspectList> entry : stringsAndAspects.entrySet()) {

      JStringVariableIdentifier jid = entry.getKey();
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
