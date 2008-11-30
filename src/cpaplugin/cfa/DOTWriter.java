package cpaplugin.cfa;

import java.util.ArrayList;
import java.util.List;

public class DOTWriter {

	protected String name;
	private List<String> entries;

	public DOTWriter(String functionName) {
		name = functionName;
		entries = new ArrayList<String>();
	}
	
	public void add(String s){
		entries.add(s);
	}
	
	public String getSubGraph(){
		String s = "";
		for(String line:entries){
			s = s + line + "\n";
		}
		return s;
	}
}
