/**
 * 
 */
package compositeCPA;

import java.util.Iterator;
import java.util.List;

import cpa.common.interfaces.Precision;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecision implements Precision {
  private final List<Precision> precisions;

  public CompositePrecision (List<Precision> precisions)
  {
    this.precisions = precisions;
  }

  public List<Precision> getPrecisions ()
  {
    return precisions;
  }

  @Override
  public boolean equals (Object other)
  {
    if (other == this)
      return true;

    if (!(other instanceof CompositePrecision))
      return false;

    CompositePrecision otherPrecision = (CompositePrecision) other;

    if (otherPrecision.precisions.size() != this.precisions.size ())
      return false;
    
    Iterator<Precision> iter = precisions.iterator();
    Iterator<Precision> otherIter = otherPrecision.precisions.iterator();
    
    while (iter.hasNext()) {
      if (!iter.next().equals(otherIter.next())) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;

    for (Precision p : precisions) {
      if (p != null) hashCode += p.hashCode();
    }

    return hashCode;
  }

  public Precision get(int idx) {
    return precisions.get(idx);
  }
}
