package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class SpecificationDocumentProvider extends FileDocumentProvider {

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new MyFastPartitioner(
					new SpecificationPartitionScanner(),
					new String[] {
						SpecificationConfiguration.SPECIFICATION_COMMENT,
						SpecificationConfiguration.SPECIFICATION_INCLUDE,
						SpecificationConfiguration.AUTOMATON_KEYWORD,
						SpecificationConfiguration.AUTOMATON_STRING
						
						});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}

	public class MyFastPartitioner extends FastPartitioner {
	
		public MyFastPartitioner(IPartitionTokenScanner scanner,
				String[] legalContentTypes) {
			super(scanner, legalContentTypes);
		}
		public void connect(IDocument document, boolean delayInitialise)
		{
		    super.connect(document, delayInitialise);
		    printPartitions(document);
		}
	
		public void printPartitions(IDocument document)
		{
		    StringBuffer buffer = new StringBuffer();
	
		    ITypedRegion[] partitions = computePartitioning(0, document.getLength());
		    for (int i = 0; i < partitions.length; i++)
		    {
		        try
		        {
		            buffer.append("Partition type: " 
		              + partitions[i].getType() 
		              + ", offset: " + partitions[i].getOffset()
		              + ", length: " + partitions[i].getLength());
		            buffer.append("\n");
		            buffer.append("Text:\n");
		            buffer.append(document.get(partitions[i].getOffset(), 
		             partitions[i].getLength()));
		            buffer.append("\n---------------------------\n\n\n");
		        }
		        catch (BadLocationException e)
		        {
		            e.printStackTrace();
		        }
		    }
		    System.out.print(buffer);
		}
	
	}
}