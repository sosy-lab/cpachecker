package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import org.eclipse.ui.editors.text.TextEditor;

public class AutomatonEditor extends TextEditor {

	private ColorManager colorManager;

	public AutomatonEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		setDocumentProvider(new AutomatonDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
