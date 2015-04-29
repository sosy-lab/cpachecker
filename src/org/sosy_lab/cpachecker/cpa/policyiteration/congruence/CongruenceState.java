package org.sosy_lab.cpachecker.cpa.policyiteration.congruence;

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.policyiteration.Template;

import com.google.common.collect.ImmutableMap;

public class CongruenceState {

  private final ImmutableMap<Template, Congruence> data;

  public CongruenceState(Map<Template, Congruence> pData) {
    data = ImmutableMap.copyOf(pData);
  }

  public Map<Template, Congruence> getAbstraction() {
    return data;
  }

  public static CongruenceState empty() {
    return new CongruenceState(ImmutableMap.<Template, Congruence>of());
  }
}
