/**
 *
 */
package cpa.common.interfaces;

import java.util.List;

import cfa.objectmodel.CFAEdge;
import exceptions.CPATransferException;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface TransferRelationPlus {
  public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision)
    throws CPATransferException;
  public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision precision)
    throws CPAException, CPATransferException;
}
