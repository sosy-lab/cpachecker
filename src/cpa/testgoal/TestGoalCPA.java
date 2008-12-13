/**
 * 
 */
package cpa.testgoal;

import java.util.Collection;
import java.util.List;

import common.Pair;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import exceptions.CPATransferException;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.PrecisionDomain;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class TestGoalCPA implements ConfigurableProgramAnalysis {

  public class TestGoalPrecisionDomain implements PrecisionDomain {

  }

  public class TestGoalMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2,
                                 Precision prec) throws CPAException {
      // no join
      return pElement2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
  }

  public class TestGoalStopOperator implements StopOperator {

    @Override
    public <AE extends AbstractElement> boolean stop(AE pElement,
                                                     Collection<AE> pReached, Precision prec)
    throws CPAException {
      assert(pElement != null);
      assert(pReached != null);

      // exists lElement in pReached with stop(pElement, lElement)?
      for (AbstractElement lElement : pReached) {
        if (stop(pElement, lElement)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public boolean stop(AbstractElement pElement,
                        AbstractElement pReachedElement) throws CPAException {
      return mDomain.getPartialOrder().satisfiesPartialOrder(pElement, pReachedElement);
    }

  }

  public class TestGoalPrecisionAdjustment implements PrecisionAdjustment {

    public <AE extends AbstractElement> Pair<AE, Precision> prec(
        AE pElement,
        Precision pPrecision,
        Collection<Pair<AE, Precision>> pElements) {
      return new Pair<AE,Precision> (pElement, pPrecision);
    }

  }

  public class TestGoalTransferRelation implements TransferRelation {

    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge, Precision prec)
    throws CPATransferException {
      return mDomain.getSuccessor(pElement, pCfaEdge);
    }

    @Override
    public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation pElement, Precision prec)
    throws CPAException,
    CPATransferException {
      // this method may not be called!
      assert(false);

      return null;
    }

  }

  private AutomatonCPADomain<CFAEdge> mDomain;
  private PrecisionDomain mPrecisionDomain;
  private TestGoalTransferRelation mTransferRelation;
  private TestGoalMergeOperator mMergeOperator;
  private TestGoalStopOperator mStopOperator;
  private PrecisionAdjustment mPrecisionAdjustment;

  public TestGoalCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: Final states will not be left once they are reached.
    for (Automaton<CFAEdge>.State lState : pTestGoalAutomaton.getFinalStates()) {
      // we ensure the property by a very strict assumption:
      assert(lState.hasUnconditionalSelfLoop());
    }

    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    mPrecisionDomain = new TestGoalPrecisionDomain();
    mTransferRelation = new TestGoalTransferRelation();
    mMergeOperator = new TestGoalMergeOperator();
    mStopOperator = new TestGoalStopOperator();
    mPrecisionAdjustment = new TestGoalPrecisionAdjustment();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain<CFAEdge> getAbstractDomain() {
    return mDomain;
  }

  public PrecisionDomain getPrecisionDomain() {
    return mPrecisionDomain;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TestGoalTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public TestGoalMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public TestGoalStopOperator getStopOperator() {
    return mStopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }
}
