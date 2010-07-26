package stacks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import main.HierchyVisitor;

import org.objectweb.asm.ClassReader;

public final class Visit {
	public static void visit(FrameStackVisitor visitor, String file){
		if(file.endsWith(".jar"))
        	visitJar(visitor, file);
        else
        	Visit.visitFile(visitor, file);
	}
	
	public static void visitJar(FrameStackVisitor visitor, String file){
		try {
			ZipFile f;
			f = new ZipFile(file);
			Enumeration< ? extends ZipEntry> en = f.entries();
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				String name = e.getName();
				if (name.endsWith(".class")) {
					new ClassReader(f.getInputStream(e)).accept(visitor, 0);
				}
			}
		} catch (IOException e1) {
			System.err.println("Error: Failed to Read jar file "+file);
			System.exit(1);
		}
	}
	public static void visitFile(FrameStackVisitor visitor, String file) {
		File bin = new File(file);
		if(!bin.exists()) {
			System.err.println("file does not exist");
			return;
		}
		visitFile(visitor, bin);
	}

	private static void visitFile(FrameStackVisitor visitor, File file) {

		if(file.isHidden())
			return;
		if(file.isDirectory()) {
			File[] files = file.listFiles();
			for(int i = 0; i < files.length; i++) {
				visitFile(visitor, files[i]);
			}
		} else {
			String path = file.getAbsolutePath();
			if(path.endsWith(".class")) {
				ClassReader cr;
				try {
					cr = new ClassReader(new FileInputStream(path));
					cr.accept(new HierchyVisitor(), 0);
				} catch (FileNotFoundException e) {
					System.err.println("File not found");
				} catch (IOException e) {
					System.err.println("File not found");
				}
			}
		}
	}
}
