package cpa.transferrelationmonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cmdline.CPAMain;

import cfa.objectmodel.CFAEdge;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;
import exceptions.TransferTimeOutException;

public class TransferRelationMonitorTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;
  private TransferCallable tc = new TransferCallable();

  public TransferRelationMonitorTransferRelation(TransferRelation pTransferRelation) {
    transferRelation = pTransferRelation;
  }

  @Override
  public Collection<TransferRelationMonitorElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, TransferTimeOutException {
    TransferRelationMonitorElement element = (TransferRelationMonitorElement)pElement;
    Collection<? extends AbstractElement> successors = null;
    
    AbstractElement wrappedElement = element.getWrappedElements().iterator().next();
    Precision wrappedPrecision = null;
    
    if(pPrecision != null){
      wrappedPrecision = ((TransferRelationMonitorPrecision)pPrecision).getPrecision();
    }

    // time limit is given in milliseconds
    long timeLimit = Integer.parseInt(CPAMain.cpaConfig.getPropertiesArray
        ("predicateabstraction.trackabstractioncomputation.limit")[0]);
    // set the edge and element
    tc.setEdge(pCfaEdge);
    tc.setElement(wrappedElement);
    tc.setPrecision(wrappedPrecision);
    Future<Collection<? extends AbstractElement>> future = CEGARAlgorithm.executor.submit(tc);
    try{
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
    } catch (TimeoutException exc){
      throw new TransferTimeOutException(pCfaEdge, wrappedElement, wrappedPrecision);
    } catch (InterruptedException exc) {
      exc.printStackTrace();
    } catch (ExecutionException exc) {
      exc.printStackTrace();
    }

    assert(successors != null);

    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Collection<TransferRelationMonitorElement> wrappedSuccessors = new ArrayList<TransferRelationMonitorElement>();
    for (AbstractElement absElement : successors) {
      TransferRelationMonitorElement successorElem = new TransferRelationMonitorElement(element.getCpa(), absElement);
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {    
    return null;
  }

  private class TransferCallable implements Callable<Collection<? extends AbstractElement>>{

    CFAEdge cfaEdge;
    AbstractElement abstractElement;
    Precision precision;

    public TransferCallable() {

    }

    @Override
    public Collection<? extends AbstractElement> call() throws Exception {
      return transferRelation.getAbstractSuccessors(abstractElement, precision, cfaEdge);
    }

    public void setEdge(CFAEdge pCfaEdge){
      cfaEdge = pCfaEdge;
    }

    public void setElement(AbstractElement pAbstractElement){
      abstractElement = pAbstractElement;
    }

    public void setPrecision(Precision pPrecision){
      precision = pPrecision;
    }
  }

}
