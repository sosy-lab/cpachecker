package predicateabstraction;

import java.lang.reflect.Constructor;
import java.util.Set;

import cpaplugin.CPAConfiguration;

public class Tester {

	public static void main(String[] args) {
//		String s = "dkdk";
//		System.out.println(s.concat("\""));

//		String fociFormula = "~ | [ ~ & [ ~ = sstate 4352 ~ = s_state 4528 ~ = s_state 4448 ~ = s_state 4400 = s_state 12292 ~ = s_state 4384 ~ = s_state 4560 ~ = s_state 4416 ~ = s_state 4432 ~ = s_state 4512 ~ = s_state 4464 ~ = s_state 4480 ~ = s_state 4496  ] = 0 0 ]";
//		boolean b = MathSatWrapper.satisfiability(fociFormula);
//		if(b){
//		System.out.println("sat");
//		}
//		else
//		System.out.println("unsat");

		CPAConfiguration configFile = new CPAConfiguration(args);
		//System.out.println(configFile.getProperty("dfs"));
		configFile.setProperty("bfs", "gl");
		Set e = configFile.entrySet();
//		for(Object a:e){
//		Map.Entry<String, String> ent = (Map.Entry)a;
//		String key = ent.getKey();
//		String val = ent.getValue();
//		System.out.println(key + " ======= " + val);
//		}
		//configFile.setProperty("dot.path", "gl");
		String sa[] = configFile.getPropertiesArray("analysis.programNames");
		for(String s:sa){
			System.out.println(s);
		}

		try {
			Class cls = Class.forName("predicateabstraction.Cons");
			Class partypes[] = new Class[2];
			partypes[0] = String.class;
			partypes[1] = String.class;
			Constructor ct = cls.getConstructor(partypes);
			Object arglist[] = new Object[2];
			arglist[0] = "+sadsad+";
			arglist[1] = "_sasaaS_";
			Object retobj = ct.newInstance(arglist);
		}
		catch (Throwable err) {
			System.err.println(err);
		}
	}
	
}
