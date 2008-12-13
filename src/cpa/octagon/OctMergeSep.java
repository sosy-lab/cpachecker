package cpa.octagon;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import exceptions.CPAException;

public class OctMergeSep implements MergeOperator {

	private final OctDomain octDomain;

	public OctMergeSep (OctDomain octDomain)
    {
        this.octDomain = octDomain;
    }

	public AbstractDomain getAbstractDomain() {
		return octDomain;
	}

	public AbstractElement merge(AbstractElement element1,
			AbstractElement element2) {
		return element2;
	}

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
