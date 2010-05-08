package org.sosy_lab.cpachecker.plugin.eclipse.editors.automatoneditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class AutomatonDocumentProvider extends FileDocumentProvider {

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new AutomatonPartitionScanner(),
					new String[] {
						AutomatonPartitionScanner.AUTOMATON_STATE_PARTITION,
						AutomatonPartitionScanner.AUTOMATON_SINGLE_LINE_COMMENT,
						AutomatonPartitionScanner.AUTOMATON_MULTI_LINE_COMMENT,
						AutomatonPartitionScanner.AUTOMATON_STRING});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
}