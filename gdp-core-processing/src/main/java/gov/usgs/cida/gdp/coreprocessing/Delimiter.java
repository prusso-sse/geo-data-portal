package gov.usgs.cida.gdp.coreprocessing;

/**
 *
 * @author tkunicki
 */
public enum Delimiter {

        COMMA(",", ".csv", "text/csv"),
        TAB("\t", ".tsv", "text/tab-separated-values"),
        SPACE(" ", ".txt", "text/plain");

        public final String delimiter;
        public final String extension;
        public final String mimeType;

        private Delimiter(String value, String extension, String mimeType) {
            this.delimiter = value;
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public static Delimiter getDefault() {
            return COMMA;
        }

}
