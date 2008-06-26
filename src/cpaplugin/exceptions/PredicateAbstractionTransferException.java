package cpaplugin.exceptions;

public class PredicateAbstractionTransferException extends TransferRelationException
{
	private static final long serialVersionUID = 457;
	
	public PredicateAbstractionTransferException(){
		super();
	}
	
	public PredicateAbstractionTransferException(String s){
		super(s);
	}
	
	public PredicateAbstractionTransferException(String s, int i){
		super(s + " @ line " + i);
	}

}
