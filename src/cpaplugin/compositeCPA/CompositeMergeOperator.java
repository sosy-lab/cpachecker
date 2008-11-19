package cpaplugin.compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.cpas.location.LocationElement;

public class CompositeMergeOperator implements MergeOperator{
	

	private CompositeDomain compositeDomain;
	private List<MergeOperator> mergeOperators;

	public CompositeMergeOperator (CompositeDomain compositeDomain, List<MergeOperator> mergeOperators)
	{
		this.compositeDomain = compositeDomain;
		this.mergeOperators = mergeOperators;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return compositeDomain;
	}

	public AbstractElement merge (AbstractElement element1, AbstractElement element2)
	{
		// Merge Sep Code
		CompositeElement comp1 = (CompositeElement) element1;
		CompositeElement comp2 = (CompositeElement) element2;

		assert(comp1.getNumberofElements() == comp2.getNumberofElements());

		AbstractElement elementsArray1[] = new AbstractElement[comp1.getNumberofElements()];
		AbstractElement elementsArray2[] = new AbstractElement[comp2.getNumberofElements()];

//		// TODO Maybe we should not handle location element separetely
//		LocationElement locationElement1 = (LocationElement) comp1.getElements ().get (0);
//		LocationElement locationElement2 = (LocationElement) comp2.getElements ().get (0);

		for(int i=0; i<comp1.getNumberofElements(); i++){
			elementsArray1[i] = comp1.getElements().get(i);
			elementsArray2[i] = comp2.getElements().get(i);
		}

//		if (!locationElement1.equals (locationElement2))
//			return element2;

		List<AbstractElement> mergedElements = new ArrayList<AbstractElement> ();

		for(int i=0; i<comp1.getNumberofElements(); i++){
			// TODO check if the first element is not merged just return the entire second element
			AbstractElement absElem1 = elementsArray1[i];
			AbstractElement absElem2 = elementsArray2[i];
			AbstractElement merged = mergeOperators.get (i).merge (absElem1, absElem2);
			if(merged == absElem2){
				return comp2;
			}
			mergedElements.add (merged);
		}

		return new CompositeElement (mergedElements);
	}
}
