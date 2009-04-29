package predicateabstraction;

import java.util.Deque;
import java.util.Vector;

class Path {
  Vector<Integer> elemIds;

  public Path(Deque<ExplicitAbstractElement> cex) {
    elemIds = new Vector<Integer>();
    elemIds.ensureCapacity(cex.size());
    for (ExplicitAbstractElement e : cex) {
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
