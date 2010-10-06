package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.AbstractFormula;

public class AbstractionElement implements SymbPredAbsAbstractElement {

  public static class Factory {
    private int mNextId;
    
    public Factory() {
      mNextId = 0;
    }
    
    public AbstractionElement create(AbstractionElement pPreviousAbstractionElement, CFANode pAbstractionLocation, AbstractFormula pAbstractionFormula, PathFormula pInitAbstractionFormula) {
      return new AbstractionElement(pPreviousAbstractionElement, pAbstractionLocation, pAbstractionFormula, pInitAbstractionFormula, mNextId++);
    }
    
    public AbstractionElement createInitialElement(CFANode pAbstractionLocation, AbstractFormula pAbstractionFormula, PathFormula pInitAbstractionFormula) {
      return create(null, pAbstractionLocation, pAbstractionFormula, pInitAbstractionFormula);
    }
    
    public int getNumberOfCreatedAbstractionElements() {
      return mNextId;
    }
  }
  
  private final CFANode mAbstractionLocation;
  
  /** If this node is not an abstraction node, then this is invalid;
   * otherwise this is the {@link PathFormula} of the last element before the
   * abstraction is computed. This formula is used by the refinement procedure
   * to build the formula to the error location */
  private final PathFormula mInitAbstractionFormula;
    
  /** The abstraction which is updated only on abstraction locations */
  private AbstractFormula mAbstractionFormula;
  
  //public static int INSTANCES = 0;
  
  public final int ID;
  
  private AbstractionElement mPreviousAbstractionElement;
  
  private AbstractionElement(AbstractionElement pPreviousAbstractionElement, CFANode pAbstractionLocation, AbstractFormula pAbstractionFormula, PathFormula pInitAbstractionFormula, int pID) {
    mAbstractionLocation = pAbstractionLocation;
    mAbstractionFormula = pAbstractionFormula;
    mInitAbstractionFormula = pInitAbstractionFormula;
    //INSTANCES++;
    ID = pID;
    
    mPreviousAbstractionElement = pPreviousAbstractionElement;
  }
  
  public CFANode getLocation() {
    return mAbstractionLocation;
  }
  
  public AbstractFormula getAbstractionFormula() {
    return mAbstractionFormula;
  }
  
  public PathFormula getInitAbstractionFormula() {
    return mInitAbstractionFormula;
  }
  
  @Override
  public int getSizeSinceAbstraction() {
    return 0;
  }

  public boolean hasPreviousAbstractionElement() {
    return (mPreviousAbstractionElement != null);
  }
  
  public AbstractionElement getPreviousAbstractionElement() {
    return mPreviousAbstractionElement;
  }
  
  @Override
  public AbstractionElement getAbstractionElement() {
    return this;
  }
  
}
