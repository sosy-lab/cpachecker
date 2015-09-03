package org.sosy_lab.cpachecker.cpa.policyiteration.congruence;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cpa.policyiteration.Template;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class CongruenceState implements Iterable<Entry<Template, Congruence>>{

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

  public Optional<Congruence> get(Template template) {
    Congruence c = data.get(template);
    if (c == null) {
      return Optional.absent();
    }
    return Optional.of(c);
  }

  @Override
  public Iterator<Entry<Template, Congruence>> iterator() {
    return data.entrySet().iterator();
  }
}
