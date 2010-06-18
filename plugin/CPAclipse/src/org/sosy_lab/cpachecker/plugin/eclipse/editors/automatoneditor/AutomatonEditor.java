package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import org.eclipse.ui.editors.text.TextEditor;

public class AutomatonEditor extends TextEditor {

	private ColorManager colorManager;

	public AutomatonEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new AutomatonConfiguration(colorManager, this, getPreferenceStore()));
		setDocumentProvider(new AutomatonDocumentProvider());
	}
	
	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
}
