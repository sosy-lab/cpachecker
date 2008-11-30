package cpa.octagon;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;

public class OctMergeSep implements MergeOperator {

	private OctDomain octDomain;
	
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

}
