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
import cpa.common.automaton.AutomatonCPADomain.BottomElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;
import exceptions.TransferTimeOutException;

public class TransferRelationMonitorTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;
  private TransferCallable tc = new TransferCallable();
  private final TransferRelationMonitorDomain domain;
  
  public TransferRelationMonitorTransferRelation(TransferRelation pTransferRelation, TransferRelationMonitorDomain pDomain) {
    transferRelation = pTransferRelation;
    domain = pDomain;
  }

  @Override
  public Collection<TransferRelationMonitorElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, TransferTimeOutException {
    TransferRelationMonitorElement element = (TransferRelationMonitorElement)pElement;
    Collection<? extends AbstractElement> successors = null;
    long timeOfExecution = 0;
    long start = 0;
    long end = 0;
    
    AbstractElement wrappedElement = element.getWrappedElements().iterator().next();

    // time limit is given in milliseconds
    long timeLimit = Integer.parseInt(CPAMain.cpaConfig.getPropertiesArray
        ("predicateabstraction.trackabstractioncomputation.limit")[0]);
    // set the edge and element
    tc.setEdge(pCfaEdge);
    tc.setElement(wrappedElement);
    tc.setPrecision(pPrecision);
    Future<Collection<? extends AbstractElement>> future = CEGARAlgorithm.executor.submit(tc);
    try{
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      start = System.currentTimeMillis();
      successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      end = System.currentTimeMillis();
    } catch (TimeoutException exc){
      throw new TransferTimeOutException(pCfaEdge, wrappedElement, pPrecision);
    } catch (InterruptedException exc) {
      exc.printStackTrace();
    } catch (ExecutionException exc) {
      exc.printStackTrace();
    }

    timeOfExecution = end-start;

    assert(successors != null);
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Collection<TransferRelationMonitorElement> wrappedSuccessors = new ArrayList<TransferRelationMonitorElement>();
    for (AbstractElement absElement : successors) {
      TransferRelationMonitorElement successorElem = new TransferRelationMonitorElement(element.getCpa(), absElement);
      successorElem.setTransferTime(timeOfExecution);
      successorElem.setTotalTime(element.getTotalTimeOnThePath());
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {    
    TransferRelationMonitorElement monitorElement = (TransferRelationMonitorElement)element;
    AbstractElement wrappedElement = monitorElement.getWrappedElements().iterator().next();
    
    AbstractElement wrappedReturnElement = null;
    try {
      wrappedReturnElement = transferRelation.strengthen(wrappedElement, otherElements, cfaEdge, precision);
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
   
    if(wrappedReturnElement == null)
      return null;
    
    if(wrappedReturnElement instanceof BottomElement)
      return 
      
    return new TransferRelationMonitorElement(monitorElement.getCpa(), wrappedReturnElement);
    
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
