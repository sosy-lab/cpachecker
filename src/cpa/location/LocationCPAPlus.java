/**
 *
 */
package cpa.location;

import java.util.Collection;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.Pair;

import cpa.common.CPATransferException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.CPAPlus;
import cpa.common.interfaces.MergeOperatorPlus;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperatorPlus;
import cpa.common.interfaces.TransferRelationPlus;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class LocationCPAPlus implements CPAPlus {

  private class LocationMergeSepPlus implements MergeOperatorPlus {

    private final LocationCPA locationCPA;

    public LocationMergeSepPlus (LocationCPA pLocationCPA) {
      locationCPA = pLocationCPA;
    }

    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2, Precision pPrecision)
                                                                                 throws CPAException {
      return merge((AbstractElementWithLocation)pElement1, (AbstractElementWithLocation)pElement2,
          pPrecision);
    }

    public AbstractElementWithLocation merge(
                                             AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision pPrecision)
                                                                  throws CPAException {
      // return locationCPA.getMergeOperator().merge(pElement1, pElement2);
      // TODO fix as soon as AbstractElementWithLocation patch series is in trunk
      return null;
    }

  }

  private class LocationPrecisionAdjustment implements PrecisionAdjustment {

    public <AE extends AbstractElement> Pair<AE, Precision> prec(
                                                                 AE pElement,
                                                                 Precision pPrecision,
                                                                 Collection<Pair<AE, Precision>> pElements) {
      return new Pair<AE,Precision>(pElement, pPrecision);
    }
  }

  private class LocationStopSepPlus implements StopOperatorPlus {

    private final LocationCPA locationCPA;

    public LocationStopSepPlus (LocationCPA pLocationCPA) {
      locationCPA = pLocationCPA;
    }

    public <AE extends AbstractElement> boolean stop(AE pElement,
                                                     Collection<AE> pReached,
                                                     Precision pPrecision)
                                                                          throws CPAException {
      // return locationCPA.getStopOperator().stop(pElement, pReached);
      // TODO fix as soon as AbstractElementWithLocation patch series is in trunk
      return false;
    }
  }

  private class LocationTransferRelationPlus implements TransferRelationPlus {

    private final LocationCPA locationCPA;

    public LocationTransferRelationPlus (LocationCPA pLocationCPA) {
      locationCPA = pLocationCPA;
    }

    public AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge,
                                                Precision pPrecision)
                                                                     throws CPATransferException {
      return locationCPA.getTransferRelation().getAbstractSuccessor(pElement, pCfaEdge);
    }

    public List<AbstractElementWithLocation> getAllAbstractSuccessors(
                                                                      AbstractElementWithLocation pElement,
                                                                      Precision pPrecision)
                                                                                           throws CPAException,
                                                                                           CPATransferException {
      // return locationCPA.getTransferRelation().getAllAbstractSuccessors(pElement);
      // TODO fix as soon as AbstractElementWithLocation patch series is in trunk
      return null;
    }
  }

  private final LocationCPA locationCPA;
  private final LocationMergeSepPlus mergeOp;
  private final LocationPrecisionAdjustment prec;
  private final LocationStopSepPlus stopOp;
  private final LocationTransferRelationPlus transfer;

  public LocationCPAPlus (String mergeType, String stopType) throws CPAException {
    locationCPA = new LocationCPA(mergeType, stopType);
    mergeOp = new LocationMergeSepPlus(locationCPA);
    prec = new LocationPrecisionAdjustment();
    stopOp = new LocationStopSepPlus(locationCPA);
    transfer = new LocationTransferRelationPlus(locationCPA);
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getAbstractDomain()
   */
  public AbstractDomain getAbstractDomain() {
    return locationCPA.getAbstractDomain();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public AbstractElement getInitialElement(
                                                           CFAFunctionDefinitionNode pNode) {
    return locationCPA.getInitialElement(pNode);
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getInitialPrecision(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getMergeOperator()
   */
  public MergeOperatorPlus getMergeOperator() {
    return mergeOp;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getPrecisionAdjustment()
   */
  public PrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getPrecisions()
   */
  public Collection<Precision> getPrecisions() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getStopOperator()
   */
  public StopOperatorPlus getStopOperator() {
    return stopOp;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.CPAPlus#getTransferRelation()
   */
  public TransferRelationPlus getTransferRelation() {
    return transfer;
  }

}
