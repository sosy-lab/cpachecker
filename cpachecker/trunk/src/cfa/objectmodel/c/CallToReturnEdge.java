package cfa.objectmodel.c;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;

import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractElement;
import cpa.predicateabstraction.PredicateAbstractionElement;

public class CallToReturnEdge extends AbstractCFAEdge {

	private IASTExpression expression;
	private AbstractElement abstractElement;

	public CallToReturnEdge(String rawStatement, IASTExpression exp) {
		super(rawStatement);
		this.expression = exp;
	}

	public void initializeSummaryEdge(CFANode predecessorNode, CFANode successorNode) {
		predecessorNode.addLeavingSummaryEdge(this);
		predecessor = predecessorNode;
		successorNode.addEnteringSummaryEdge (this);
		successor= successorNode;
	}

	public IASTExpression getExpression ()
	{
		return expression;
	}

	public CFAEdgeType getEdgeType ()
	{
		return CFAEdgeType.CallToReturnEdge;
	}

	public AbstractElement getAbstractElement() {
		return abstractElement;
	}

	public void setAbstractElement(AbstractElement abstractElement) {
		this.abstractElement = abstractElement;
	}

	public AbstractElement extractAbstractElement(String elementName){
		CompositeElement compElem = (CompositeElement) abstractElement;
		List<AbstractElement> compElems = compElem.getElements();
		for(AbstractElement item:compElems){
			if(item.getClass().getSimpleName().equals(elementName)){
				return item;
			}
		}
		return null;
	}

  /**
   * @return
   */
  public boolean hasAnyPointerParameters() {
    // db: Not implemented.
    assert(false);
    return false;
  }

  /**
   * @return
   */
  public Iterable<AliasedPointers> getAliasedPointersList() {
    // db: Not implemented.
    assert(false);
    return null;
  }

  /**
   * @param pArgumentName
   * @param pParameterName
   */
  public void registerAliasesOnFunctionCalls(String pArgumentName,
                                             String pParameterName) {
    // db: Not implemented.
    assert(false);
  }

  /**
   * @param pString
   * @param pPredAbsElement
   */
  public void registerElementOnSummaryEdge(
                                           String pString,
                                           PredicateAbstractionElement pPredAbsElement) {
    // db: Not implemented.
    assert(false);
  }

  /**
   * @param pString
   * @return
   */
  public PredicateAbstractionElement retrieveAbstractElement(String pString) {
    // db: Not implemented.
    assert(false);
    return null;
  }
}
