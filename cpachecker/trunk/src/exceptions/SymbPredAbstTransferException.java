package exceptions;

import exceptions.TransferRelationException;

public class SymbPredAbstTransferException extends TransferRelationException{

	private static final long serialVersionUID = 457;

	public SymbPredAbstTransferException(){
		super();
	}

	public SymbPredAbstTransferException(String s){
		super(s);
	}

	public SymbPredAbstTransferException(String s, int i){
		super(s + " @ line " + i);
	}
}
