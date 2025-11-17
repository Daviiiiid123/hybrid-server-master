/**
 *  HybridServer
 *  Copyright (C) 2025 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.dao.PageDAO;
import es.uvigo.esei.dai.hybridserver.dao.XMLPageDatabaseDAO;
import es.uvigo.esei.dai.hybridserver.dao.XSDPageDatabaseDAO;
import es.uvigo.esei.dai.hybridserver.dao.XSLPageDatabaseDAO;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

public class ServiceThread implements Runnable {
    private final Socket socket;
    private final HybridServer server;


    public ServiceThread
    (Socket socket, HybridServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        HTTPResponse response;

        try (InputStreamReader reader = new InputStreamReader(socket.getInputStream(), "UTF-8");
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8")) {

            try {
                System.out.println("[ServiceThread] Iniciando procesamiento de petición");

                HTTPRequest request = new HTTPRequest(reader);

                System.out.println(
                        "[ServiceThread] Petición parseada: " + request.getMethod() + " " + request.getResourceName());

                response = processRequest(request);

                System.out.println("[ServiceThread] Respuesta generada: " + response.getStatus());
                System.out.println("[ServiceThread] Content-Type: " + response.getParameters().get("Content-Type"));
                System.out.println("[ServiceThread] Content-Length: " + response.getParameters().get("Content-Length"));

            } catch (Exception e) {
                System.err.println("[ServiceThread] Error procesando petición: " + e.getMessage());
                e.printStackTrace();

                response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S500);
                response.putParameter("Content-Type", "text/html");
                response.setContent("<html><body><h1>500 Internal Server Error</h1></body></html>");
            }

            response.print(writer);
            writer.flush();
            System.out.println("[ServiceThread] Respuesta enviada correctamente");

        } catch (IOException ex) {
            System.err.println("[ServiceThread] Error fatal de E/S al escribir en el socket: " + ex.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignoramos errores al cerrar
            }
        }
    }

    private HTTPResponse processRequest(HTTPRequest request) {
        if (HTTPRequestMethod.GET.equals(request.getMethod())) {
            return handleGet(request);
        } else if (HTTPRequestMethod.POST.equals(request.getMethod())) {
            return handlePost(request);
        } else if (HTTPRequestMethod.DELETE.equals(request.getMethod())) {
            return handleDelete(request);
        } else {
            return createErrorResponse(HTTPResponseStatus.S405, "Method Not Allowed");
        }
    }

    private HTTPResponse handleGet(HTTPRequest request) {
        String path = request.getResourceName();

        if ("".equals(path) || "/".equals(path)) {
            return createWelcomePage();
        }

        DocumentType docType = DocumentType.fromPath(path);
        if (docType != null) {
            String uuid = request.getResourceParameters().get("uuid");
            if (uuid != null) {
                return serveDocument(uuid, docType);
            } else {
                return createDocumentList(docType);
            }
        }

        return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
    }

    private HTTPResponse handlePost(HTTPRequest request) {
        String path = request.getResourceName();
        DocumentType docType = DocumentType.fromPath(path);

        if (docType != null) {
            return createDocument(request, docType);
        }

        return createErrorResponse(HTTPResponseStatus.S405, "Method Not Allowed");
    }

    private HTTPResponse handleDelete(HTTPRequest request) {
        String path = request.getResourceName();
        DocumentType docType = DocumentType.fromPath(path);

        if (docType != null) {
            String uuid = request.getResourceParameters().get("uuid");

            System.out.println("[DELETE] UUID solicitado: " + uuid);

            if (uuid == null || uuid.isEmpty()) {
                return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
            }

            return deleteDocument(uuid, docType);
        }

        return createErrorResponse(HTTPResponseStatus.S404, "Not Found");
    }

    // Método genérico para crear documentos
    private HTTPResponse createDocument(HTTPRequest request, DocumentType docType) {
        String contentParam = request.getResourceParameters().get(docType.getPath());

        if (contentParam == null || contentParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - Missing content parameter");
        }

        // Validación especial para XSLT
        if (docType == DocumentType.XSLT) {
            String xsdUuid = request.getResourceParameters().get("xsd");
            
            if (xsdUuid == null || xsdUuid.isEmpty()) {
                return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - Missing xsd parameter");
            }

            // Verificar que el XSD existe
            try {
                XSDPageDatabaseDAO xsdDAO = server.getXsdDAO();
                if (!xsdDAO.pageExists(xsdUuid)) {
                    return createErrorResponse(HTTPResponseStatus.S404, "XSD Not Found");
                }
            } catch (SQLException e) {
                System.err.println("[ServiceThread] Error verificando XSD: " + e.getMessage());
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            // Guardar XSLT con su XSD asociado
            return createXSLTDocument(contentParam, xsdUuid);
        }

        // Para HTML, XML, XSD
        String uuid = UUID.randomUUID().toString();

        try {
            boolean saved = saveDocument(uuid, contentParam, docType);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            return createSuccessResponse(uuid, docType);

        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error en el POST: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }

    private HTTPResponse createXSLTDocument(String content, String xsdUuid) {
        String uuid = UUID.randomUUID().toString();

        try {
            XSLPageDatabaseDAO xsltDAO = server.getXslDAO();
            boolean saved = xsltDAO.savePageXSL(uuid, content, xsdUuid);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            return createSuccessResponse(uuid, DocumentType.XSLT);

        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error guardando XSLT: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }

    private boolean saveDocument(String uuid, String content, DocumentType docType) throws SQLException {
        switch (docType) {
            case HTML:
                return server.getHtmlDAO().savePage(uuid, content);
            case XML:
                return server.getXmlDAO().savePage(uuid, content);
            case XSD:
                return server.getXsdDAO().savePage(uuid, content);
            default:
                return false;
        }
    }

    // Método genérico para servir documentos
    private HTTPResponse serveDocument(String uuid, DocumentType docType) {
        try {
            String content = getDocumentContent(uuid, docType);

            if (content != null) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", docType.getContentType());
                response.setContent(content);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, 
                    docType.name() + " Document Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error sirviendo documento " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }

    private String getDocumentContent(String uuid, DocumentType docType) throws SQLException {
        switch (docType) {
            case HTML:
                return server.getHtmlDAO().getPage(uuid);
            case XML:
                return server.getXmlDAO().getPage(uuid);
            case XSD:
                return server.getXsdDAO().getPage(uuid);
            case XSLT:
                return server.getXslDAO().getPage(uuid); 
            default:
                return null;
        }
    }

    // Método genérico para eliminar documentos
    private HTTPResponse deleteDocument(String uuid, DocumentType docType) {
        try {
            boolean deleted = deleteDocumentByType(uuid, docType);
            System.out.println("[DELETE] Documento eliminado? " + deleted);

            if (deleted) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");

                String html = "<html>" +
                        "<head><title>Document Deleted</title></head>" +
                        "<body>" +
                        "<h1>Document Deleted</h1>" +
                        "<p>The " + docType.name() + " document with UUID " + uuid + 
                        " has been successfully deleted.</p>" +
                        "<p><a href='/" + docType.getPath() + "'>Ver lista de documentos</a></p>" +
                        "<p><a href='/'>Volver al inicio</a></p>" +
                        "</body>" +
                        "</html>";

                response.setContent(html);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "Document Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error eliminando documento " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }

    private boolean deleteDocumentByType(String uuid, DocumentType docType) throws SQLException {
        switch (docType) {
            case HTML:
                return server.getHtmlDAO().deletePage(uuid);
            case XML:
                return server.getXmlDAO().deletePage(uuid);
            case XSD:
                return server.getXsdDAO().deletePage(uuid);
            case XSLT:
                return server.getXslDAO().deletePage(uuid);
            default:
                return false;
        }
    }

    private HTTPResponse createWelcomePage() {
        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        String html = "<html>" +
                "<head><title>Pagina Principal - Hybrid Server</title></head>" +
                "<body>" +
                "<h1>Hybrid Server</h1>" +
                "<h2>Servidor HTTP en Java</h2>" +
                "<p>Bienvenido al servidor hibrido de documentos</p>" +
                "<div>" +
                "<h3>Tipos de documentos disponibles:</h3>" +
                "<ul>" +
                "<li><a href='/html'>Páginas HTML</a></li>" +
                "<li><a href='/xml'>Documentos XML</a></li>" +
                "<li><a href='/xsd'>Esquemas XSD</a></li>" +
                "<li><a href='/xslt'>Transformaciones XSLT</a></li>" +
                "</ul>" +
                "</div>" +
                "<hr>" +
                "<p>Autores:</p>" +
                "<p>Alejandro M Calvar Blanco</p>" +
                "<p>David Fraga Rincon</p>" +
                "</body>" +
                "</html>";

        response.setContent(html);
        return response;
    }

    private HTTPResponse createDocumentList(DocumentType docType) {
        Map<String, String> documents;

        try {
            documents = getDocumentsByType(docType);
        } catch (Exception e) {
            System.err.println("[ServiceThread] Error obteniendo lista de documentos: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error (Database Error)");
        }

        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><title>Lista de Documentos ").append(docType.name()).append("</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Documentos ").append(docType.name()).append("</h1>");
        html.append("<ul>");

        if (documents != null && !documents.isEmpty()) {
            for (String uuid : documents.keySet()) {
                html.append("<li>");
                html.append("<a href='/").append(docType.getPath()).append("?uuid=")
                    .append(uuid).append("'>").append(uuid).append("</a>");
                html.append("</li>");
            }
        } else {
            html.append("<li>No hay documentos disponibles</li>");
        }

        html.append("</ul>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private Map<String, String> getDocumentsByType(DocumentType docType) throws SQLException {
        switch (docType) {
            case HTML:
                return server.getHtmlDAO().getAllPages();
            case XML:
                return server.getXmlDAO().getAllPages();
            case XSD:
                return server.getXsdDAO().getAllPages();
            case XSLT:
                return server.getXslDAO().getAllPages();
            default:
                return null;
        }
    }

    private HTTPResponse createSuccessResponse(String uuid, DocumentType docType) {
        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><title>Document Created</title></head>");
        html.append("<body>");
        html.append("<h1>").append(docType.name()).append(" Document Created</h1>");
        html.append("<p>New document id: <a href=\"/").append(docType.getPath())
            .append("?uuid=").append(uuid).append("\">").append(uuid).append("</a></p>");
        html.append("<p><a href=\"/").append(docType.getPath())
            .append("\">Ver lista de documentos</a></p>");
        html.append("<p><a href=\"/\">Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse createErrorResponse(HTTPResponseStatus status, String message) {
        HTTPResponse response = new HTTPResponse();
        response.setStatus(status);
        response.putParameter("Content-Type", "text/html");

        String html = "<html>" +
                "<head><title>" + getStatusCode(status) + " " + message + "</title></head>" +
                "<body>" +
                "<h1>" + getStatusCode(status) + " " + message + "</h1>" +
                "<p><a href='/'>Volver al inicio</a></p>" +
                "</body>" +
                "</html>";

        response.setContent(html);
        return response;
    }

    private int getStatusCode(HTTPResponseStatus status) {
        String statusName = status.name();
        return Integer.parseInt(statusName.substring(1));
    }
}