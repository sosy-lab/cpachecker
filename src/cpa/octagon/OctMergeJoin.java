package cpa.octagon;

import octagon.LibraryAccess;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import exceptions.CPAException;

public class OctMergeJoin implements MergeOperator{

	private final OctDomain octDomain;

	public OctMergeJoin (OctDomain octDomain)
	{
		this.octDomain = octDomain;
	}

	public AbstractDomain getAbstractDomain() {
		return octDomain;
	}

	public AbstractElement merge(AbstractElement element1, AbstractElement element2) {
		OctElement octEl1 = (OctElement) element1;
		OctElement octEl2 = (OctElement) element2;

		int dim1 = LibraryAccess.getDim(octEl1);
		int dim2 = LibraryAccess.getDim(octEl2);

		// TODO recursive join should be handled gracefully here
		//octEl2.addVariablesFrom(octEl1);
		//System.out.println(octEl1.getNumberOfVars() + "{ }" + octEl2.getNumberOfVars());

		assert(dim1 == dim2);

		if(OctConstants.useWidening){
			OctConstants.useWidening = false;
			return LibraryAccess.widening(octEl2, octEl1);
		}
		else{
			System.out.println("Using UNION");
			return LibraryAccess.union(octEl2, octEl1);
		}
	}

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
