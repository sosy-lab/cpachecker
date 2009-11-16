package cpa.art;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class ARTDomain implements AbstractDomain {
  
  private final ARTCPA cpa;

  private static class ArtBottomElement extends ARTElement
  {
    public ArtBottomElement() {
      super(null, null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof ArtBottomElement);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public String toString() {
      return "ArtBottomElement";
    }
  }

  private static class ArtTopElement extends ARTElement
  {
    public ArtTopElement() {
      super(null, null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof ArtTopElement);
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

      if (element1 instanceof ArtBottomElement || element2 instanceof ArtTopElement){
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
      ARTElement artElement1 = (ARTElement) element1;
      ARTElement artElement2 = (ARTElement) element2;

      if (artElement1.equals (artElement2))
        return artElement1;

      if (artElement1.equals(bottomElement))
        return artElement2;
      if (artElement2.equals(bottomElement))
        return artElement1;

      return topElement;
    }
  }
  
  private final static ArtBottomElement bottomElement = new ArtBottomElement ();
  private final static ArtTopElement topElement = new ArtTopElement ();
  private final static PartialOrder partialOrder = new ArtPartialOrder ();
  private final static JoinOperator joinOperator = new ArtJoinOperator ();

  public ARTDomain(ARTCPA pCpa)
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

  public ARTCPA getCpa(){
    return cpa;
  }
  
}