package gov.usgs.cida.gdp.io.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.GenericFileDataConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the GenericFileData class so we can unzip files with arbitrary directory depths.
 *
 * @author isuftin
 */
public class ZippedGenericFileData extends GenericFileData {

    private static Logger LOGGER = LoggerFactory.getLogger(ZippedGenericFileData.class);

    public ZippedGenericFileData(InputStream stream, String mimeType) {
        super(stream, mimeType);
    }

    @Override
    public String writeData(File workspaceDir) {
        String fileName = null;
        if (GenericFileDataConstants.getIncludeFilesByMimeType(this.mimeType) != null) {
            try {
                fileName = unzipData(this.dataStream, this.fileExtension, workspaceDir);
            } catch (IOException e) {
                LOGGER.error("Could not unzip the archive to " + workspaceDir, e);
            }
        } else {
            try {
                fileName = justWriteData(this.dataStream, this.fileExtension, workspaceDir);
            } catch (IOException e) {
                LOGGER.error("Could not write the input to " + workspaceDir, e);
            }
        }
        return fileName;
    }

    private String justWriteData(InputStream is, String extension, File writeDirectory) throws IOException {
        int bufferLength = 2048;
        byte buffer[] = new byte[bufferLength];
        String fileName = null;
        String baseFileName = Long.toString(System.currentTimeMillis());

        fileName = baseFileName + "." + extension;
        File currentFile = new File(writeDirectory, fileName);
        currentFile.createNewFile();

        fileName = currentFile.getAbsolutePath();

        FileOutputStream fos = new FileOutputStream(currentFile);
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(fos, bufferLength);

            int cnt;
            while ((cnt = is.read(buffer, 0, bufferLength)) != -1) {
                bos.write(buffer, 0, cnt);
            }
        } finally {
            if (bos != null) {
                IOUtils.closeQuietly(bos);
            }
        }
        return fileName;
    }

    private String unzipData(InputStream is, String extension, File writeDirectory) throws IOException {
        int bufferLength = 2048;
        byte buffer[] = new byte[bufferLength];
        String baseFileName = Long.toString(System.currentTimeMillis());

        ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(is));
        ZipEntry entry;

        String returnFile = null;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            String currentExtension = entry.getName();
            // We want to skip past directories and metadata files (MACOSX ZIPPING FIX)
            if (!currentExtension.endsWith(File.separator)
                    && !currentExtension.startsWith(".")
                    && !currentExtension.contains(File.separator + ".")) {
                int beginIndex = currentExtension.lastIndexOf(".") + 1;
                currentExtension = currentExtension.substring(beginIndex);

                String fileName = (baseFileName + "." + currentExtension).replace(" ", "_");
                File currentFile = new File(writeDirectory, fileName);

                currentFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(currentFile);
                BufferedOutputStream bos = null;
                try {
                    bos = new BufferedOutputStream(fos, bufferLength);
                    int cnt;
                    while ((cnt = zipInputStream.read(buffer, 0, bufferLength)) != -1) {
                        bos.write(buffer, 0, cnt);
                    }
                } finally {
                    if (bos != null) {
                        IOUtils.closeQuietly(bos);
                    }
                }
                if (currentExtension.equalsIgnoreCase(extension)) {
                    returnFile = currentFile.getAbsolutePath();
                }
            }
        }
        zipInputStream.close();
        return returnFile;
    }
}
