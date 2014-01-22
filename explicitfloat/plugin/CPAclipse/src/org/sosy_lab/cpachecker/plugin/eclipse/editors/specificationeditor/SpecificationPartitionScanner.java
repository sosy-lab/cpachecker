package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class SpecificationPartitionScanner extends RuleBasedPartitionScanner {
	
	private final static String[] keywords = {
		"STATE",
		"USEALL",
		"USEFIRST",
		"INITIAL",
		"LOCAL",
		"MATCH",
		"ASSERT",
		"ERROR",
		"STOP",
		"AUTOMATON",
		"END AUTOMATON",
		"DO",
		"GOTO",
		"CHECK",
		"MODIFY",
		"PRINT",
		"EXIT"
	};

	public SpecificationPartitionScanner() {

		List<IPredicateRule> rules = new ArrayList<IPredicateRule>();
		rules.add(new SingleLineRule("\"", "\"", new Token(SpecificationConfiguration.AUTOMATON_STRING), '\\')); 
		Token keywordToken = new Token(SpecificationConfiguration.AUTOMATON_KEYWORD);
		for (int i = 0; i < keywords.length; i++) {
			rules.add(new SingleLineRule(keywords[i], " ", keywordToken));
		}
		
		rules.add(new MultiLineRule("/*", "*/", new Token(SpecificationConfiguration.SPECIFICATION_COMMENT)));
		rules.add(new EndOfLineRule("//", new Token(SpecificationConfiguration.SPECIFICATION_COMMENT)));
		
		rules.add(new EndOfLineRule("#include ", new Token(SpecificationConfiguration.SPECIFICATION_INCLUDE)));

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
