package compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.MergeOperator;
import cpa.location.LocationElement;
import exceptions.CPAException;

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


	// TODO fix this part
	public AbstractElement merge (AbstractElement element1, AbstractElement element2) throws CPAException
	{
		// TODO check
		if(element1 instanceof BottomElement){
			return element2;
		}

		// Merge Sep Code
		CompositeElement comp1 = (CompositeElement) element1;
		CompositeElement comp2 = (CompositeElement) element2;

		assert(comp1.getNumberofElements() == comp2.getNumberofElements());

		AbstractElement elementsArray1[] = new AbstractElement[comp1.getNumberofElements()];
		AbstractElement elementsArray2[] = new AbstractElement[comp2.getNumberofElements()];

		// TODO we assume that the first element has the location information
		assert(comp1.getElements().get(0) instanceof LocationElement);
		assert(comp2.getElements().get(0) instanceof LocationElement);

		LocationElement locationElement1 = (LocationElement) comp1.getElements().get(0);
		LocationElement locationElement2 = (LocationElement) comp2.getElements().get(0);

		if (!locationElement1.equals (locationElement2))
			return element2;

		// check for call stack
		CallStack cs1 = comp1.getCallStack();
		CallStack cs2 = comp2.getCallStack();

		// do not merge if call stacks are not equal
		if(!cs1.equals(cs2)){
			return element2;
		}

		for(int i=0; i<comp1.getNumberofElements(); i++){
			elementsArray1[i] = comp1.getElements().get(i);
			elementsArray2[i] = comp2.getElements().get(i);
		}

		List<AbstractElement> mergedElements = new ArrayList<AbstractElement> ();

		for(int i=0; i<comp1.getNumberofElements(); i++){
			AbstractElement absElem1 = elementsArray1[i];
			AbstractElement absElem2 = elementsArray2[i];
			AbstractElement merged = mergeOperators.get(i).merge(absElem1, absElem2);
			// if the element is not location and it is not merged we do not need to merge
			if(i != 0 && merged == absElem2){
				return comp2;
			}
			mergedElements.add (merged);
		}

		return new CompositeElement (mergedElements, cs1);
	}
}
