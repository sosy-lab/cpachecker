package symbpredabstraction;

import java.util.Deque;
import java.util.Vector;

import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;

public class Path {

  Vector<Integer> elemIds;

  public Path(Deque<SymbPredAbsAbstractElement> cex) {
    elemIds = new Vector<Integer>();
    elemIds.ensureCapacity(cex.size());
    for (SymbPredAbsAbstractElement e : cex) {
      elemIds.add(e.getAbstractionLocation().getNodeNumber());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Path) {
      return elemIds.equals(((Path)o).elemIds);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return elemIds.hashCode();
  }

}
