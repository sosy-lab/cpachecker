package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class SpecificationConfiguration extends TextSourceViewerConfiguration {
	private SpecificationDoubleClickStrategy doubleClickStrategy;
	private TextEditor fEditor;
	private ColorManager colorManager;
	public final static String AUTOMATON_KEYWORD = "__automaton_KEYWORD";
	public final static String AUTOMATON_STRING = "__automaton_STRING";
	public final static String SPECIFICATION_INCLUDE = "__specification_INCLUDE";
	public final static String SPECIFICATION_COMMENT = "__specification_COMMENT";
	
	public SpecificationConfiguration(ColorManager colorManager, TextEditor pEditor, IPreferenceStore iPreferenceStore) {
		super(iPreferenceStore);
		this.colorManager = colorManager;
		fEditor = pEditor;
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			SPECIFICATION_COMMENT,
			AUTOMATON_STRING,
			AUTOMATON_KEYWORD,
			SPECIFICATION_INCLUDE
			};
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new SpecificationDoubleClickStrategy();
		return doubleClickStrategy;
	}
	@SuppressWarnings("unchecked") // this part of the API is not generic
	@Override
	protected Map<Object, Object> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		Map<Object, Object> targets= super.getHyperlinkDetectorTargets(sourceViewer);
		targets.put("org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor.SpecificationEditor", fEditor);
		return targets;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
	    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new StringScanner());
	    reconciler.setDamager(dr, AUTOMATON_STRING);
	    reconciler.setRepairer(dr, AUTOMATON_STRING);

	    dr = new DefaultDamagerRepairer(new KeywordScanner());
	    reconciler.setDamager(dr, AUTOMATON_KEYWORD);
	    reconciler.setRepairer(dr, AUTOMATON_KEYWORD);
	    
	    dr = new DefaultDamagerRepairer(new CommentScanner());
	    reconciler.setDamager(dr, SPECIFICATION_COMMENT);
	    reconciler.setRepairer(dr, SPECIFICATION_COMMENT);
	    
	    dr = new DefaultDamagerRepairer(new IncludeScanner());
	    reconciler.setDamager(dr, SPECIFICATION_INCLUDE);
	    reconciler.setRepairer(dr, SPECIFICATION_INCLUDE);
	    
	    /*dr = new DefaultDamagerRepairer(new XMLScanner());
	    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
	    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	    */

	    
	    return reconciler;
	}
	private class KeywordScanner extends RuleBasedScanner {
		private String[] keywords = {
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
			"PRINT"
		};
	    public KeywordScanner() {
	        TextAttribute textAttribute = new TextAttribute(colorManager.getColor(ColorManager.KEYWORD), null, SWT.BOLD);
	        IToken keywordToken = new Token(textAttribute);
	        IRule[] rules = new IRule[keywords.length];
			for (int i = 0; i < keywords.length; i++) {
				rules[i] = new SingleLineRule(keywords[i], " ", keywordToken);
			}
	        setRules(rules);
	    }
	}
	private class CommentScanner extends RuleBasedScanner {
	    public CommentScanner() {
	        TextAttribute textAttribute = new TextAttribute(colorManager.getColor(ColorManager.COMMENT));
	        IToken token = new Token(textAttribute);
	        IRule[] rules = new IRule[2];
	        rules[0] = new MultiLineRule("/*", "*/", token);
			rules[1] = new EndOfLineRule("//", token);
	        setRules(rules);
	    }
	}
	private class IncludeScanner extends RuleBasedScanner {
	    public IncludeScanner() {
	        TextAttribute textAttribute = new TextAttribute(colorManager.getColor(ColorManager.INCLUDE));
	        IToken token = new Token(textAttribute);
	        IRule[] rules = new IRule[1];
	        // Might use WordPatternRule to color #include and filename differently
	        // i think using the same color is good
	        rules[0] = new EndOfLineRule("#include ", token);
	        setRules(rules);
	    }
	}
	private class StringScanner extends RuleBasedScanner {
	    public StringScanner() {
	        TextAttribute textAttribute = new TextAttribute(colorManager.getColor(ColorManager.STRING));
	        IToken string = new Token(textAttribute);
	        IRule[] rules = new IRule[2];
	        // Add rule for double quotes
	        rules[0] = new SingleLineRule("\"", "\"", string, '\\');
	        // Add a rule for single quotes
	        rules[1] = new SingleLineRule("'", "'", string, '\\');
	        setRules(rules);
	    }
	}
}