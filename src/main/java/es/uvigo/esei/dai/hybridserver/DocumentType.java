package es.uvigo.esei.dai.hybridserver;

public enum DocumentType {
        HTML("html", "text/html"),
        XML("xml", "application/xml"),
        XSD("xsd", "application/xml"),
        XSLT("xslt", "application/xslt+xml");

        private final String path;
        private final String contentType;

        DocumentType(String path, String contentType) {
            this.path = path;
            this.contentType = contentType;
        }

        public String getPath() {
            return path;
        }

        public String getContentType() {
            return contentType;
        }

        public static DocumentType fromPath(String path) {
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            for (DocumentType type : values()) {
                if (type.path.equals(cleanPath)) {
                    return type;
                }
            }
            return null;
        }
    } 