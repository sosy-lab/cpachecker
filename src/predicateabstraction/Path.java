package predicateabstraction;

import java.util.Deque;
import java.util.Vector;

import cpa.predicateabstraction.PredicateAbstractionAbstractElement;

class Path {
  Vector<Integer> elemIds;

  public Path(Deque<PredicateAbstractionAbstractElement> cex) {
    elemIds = new Vector<Integer>();
    elemIds.ensureCapacity(cex.size());
    for (PredicateAbstractionAbstractElement e : cex) {
      elemIds.add(e.getLocation().getNodeNumber());
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
