/**
 * 
 */
package cpaplugin.cpa.cpas.dominator.parametric;

import java.util.Iterator;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.exceptions.CPAException;

import java.util.Set;
import java.util.HashSet;

/**
 * @author holzera
 *
 */
public class DominatorDomain implements AbstractDomain, PartialOrder, JoinOperator {

	private ConfigurableProgramAnalysis cpa;
	
	public DominatorDomain(ConfigurableProgramAnalysis cpa) {
		this.cpa = cpa;
	}
	
	private static class DominatorBottomElement implements BottomElement
    {
		private DominatorBottomElement() {
			
		}
		
        @Override
        public String toString() {
        	return "\\top";
        }
        
        @Override
        public boolean equals(Object o) {
        	return (o instanceof DominatorBottomElement);
        }
        
        @Override
        public int hashCode() {
        	return Integer.MAX_VALUE;
        }
    }
	
	private static class DominatorTopElement implements TopElement
    {
		private DominatorTopElement() {
			
		}
		
        @Override
        public String toString() {
        	return "\\bot";
        }
        
        @Override
        public boolean equals(Object o) {
        	return (o instanceof DominatorTopElement);
        }
        
        @Override
        public int hashCode() {
        	return Integer.MIN_VALUE;
        }
    }
    
    private final static DominatorBottomElement bottomElement = new DominatorBottomElement();
    private final static DominatorTopElement topElement = new DominatorTopElement();
    
    public boolean satisfiesPartialOrder(AbstractElement element1, AbstractElement element2) throws CPAException
    {
        if (element1.equals(element2))
            return true;
        
        if (element1.equals(bottomElement) || element2.equals(topElement))
            return true;
        
        if (element1 instanceof DominatorElement && element2 instanceof DominatorElement) {
        	DominatorElement dominatorElement1 = (DominatorElement)element1;
        	DominatorElement dominatorElement2 = (DominatorElement)element2;
        	
        	if (this.cpa.getAbstractDomain().getPartialOrder().satisfiesPartialOrder(dominatorElement1.getDominatedElement(), dominatorElement2.getDominatedElement())) {
        		Iterator<AbstractElementWithLocation> dominatorIterator = dominatorElement2.getIterator();
        		
        		while (dominatorIterator.hasNext()) {
        			AbstractElementWithLocation dominator = dominatorIterator.next();
        			
        			if (!dominatorElement1.isDominatedBy(dominator)) {
        				return false;
        			}
        		}
        		
        		return true;
        	}
        }
        
        return false;
    }
    
    public AbstractElement join(AbstractElement element1, AbstractElement element2) {
    	if (element1.equals(bottomElement)) {
			return element2;
		}

		if (element1.equals(topElement)) {
			return element1;
		}

		if (element2.equals(bottomElement)) {
			return element1;
		}

		if (element2.equals(topElement)) {
			return element2;
		}

		// neither element1 nor element2 are top or bottom
		if (!(element1 instanceof DominatorElement)) {
			throw new IllegalArgumentException(
					"element1 is not a DominatorElement!");
		}

		if (!(element2 instanceof DominatorElement)) {
			throw new IllegalArgumentException(
					"element2 is not a DominatorElement!");
		}

		DominatorElement dominatorElement1 = (DominatorElement) element1;
		DominatorElement dominatorElement2 = (DominatorElement) element2;

		if (!dominatorElement1.getDominatedElement().equals(dominatorElement2.getDominatedElement())) {
			return topElement;
		}
		
		Set<AbstractElementWithLocation> intersectingDominators = new HashSet<AbstractElementWithLocation>();

		Iterator<AbstractElementWithLocation> dominatorIterator = dominatorElement1.getIterator();

		while (dominatorIterator.hasNext()) {
			AbstractElementWithLocation dominator = dominatorIterator.next();

			if (dominatorElement2.isDominatedBy(dominator)) {
				intersectingDominators.add(dominator);
			}
		}
		
		DominatorElement result = new DominatorElement(dominatorElement1.getDominatedElement(), intersectingDominators);
		
		result.update(dominatorElement1.getDominatedElement());
		
		return result;
	}  
       
	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getBottomElement()
	 */
	@Override
	public BottomElement getBottomElement() {
		return bottomElement;
	}
	
	public boolean isBottomElement(AbstractElement element) {
		return element.equals(bottomElement);
	}
	
	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getTopElement()
	 */
	@Override
	public TopElement getTopElement() {
		return topElement;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getJoinOperator()
	 */
	@Override
	public JoinOperator getJoinOperator() {
		return this;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getPartialOrder()
	 */
	@Override
	public PartialOrder getPartialOrder() {
		return this;
	}
}
