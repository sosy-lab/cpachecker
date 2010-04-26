package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class TransferRelationMonitorDomain implements AbstractDomain{

  private final TransferRelationMonitorCPA cpa;

  private static class TransferRelationMonitorBottomElement extends TransferRelationMonitorElement
  {
    public TransferRelationMonitorBottomElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof TransferRelationMonitorBottomElement);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public String toString() {
      return "TransferRelationMonitorBottomElement";
    }
  }

  private static class TransferRelationMonitorTopElement extends TransferRelationMonitorElement
  {
    public TransferRelationMonitorTopElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof TransferRelationMonitorTopElement);
    }

    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
      return "ArtTopElement";
    }
  }

  private static class ArtPartialOrder implements PartialOrder
  {
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
    {
      if (element1.equals (element2)){
        return true;
      }

      if (element1 instanceof TransferRelationMonitorBottomElement || element2 instanceof TransferRelationMonitorTopElement){
        return true;
      }

      return false;
    }
  }

  private static class ArtJoinOperator implements JoinOperator
  {
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      // Useless code, but helps to catch bugs by causing cast exceptions
      TransferRelationMonitorElement transferRelationMonitorElement1 = (TransferRelationMonitorElement) element1;
      TransferRelationMonitorElement transferRelationMonitorElement2 = (TransferRelationMonitorElement) element2;

      if (transferRelationMonitorElement1.equals (transferRelationMonitorElement2))
        return transferRelationMonitorElement1;

      if (transferRelationMonitorElement1.equals(bottomElement))
        return transferRelationMonitorElement2;
      if (transferRelationMonitorElement2.equals(bottomElement))
        return transferRelationMonitorElement1;

      return topElement;
    }
  }
  
  private final static TransferRelationMonitorBottomElement bottomElement = new TransferRelationMonitorBottomElement ();
  private final static TransferRelationMonitorTopElement topElement = new TransferRelationMonitorTopElement ();
  private final static PartialOrder partialOrder = new ArtPartialOrder ();
  private final static JoinOperator joinOperator = new ArtJoinOperator ();

  public TransferRelationMonitorDomain(TransferRelationMonitorCPA pCpa)
  {
    cpa = pCpa;
  }

  public AbstractElement getBottomElement ()
  {
      return bottomElement;
  }

  public AbstractElement getTopElement ()
  {
      return topElement;
  }

  public JoinOperator getJoinOperator ()
  {
      return joinOperator;
  }

  public PartialOrder getPartialOrder ()
  {
      return partialOrder;
  }

  public TransferRelationMonitorCPA getCpa(){
    return cpa;
  }
}
