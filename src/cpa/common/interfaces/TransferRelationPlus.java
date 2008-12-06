/**
 *
 */
package cpa.common.interfaces;

import java.util.List;

import cfa.objectmodel.CFAEdge;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface TransferRelationPlus {
  public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision);
  public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element, Precision precision);
}
