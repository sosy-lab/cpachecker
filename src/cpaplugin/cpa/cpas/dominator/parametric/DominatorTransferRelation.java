/**
 * 
 */
package cpaplugin.cpa.cpas.dominator.parametric;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class DominatorTransferRelation implements TransferRelation {

	private DominatorDomain domain;
	private ConfigurableProblemAnalysis cpa;
	
	public DominatorTransferRelation(DominatorDomain domain, ConfigurableProblemAnalysis cpa) {
		if (domain == null) {
			throw new IllegalArgumentException("domain is null!");
		}
		
		if (cpa == null) {
			throw new IllegalArgumentException("cpa is null!");
		}
		
		this.domain = domain;
		this.cpa = cpa;
	}
	
	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractDomain()
	 */
	@Override
	public AbstractDomain getAbstractDomain() {
		return this.domain;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cfa.objectmodel.CFAEdge)
	 */
	@Override
	public AbstractElement getAbstractSuccessor(AbstractElement element, CFAEdge cfaEdge) throws CPATransferException {
		if (!(element instanceof DominatorElement)) {
			return this.domain.getBottomElement();
		}
		
		DominatorElement dominatorElement = (DominatorElement)element;
		
		AbstractElement successorOfDominatedElement_tmp = this.cpa.getTransferRelation().getAbstractSuccessor(dominatorElement.getDominatedElement(), cfaEdge);
		
		// TODO: make this nicer
		AbstractElementWithLocation successorOfDominatedElement = (AbstractElementWithLocation)successorOfDominatedElement_tmp;
		
		if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getBottomElement())) {
			return this.domain.getBottomElement();
		}
		
		if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getTopElement())) {
			return this.domain.getTopElement();
		}
		
		DominatorElement successor = new DominatorElement(successorOfDominatedElement, dominatorElement);
		
		successor.update(successorOfDominatedElement);

		return successor;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAllAbstractSuccessors(cpaplugin.cpa.common.interfaces.AbstractElement)
	 */
	@Override
	public List<AbstractElement> getAllAbstractSuccessors(
			AbstractElement element) throws CPAException, CPATransferException {
		List<AbstractElement> successors = new ArrayList<AbstractElement>();
		
		if (element instanceof DominatorElement) {
			DominatorElement dominatorElement = (DominatorElement)element;
			
			List<AbstractElement> successorsOfDominatedElement = this.cpa.getTransferRelation().getAllAbstractSuccessors(dominatorElement.getDominatedElement());

			for (AbstractElement successorOfDominatedElement_tmp : successorsOfDominatedElement) {
				// TODO: make this nicer
				AbstractElementWithLocation successorOfDominatedElement = (AbstractElementWithLocation)successorOfDominatedElement_tmp;
				
				if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getBottomElement())) {
					successors.add(this.domain.getBottomElement());
					
					continue;
				}
				
				if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getTopElement())) {
					successors.add(this.domain.getTopElement());
					
					continue;
				}
				
				DominatorElement successor = new DominatorElement(successorOfDominatedElement, dominatorElement);
				
				successor.update(successorOfDominatedElement);

				successors.add(successor);
			}
		}
		
		return successors;
	}

}
