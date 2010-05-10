package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class AutomatonPartitionScanner extends RuleBasedPartitionScanner {
	public final static String AUTOMATON_MULTI_LINE_COMMENT = "__automaton_multiComment";
	public final static String AUTOMATON_SINGLE_LINE_COMMENT = "__automaton_singleComment";
	public final static String AUTOMATON_STRING = "__automaton_String";
	public final static String AUTOMATON_KEYWORD = "__automaton_KEYWORD";
	
	private final static String[] keywords = {
		"STATE",
		"NONDET",
		"INITIAL",
		"LOCAL",
		"MATCH",
		"ASSERT",
		"ERROR",
		"STOP",
		"AUTOMATON",
		"DO",
		"GOTO",
		"CHECK",
		"MODIFY",
		"PRINT",
		";"
	};

	public AutomatonPartitionScanner() {

		List<IPredicateRule> rules = new ArrayList<IPredicateRule>();
		rules.add(new SingleLineRule("\"", "\"", new Token(AUTOMATON_STRING), '\\')); 
		Token keywordToken = new Token(AUTOMATON_KEYWORD);
		for (int i = 0; i < keywords.length; i++) {
			rules.add(new SingleLineRule(keywords[i], " ", keywordToken));
		}
		
		rules.add(new MultiLineRule("/*", "*/", new Token(AUTOMATON_MULTI_LINE_COMMENT)));
		rules.add(new EndOfLineRule("//", new Token(AUTOMATON_SINGLE_LINE_COMMENT)));

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
