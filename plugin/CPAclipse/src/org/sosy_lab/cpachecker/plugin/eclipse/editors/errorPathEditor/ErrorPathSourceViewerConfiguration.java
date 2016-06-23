package org.sosy_lab.cpachecker.plugin.eclipse.editors.errorPathEditor;

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class ErrorPathSourceViewerConfiguration extends TextSourceViewerConfiguration {
	private final ErrorPathEditor fEditor;
	
	public ErrorPathSourceViewerConfiguration(ErrorPathEditor editor, IPreferenceStore prefStore) {
		super(prefStore);
		this.fEditor = editor;
	}
	
	@SuppressWarnings("unchecked") // this part of the API is not generic
	@Override
	protected Map<Object, Object> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		Map<Object, Object> targets= super.getHyperlinkDetectorTargets(sourceViewer);
		targets.put("org.sosy_lab.cpachecker.plugin.eclipse.editors.errorPathEditor.ErrorPathEditor", fEditor);
		return targets;
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
				ErrorPathEditor.ERROR_PATH_LINE_PARTITION
			};
	}
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
/*		DefaultDamagerRepairer ddr = new DefaultDamagerRepairer(new AutomatonPartitionScanner());
		reconciler.setDamager(ddr, AutomatonPartitionScanner.AUTOMATON_STRING);
		reconciler.setRepairer(ddr, AutomatonPartitionScanner.AUTOMATON_STRING);
*/
		
		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(new TextAttribute(fEditor.LINE_COLOR));
		
		reconciler.setDamager(ndr, ErrorPathEditor.ERROR_PATH_LINE_PARTITION);
		reconciler.setRepairer(ndr, ErrorPathEditor.ERROR_PATH_LINE_PARTITION);
		
		return reconciler;
	}
}
