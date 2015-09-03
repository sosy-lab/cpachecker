package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import org.eclipse.ui.editors.text.TextEditor;

public class SpecificationEditor extends TextEditor {

	private ColorManager colorManager;

	public SpecificationEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new SpecificationConfiguration(colorManager, this, getPreferenceStore()));
		setDocumentProvider(new SpecificationDocumentProvider());
	}
	
	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
}
