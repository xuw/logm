package tests;

import java.io.IOException;

public class SubSubSubTest211 extends SubSubTest21 {
	private SubSubTest11 fs;
	private int length;
	private SubSubTest11 IBUF;
	private int bytes;
	
	public void ls(Object src, boolean recursive, boolean printHeader ) throws IOException {
        Object items[] = fs.listPaths(src);
        if (items == null) {
            throw new IOException("Could not get listing for " + src);
        } else {
            if(!recursive && printHeader ) {
            	System.out.println("Found " + items.length + " items");
            }
            for (int i = 0; i < items.length; i++) {
                Object cur = items[i];
                System.out.println(cur + "\t" 
                                    + (fs.isDirectory(cur) ? 
                                        "<dir>" : 
                                        ("<r " + fs.getReplication(cur) 
                                            + ">\t" + fs.getLength(cur))));
                if(recursive && fs.isDirectory(cur)) {
                  ls(cur, recursive, printHeader);
                }
            }
        }
    }
	
	public String toString() {
		StringBuffer buffer = new StringBuffer(length);
		try {
			synchronized (IBUF) {
				IBUF.reset(bytes, length);
				readChars(IBUF, buffer, length);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return buffer.toString();
	}
	
	public void readChars(SubSubTest11 o, StringBuffer buffer, int length) {
		return;
	}
}
