package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class AutomatonConfiguration extends SourceViewerConfiguration {
	private AutomatonDoubleClickStrategy doubleClickStrategy;
	private ColorManager colorManager;

	public AutomatonConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			AutomatonPartitionScanner.AUTOMATON_SINGLE_LINE_COMMENT,
			AutomatonPartitionScanner.AUTOMATON_MULTI_LINE_COMMENT,
			AutomatonPartitionScanner.AUTOMATON_STRING,
			AutomatonPartitionScanner.AUTOMATON_KEYWORD}
		;
	}
	
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new AutomatonDoubleClickStrategy();
		return doubleClickStrategy;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
/*		DefaultDamagerRepairer ddr = new DefaultDamagerRepairer(new AutomatonPartitionScanner());
		reconciler.setDamager(ddr, AutomatonPartitionScanner.AUTOMATON_STRING);
		reconciler.setRepairer(ddr, AutomatonPartitionScanner.AUTOMATON_STRING);
*/
		
		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COMMENT)));
		reconciler.setDamager(ndr, AutomatonPartitionScanner.AUTOMATON_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(ndr, AutomatonPartitionScanner.AUTOMATON_SINGLE_LINE_COMMENT);
		
		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.KEYWORD)));
		reconciler.setDamager(ndr, AutomatonPartitionScanner.AUTOMATON_KEYWORD);
		reconciler.setRepairer(ndr, AutomatonPartitionScanner.AUTOMATON_KEYWORD);
		
		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.STRING)));
		reconciler.setDamager(ndr, AutomatonPartitionScanner.AUTOMATON_STRING);
		reconciler.setRepairer(ndr, AutomatonPartitionScanner.AUTOMATON_STRING);
		
		return reconciler;
	}
}