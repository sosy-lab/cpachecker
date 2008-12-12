package cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

public class ExplicitAnalysisDomain implements AbstractDomain {

	private static class ExplicitAnalysisBottomElement extends ExplicitAnalysisElement implements BottomElement
	{
		public ExplicitAnalysisBottomElement ()
		{
			
		}
	}

	private static class ExplicitAnalysisTopElement implements TopElement
	{

	}

	private static class ExplicitAnalysisPartialOrder implements PartialOrder
	{
		// returns true if element1 < element2 on lattice
		public boolean satisfiesPartialOrder(AbstractElement element1, AbstractElement element2)
		{
			ExplicitAnalysisElement explicitAnalysisElement1 = (ExplicitAnalysisElement) element1;
			ExplicitAnalysisElement explicitAnalysisElement2 = (ExplicitAnalysisElement) element2;

			Map<String, Integer> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
			Map<String, Integer> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

			if(constantsMap1.size() > constantsMap2.size()){
				return false;
			}

			for(String key:constantsMap2.keySet()){
				if(!constantsMap1.containsKey(key) | 
						constantsMap1.get(key) != constantsMap2.get(key)){
					return false;
				}
			}

			return true;
		}
	}

	private static class ExplicitAnalysisJoinOperator implements JoinOperator
	{
		public AbstractElement join(AbstractElement element1, AbstractElement element2)
		{
			ExplicitAnalysisElement explicitAnalysisElement1 = (ExplicitAnalysisElement) element1;
			ExplicitAnalysisElement explicitAnalysisElement2 = (ExplicitAnalysisElement) element2;

			Map<String, Integer> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
			Map<String, Integer> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

			Map<String, Integer> newMap = new HashMap<String, Integer>();

			for(String key:constantsMap1.keySet()){
				newMap.put(key, constantsMap1.get(key));
			}

			for(String key:constantsMap2.keySet()){
				if(newMap.containsKey(key)){
					if(newMap.get(key) != constantsMap2.get(key)){
						newMap.remove(key);
					}
				}
				else {
					newMap.put(key, constantsMap2.get(key));
				}
			}
			return new ExplicitAnalysisElement(newMap);
		}
	}

	private final static BottomElement bottomElement = new ExplicitAnalysisBottomElement ();
	private final static TopElement topElement = new ExplicitAnalysisTopElement ();
	private final static PartialOrder partialOrder = new ExplicitAnalysisPartialOrder ();
	private final static JoinOperator joinOperator = new ExplicitAnalysisJoinOperator ();

	public ExplicitAnalysisDomain ()
	{

	}

	public BottomElement getBottomElement ()
	{
		return bottomElement;
	}

	public boolean isBottomElement(AbstractElement element) {
		if( ((ExplicitAnalysisElement)element) instanceof BottomElement){
			return true;
		}
		else {
			return false;
		}
	}

	public TopElement getTopElement ()
	{
		return topElement;
	}

	public JoinOperator getJoinOperator ()
	{
		return joinOperator;
	}

	public PartialOrder getPartialOrder ()
	{
		return partialOrder;
	}
}
