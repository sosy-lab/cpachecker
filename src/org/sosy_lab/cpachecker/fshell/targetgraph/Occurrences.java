package org.sosy_lab.cpachecker.fshell.targetgraph;

import java.util.HashMap;
import java.util.Map;

public class Occurrences {

  Map<Edge, Integer> mOccurrences = new HashMap<Edge, Integer>();

  public Occurrences() {

  }

  public void decrement(Edge pEdge) {
    if (mOccurrences.containsKey(pEdge)) {
      int lCurrentValue = mOccurrences.get(pEdge);

      lCurrentValue--;

      if (lCurrentValue < 0) {
        lCurrentValue = 0;
      }

      mOccurrences.put(pEdge, lCurrentValue);
    }
    else {
      throw new RuntimeException();
    }
  }

  public int increment(Edge pEdge) {
    int lCurrentValue = 0;

    if (mOccurrences.containsKey(pEdge)) {
      lCurrentValue = mOccurrences.get(pEdge);
    }

    lCurrentValue++;

    mOccurrences.put(pEdge, lCurrentValue);

    return lCurrentValue;
  }

}
