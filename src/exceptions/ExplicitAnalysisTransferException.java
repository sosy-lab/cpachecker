package exceptions;

public class ExplicitAnalysisTransferException extends TransferRelationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ExplicitAnalysisTransferException(){
		super();
	}

	public ExplicitAnalysisTransferException(String s){
		super(s);
	}

	public ExplicitAnalysisTransferException(String s, int i){
		super(s + " @ line " + i);
	}

}
