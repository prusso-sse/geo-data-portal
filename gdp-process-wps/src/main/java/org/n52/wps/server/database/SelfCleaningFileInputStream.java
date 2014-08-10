package org.n52.wps.server.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * After calling close on this InputStream, will attempt to delete the underlying file
 * 
 * @author isuftin
 */
public class SelfCleaningFileInputStream extends FileInputStream {

	private File file;

	public SelfCleaningFileInputStream(File file) throws FileNotFoundException {
		super(file);
	}

	@Override
	public void close() throws IOException {
		super.close();
		
		if (file.exists() && file.isFile() && file.canWrite()) {
			FileUtils.deleteQuietly(file);
		}
	}
}
