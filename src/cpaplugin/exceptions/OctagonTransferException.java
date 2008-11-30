package cpaplugin.exceptions;

public class OctagonTransferException extends TransferRelationException
{
	private static final long serialVersionUID = 3;
	
	public OctagonTransferException(){
		super();
	}
	
	public OctagonTransferException(String s){
		super(s);
	}
	
	public OctagonTransferException(String s, int i){
		super(s + " @ line " + i);
	}

}
