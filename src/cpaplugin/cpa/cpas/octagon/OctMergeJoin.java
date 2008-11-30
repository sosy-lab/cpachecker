package cpaplugin.cpa.cpas.octagon;

import octagon.LibraryAccess;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

public class OctMergeJoin implements MergeOperator{

	private OctDomain octDomain;

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

}
