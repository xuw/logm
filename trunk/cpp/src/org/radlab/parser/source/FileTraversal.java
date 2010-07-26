package org.radlab.parser.source;
/**
 * A file traversal class which recursively find the file in a directly.
 * Referred from 
 * file://20071112204524.htm
 */
import java.io.File;
import java.io.IOException;

public class FileTraversal {
	public final void traverse( final File f ) throws IOException {
		
		if (f.isDirectory()) {
			onDirectory(f);
			final File[] childs = f.listFiles();
			for( File child : childs ) {				
				traverse(child);				
			}
			return;
		}
		onFile(f);
	}

	public void onDirectory( final File d ) {
	}

	public void onFile( final File f ) {		
	}
}
