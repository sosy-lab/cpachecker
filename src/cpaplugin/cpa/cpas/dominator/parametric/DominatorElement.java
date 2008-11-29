/**
 * 
 */
package cpaplugin.cpa.cpas.dominator.parametric;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * @author holzera
 *
 */
public class DominatorElement implements AbstractElementWithLocation {

	private AbstractElementWithLocation dominatedElement;
	private Set<AbstractElementWithLocation> dominators = new HashSet<AbstractElementWithLocation>();
	
	public DominatorElement(AbstractElementWithLocation dominatedElement) {
		if (dominatedElement == null) {
			throw new IllegalArgumentException("dominatedElement is null!");
		}
		
		this.dominatedElement = dominatedElement;
	}
	
	public DominatorElement(AbstractElementWithLocation dominatedElement, Set<AbstractElementWithLocation> dominators) {
		this(dominatedElement);
		
		if (dominators == null) {
			throw new IllegalArgumentException("dominators is null!");
		}
		
		this.dominators.addAll(dominators);
	}
	
	public DominatorElement(DominatorElement other) {
		this(other.dominatedElement, other.dominators);
	}
	
	public DominatorElement(AbstractElementWithLocation dominatedElement, DominatorElement other) {
		this(dominatedElement, other.dominators);
	}
	
	@Override
	public DominatorElement clone()
    {
        return new DominatorElement(this);
    }
	
	public void update(AbstractElementWithLocation dominator) {
		if (dominator == null) {
			throw new IllegalArgumentException("dominator is null!");
		}
		
		dominators.add(dominator);
	}
	
	public AbstractElementWithLocation getDominatedElement() {
		return this.dominatedElement;
	}
	
	public Iterator<AbstractElementWithLocation> getIterator ()
    {
        return this.dominators.iterator();
    }
	
	public boolean isDominatedBy(AbstractElementWithLocation dominator) {
		return this.dominators.contains(dominator);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DominatorElement)) {
			return false;
		}
		
		DominatorElement other_element = (DominatorElement)other;
		
		if (!(this.dominatedElement.equals(other_element.dominatedElement))) {
			return false;
		}
		
		if (dominators.size() != other_element.dominators.size()) {
			return false;
		}
		
		for (AbstractElementWithLocation dominator : dominators) {
			if (!other_element.isDominatedBy(dominator)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder ();
        builder.append ("( " + this.dominatedElement.toString() + ", {");
        
        boolean first = true;
        for (AbstractElementWithLocation dominator : this.dominators) {
        	if (first)  {
        		first = false;
        	}
        	else {
        		builder.append(", ");
        	}
        	
        	builder.append(dominator.toString());
        }
        
        builder.append ("})");
        
        return builder.toString ();
	}

	public CFANode getLocationNode() {
		return this.dominatedElement.getLocationNode();
	}
	
	@Override
	public int hashCode() {
		// TODO: create better hash code?
		return this.dominatedElement.hashCode();
	}
}
