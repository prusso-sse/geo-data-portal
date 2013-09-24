package org.n52.wps.commons;

/**
 * I'm overriding the 52 North class so we can provide .txt and .tsv files.
 * This may make sense in 52 North package, but I'm not sure anyone does csv type outputs (JIW)
 * @author tkunicki
 */
public class MIMEUtil {

    public static String getSuffixFromMIMEType(String mimeType) {
        String[] mimeTypeSplit = mimeType.split("/");
        String suffix =  mimeTypeSplit[mimeTypeSplit.length - 1];
        if ("geotiff".equalsIgnoreCase(suffix) || "x-geotiff".equalsIgnoreCase(suffix)) {
            suffix = "tiff";
        } else if ("netcdf".equalsIgnoreCase(suffix) || "x-netcdf".equalsIgnoreCase(suffix)) {
            suffix = "nc";
        } else if ("text/plain".equalsIgnoreCase(mimeType)) {
            suffix = "txt";
        } else if ("text/tab-separated-values".equalsIgnoreCase(mimeType)) {
            suffix = "tsv"; // could also do .tab
        }
        return suffix;
    }
}
