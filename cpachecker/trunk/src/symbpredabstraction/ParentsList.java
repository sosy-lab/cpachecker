package symbpredabstraction;

import java.util.ArrayList;
import java.util.List;

public class ParentsList {

	private List<Integer> parents;

	public ParentsList(){
		parents = new ArrayList<Integer>();
	}

	public void addToList(int i){
		parents.add(i);
	}

	@Override
	public boolean equals(Object o){
		ParentsList otherParentsList = (ParentsList) o;
		List<Integer> otherList = otherParentsList.parents;
		if (this.parents.size() != otherList.size()){
			return false;
		}
		else{
			for(int i=0; i<otherList.size(); i++){
				if(this.parents.get(i) != otherList.get(i)){
					return false;
				}
			}
		}
		return true;
	}

	public void copyFromExisting(ParentsList parents2) {
		parents.addAll(parents2.parents);
	}
}
