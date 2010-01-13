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

import cfa.objectmodel.CFAEdge;
import cmdline.CPAMain;
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
      start = System.currentTimeMillis();
      if(timeLimit == 0){
        successors = future.get();
      }
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      else{
        assert(timeLimit > 0);
        successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      }
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
    List<AbstractElement> retList = new ArrayList<AbstractElement>();
    
    try {
       Collection<? extends AbstractElement> wrappedList = transferRelation.strengthen(wrappedElement, otherElements, cfaEdge, precision);
       // if the returned list is null return null
       if(wrappedList == null)
         return null;
    // TODO we assume that only one element is returned or empty set to represent bottom
       assert(wrappedList.size() < 2);
       // if bottom return empty list
       if(wrappedList.size() == 0){
         return retList;
       }
       
       AbstractElement wrappedReturnElement = wrappedList.iterator().next();
       retList.add(new TransferRelationMonitorElement(monitorElement.getCpa(), wrappedReturnElement));
       return retList;
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
    
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
