package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.*;

public class AutomatonPartitionScanner extends RuleBasedPartitionScanner {
	public final static String AUTOMATON_MULTI_LINE_COMMENT = "__automaton_multiComment";
	public final static String AUTOMATON_SINGLE_LINE_COMMENT = "__automaton_singleComment";
	public final static String AUTOMATON_STATE_PARTITION = "__automaton_State";
	public final static String AUTOMATON_STRING = "__automaton_String";

	public AutomatonPartitionScanner() {

		List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

		rules.add(new MultiLineRule("/*", "*/", new Token(AUTOMATON_MULTI_LINE_COMMENT)));
		rules.add(new SingleLineRule("\"", "\"", new Token(AUTOMATON_STRING), '\\')); 
		rules.add(new EndOfLineRule("//", new Token(AUTOMATON_SINGLE_LINE_COMMENT)));
		rules.add(new StatePartitionRule(new Token(AUTOMATON_STATE_PARTITION)));

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
