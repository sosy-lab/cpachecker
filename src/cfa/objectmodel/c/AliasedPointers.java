package cfa.objectmodel.c;

public class AliasedPointers {

	private String var1;
	private String var2;

	public AliasedPointers(String s1, String s2){
		var1 = s1;
		var2 = s2;
	}

	public String getFirstVar(){
		return var1;
	}

	public String getSecondVar(){
		return var2;
	}
}
