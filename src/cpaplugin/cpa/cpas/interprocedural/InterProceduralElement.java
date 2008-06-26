package cpaplugin.cpa.cpas.interprocedural;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractElement;

public class InterProceduralElement implements AbstractElement
{
	private List<CallElement> callStack;
	
	public InterProceduralElement ()
	{
		this.callStack = new ArrayList<CallElement>();
	}

	public InterProceduralElement (List<CallElement> stack)
	{
		this.callStack = stack;
	}

	public List<CallElement> getCallStack ()
	{
		return callStack;
	}

	public boolean equals (Object other)
	{
		if (!(other instanceof InterProceduralElement)){
			return false;
		}
		
		InterProceduralElement ipe = (InterProceduralElement) other;
		if (ipe.getStackSize() != this.getStackSize()){
			return false;
		}

		Iterator<CallElement> it = this.callStack.iterator();
		while(it.hasNext()){
			CallElement ce = it.next();
			if(!ipe.containsCallElement(ce)){
				return false;
			}
		}
		return true;
	}

	public boolean containsCallElement (CallElement elem){
		return (this.callStack.contains(elem));
	}

	public boolean containsCall(String fName){
		Iterator<CallElement> it = this.callStack.iterator();
		while(it.hasNext()){
			CallElement ce = it.next();
			if(ce.getFunctionName().compareTo("fName") == 0){
				return true;
			}
		}
		return false;
	}

	public String toString ()
	{
		String s = "Call Stack: \n ============ \n";
		Iterator<CallElement> it = this.callStack.iterator();
		while(it.hasNext()){
			CallElement ce = it.next();
			s = s + ce + "\n";
		}
		return s;
	}
	
	public int getStackSize(){
		return this.callStack.size();
	}
	
	public InterProceduralElement clone(){
		List<CallElement> list = new ArrayList<CallElement>();
		Iterator<CallElement> it = callStack.iterator();
		while(it.hasNext()){
			CallElement elem = it.next();
			list.add(elem);
		}
		return new InterProceduralElement(list);
	}
	
	public void addCallElement(CallElement ce){
		this.callStack.add(ce);
	}
	
	public CallElement getCallElement(String pfName) {
		Iterator<CallElement> it = callStack.iterator();
		assert(containsCall(pfName));
		while(it.hasNext()){
			CallElement elem = it.next();
			if(elem.getFunctionName().compareTo(pfName) == 0){
				return elem;
			}
		}
		return null;
	}

	public void removeCallElement(String pfName) {
		this.callStack.remove(getCallElement(pfName));
	}
}
