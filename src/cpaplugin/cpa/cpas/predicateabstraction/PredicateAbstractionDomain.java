package cpaplugin.cpa.cpas.predicateabstraction;

import java.io.IOException;

import predicateabstraction.MathSatWrapper;
import predicateabstraction.Predicate;
import predicateabstraction.ThreeValuedBoolean;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class PredicateAbstractionDomain implements AbstractDomain{

    private static class PredicateAbstractionBottomElement extends PredicateAbstractionElement implements BottomElement
    {
        public PredicateAbstractionBottomElement ()
        {
            super (null, true);
        }
    }
    
    private static class PredicateAbstractionTopElement implements TopElement
    {
    	public PredicateAbstractionTopElement ()
        {
        }
    }
    
    private static class PredicateAbstractionPreOrder implements PreOrder
    {
    	// returns true if element1 < element2 on lattice
        public boolean satisfiesPreOrder (AbstractElement element1, AbstractElement element2)
        {
        	PredicateAbstractionElement predicateAbstractionElement1 = (PredicateAbstractionElement) element1;
        	PredicateAbstractionElement predicateAbstractionElement2 = (PredicateAbstractionElement) element2;
            
        	ThreeValuedBoolean tbv = ThreeValuedBoolean.DONTKNOW;
			try {
				tbv = MathSatWrapper.implies(predicateAbstractionElement1.getRegion(), predicateAbstractionElement2.getRegion());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if(tbv == ThreeValuedBoolean.TRUE){
            	return true;
            }
            else{
            	return false;
            }
        }
    }
    
    private static class PredicateAbstractionJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            PredicateAbstractionElement predAbstElement1 = (PredicateAbstractionElement) element1;
            PredicateAbstractionElement predAbstElement2 = (PredicateAbstractionElement) element2;

            if(predAbstElement1.getPredicateList().size() != 
            	predAbstElement2.getPredicateList().size()){
            	// TODO handle this case
            }
            
            PredicateAbstractionElement joined = new PredicateAbstractionElement(predAbstElement1.getPredicateList(), false);
            
            for(Predicate pred1:predAbstElement1.getPredicateList().getPredicates()){
            	Predicate pred2 = predAbstElement2.getPredicateList().getPredicate(pred1);
            	Predicate joinedPred = joined.getPredicateList().getPredicate(pred1);
            	if(pred1.getTruthValue() == pred2.getTruthValue()){
            		joinedPred.setTruthValue(pred1.getTruthValue());
            	}
            	else{
            		joinedPred.setTruthValue(ThreeValuedBoolean.DONTKNOW);
            	}
            }
            return joined;
        }
    }
    
    private final static BottomElement bottomElement = new PredicateAbstractionBottomElement ();
    private final static TopElement topElement = new PredicateAbstractionTopElement ();
    private final static PreOrder preOrder = new PredicateAbstractionPreOrder ();
    private final static JoinOperator joinOperator = new PredicateAbstractionJoinOperator ();
       
    public PredicateAbstractionDomain ()
    {

    }
    
    public BottomElement getBottomElement ()
    {
        return bottomElement;
    }
    
    public TopElement getTopElement ()
    {
        return topElement;
    }

    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PreOrder getPreOrder ()
    {
        return preOrder;
    }
}
