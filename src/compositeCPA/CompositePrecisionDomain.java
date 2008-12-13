/**
 * 
 */
package compositeCPA;

import java.util.Iterator;
import java.util.List;

import cpa.common.interfaces.PrecisionDomain;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecisionDomain implements PrecisionDomain {
  private final List<PrecisionDomain> precisionDomains;

  public CompositePrecisionDomain (List<PrecisionDomain> precisionDomains)
  {
    this.precisionDomains = precisionDomains;
  }

  public List<PrecisionDomain> getPrecisionDomains ()
  {
    return precisionDomains;
  }

  @Override
  public boolean equals (Object other)
  {
    if (other == this)
      return true;

    if (!(other instanceof CompositePrecisionDomain))
      return false;

    CompositePrecisionDomain otherPrecisionDomain = (CompositePrecisionDomain) other;

    if (otherPrecisionDomain.precisionDomains.size() != this.precisionDomains.size ())
      return false;
    
    Iterator<PrecisionDomain> iter = precisionDomains.iterator();
    Iterator<PrecisionDomain> otherIter = otherPrecisionDomain.precisionDomains.iterator();
    
    while (iter.hasNext()) {
      if (!iter.next().equals(otherIter.next())) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;

    for (PrecisionDomain p : precisionDomains) {
      if (p != null) hashCode += p.hashCode();
    }

    return hashCode;
  }

  public PrecisionDomain get(int idx) {
    return precisionDomains.get(idx);
  }
}
