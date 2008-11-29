package cpaplugin.cpa.common.interfaces;

import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.exceptions.CPAException;

/**
 * Interface for transfer relations.
 * The instance of the relation is used to calculate the post operation
 * @author erkan
 *
 */
public interface TransferRelation
{
    /**
     * @return abstract domain associated with the transfer relation, may be useful
     * if bottom or top elements is accessed by an operation triggered on transfer
     * relation
     */
    public AbstractDomain getAbstractDomain ();
    /**
     * Transfers the state to the next abstract state. For example if the analysis in on node 3 and it 
     * will proceed to node 4, there is an edge from node 3 to 4. 
     * Element is the abstract element on node 3 and it is copied as the element on node 4. The copied element
     * will be updated by processing the edge.
     * @param element abstract element on current state
     * @param cfaEdge the edge from one location of CFA to the other
     * @return updated abstract element
     */
    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge) 
        throws CPATransferException;
    /** Gets all successors of the current element. This method returns a non-null list
     * only if the abstract element contains traversal information such as a CFA or CFG
     * @param element abstract element on current state
     * @return list of all successors of the current state
     * @throws CPAException if the element does not contain any traversal information such as nodes
     * and edges on CFA.
     */
    public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) 
        throws CPAException, CPATransferException;
}
