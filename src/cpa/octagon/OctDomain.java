package cpa.octagon;

import octagon.LibraryAccess;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

public class OctDomain implements AbstractDomain{

	private static class OctBottomElement extends OctElement implements BottomElement
    {
        public OctBottomElement ()
        {
        	super ();
        }
    }

    private static class OctTopElement extends OctElement implements TopElement
    {
    	public OctTopElement ()
        {
            //super (LibraryAccess.universe(Variables.numOfVars));
        }
    }

    private static class OctPartialOrder implements PartialOrder
    {
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
            OctElement octElement1 = (OctElement) element1;
            OctElement octElement2 = (OctElement) element2;

            boolean b = LibraryAccess.isIn(octElement1, octElement2);
            return b;
        }
    }

    private static class OctJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
        	// TODO fix
        	OctElement octEl1 = (OctElement) element1;
    		OctElement octEl2 = (OctElement) element2;
    		return LibraryAccess.widening(octEl1, octEl2);
        }
    }

    private final static BottomElement bottomElement = new OctBottomElement ();
    private final static TopElement topElement = new OctTopElement ();
    private final static PartialOrder partialOrder = new OctPartialOrder ();
    private final static JoinOperator joinOperator = new OctJoinOperator ();

    public OctDomain ()
    {

    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

    //TODO test this
	public boolean isBottomElement(AbstractElement element) {
		OctElement octElem = (OctElement) element;
		return octElem.isEmpty();
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
}
