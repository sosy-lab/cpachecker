/**
 *
 */
package cpa.common.interfaces;

import java.util.Collection;

import cfa.objectmodel.CFAFunctionDefinitionNode;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface CPAPlus {
  public AbstractDomain getAbstractDomain();
  public Collection<Precision> getPrecisions ();
  public TransferRelationPlus getTransferRelation ();
  public MergeOperatorPlus getMergeOperator ();
  public StopOperatorPlus getStopOperator ();
  public <AE extends AbstractElement> AE getInitialElement (CFAFunctionDefinitionNode node);
  public Precision getInitialPrecision (CFAFunctionDefinitionNode node);
  public PrecisionAdjustment getPrecisionAdjustment ();
}
