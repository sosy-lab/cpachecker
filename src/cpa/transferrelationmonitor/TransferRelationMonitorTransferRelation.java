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
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;
import exceptions.TransferTimeOutException;

public class TransferRelationMonitorTransferRelation implements TransferRelation {

  private final AbstractDomain domain;
  private final TransferRelation transferRelation;
  private TransferCallable tc = new TransferCallable();
  private long timeLimit = 0;
  private long timeLimitForPath = 0;
  
  public TransferRelationMonitorTransferRelation(ConfigurableProgramAnalysis pWrappedCPA) {
    transferRelation = pWrappedCPA.getTransferRelation();
    domain = pWrappedCPA.getAbstractDomain();
 // time limit is given in milliseconds
    timeLimit = Integer.parseInt(CPAMain.cpaConfig.getPropertiesArray
        ("trackabstractioncomputation.limit")[0]);
    timeLimitForPath = Integer.parseInt(CPAMain.cpaConfig.getPropertiesArray
        ("trackabstractioncomputation.pathcomputationlimit")[0]);
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
    
// TODO i'll move all of this to somwehere else - Erkan
// // try this later com.sun.management.OperatingSystemMXBean
//
// try {
//         FileInputStream fis = new FileInputStream("/proc/meminfo");
//         DataInputStream dis = new DataInputStream(fis);
//         BufferedReader bfr = new BufferedReader(new InputStreamReader(dis));
//         String line;
//         
//         long memTotal = 0;
//         long memFree = 0;
//         long buffers = 0;
//         long cached = 0;
//         
//         while((line = bfr.readLine()) != null){
//           //          MemTotal:        2060840 kB
//           //          MemFree:         1732952 kB
//           //          Buffers:            3164 kB
//           //          Cached:            58376 kB
//           if(line.contains("MemTotal:")){
//             continue;
//           }
//           else if(line.contains("MemFree:")){
//             memFree = Long.valueOf(line.split("\\s+")[1]);
//           }
//           else{
//             break;
//           }
//         }
//         
////         long totalFree = memTotal - (memFree + buffers + cached);
//         long totalFree = memFree;
//         
//         if(memFree < 100000){
//           System.out.println("MEM IS OUT");
//           return;
//         }
//         
//         dis.close();
//       } catch (Exception e1) {
//         e1.printStackTrace();
//       }
    
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
      if (absElement.equals(domain.getBottomElement())) {
        // omit bottom element from successors list
        continue;
      }
      TransferRelationMonitorElement successorElem = new TransferRelationMonitorElement(element.getCpa(), absElement);
      successorElem.setTransferTime(timeOfExecution);
      successorElem.setTotalTime(element.getTotalTimeOnThePath());
      if(successorElem.getTotalTimeOnThePath() > timeLimitForPath){
        throw new TransferTimeOutException(pCfaEdge, wrappedElement, pPrecision);
      }
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
