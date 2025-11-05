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

import es.uvigo.esei.dai.hybridserver.dao.HTMLPageDAO;
import es.uvigo.esei.dai.hybridserver.dao.XMLDocumentDAO;
import es.uvigo.esei.dai.hybridserver.dao.XSDDocumentDAO;
import es.uvigo.esei.dai.hybridserver.dao.XSLTDocumentDAO;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

public class ServiceThread implements Runnable {
    // Socket asociado al cliente que atiende este hilo
    private final Socket socket;
    // Referencia al servidor principal para acceder a recursos compartidos (p.ej.
    // mapa de páginas)
    private final HybridServer server;

    public ServiceThread(Socket socket, HybridServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {

        HTTPResponse response;

        // Abrimos flujos de lectura/escritura ligados al socket del cliente.
        // Se usan dentro del try-with-resources para cerrarlos automáticamente.
        try (InputStreamReader reader = new InputStreamReader(socket.getInputStream(), "UTF-8");
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8")) {

            // Este try-catch interno es solo para la logica de la aplicacion
            try {
                System.out.println("[ServiceThread] Iniciando procesamiento de petición");

                // Parsear la petición HTTP recibida desde el cliente
                HTTPRequest request = new HTTPRequest(reader);

                System.out.println(
                        "[ServiceThread] Petición parseada: " + request.getMethod() + " " + request.getResourceName());

                // Procesar la petición y obtener la respuesta correspondiente
                // en casp de error salta al catch de abajo
                response = processRequest(request);

                System.out.println("[ServiceThread] Respuesta generada: " + response.getStatus());
                System.out.println("[ServiceThread] Content-Type: " + response.getParameters().get("Content-Type"));
                System.out.println("[ServiceThread] Content-Length: " + response.getParameters().get("Content-Length"));
                System.out.println("[ServiceThread] Contenido : " +
                        (response.getContent() != null
                                ? response.getContent().substring(0, Math.min(100, response.getContent().length()))
                                : "null"));


            } catch (Exception e) {
                // en caso de error en el try interno (parsing, BD , ...) se genera respuesta
                // 500
                System.err.println("[ServiceThread] Error procesando petición: " + e.getMessage());
                e.printStackTrace();
                // Si ocurre cualquier excepción durante el procesamiento, intentamos
                // enviar una respuesta 500 al cliente para informar del error y continuar.

                response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S500);
                response.putParameter("Content-Type", "text/html");
                response.setContent("<html><body><h1>500 Internal Server Error</h1></body></html>");

            }
            // se envia la respuesta (sea de confirmacion o de error)
            // el writer del try exterior sigue abierto aqui
           
            response.print(writer);
            writer.flush();
            System.out.println("[ServiceThread] Respuesta enviada correctamente");
            

        } catch (IOException ex) {
            // si esto falla es un error de red, no es un error propio del server
            System.err.println("[ServiceThread] Error fatal de E/S al escribir en el socket: " + ex.getMessage());
        } finally {
            // En cualquier caso cerramos el socket del cliente.
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignoramos errores al cerrar el socket para no afectar al resto del servidor.
            }
        }
    }

    private HTTPResponse processRequest(HTTPRequest request) {
        // Dispatch básico según el método HTTP (GET, POST, DELETE, ...)
        if (HTTPRequestMethod.GET.equals(request.getMethod())) {
            return handleGet(request);
        } else if (HTTPRequestMethod.POST.equals(request.getMethod())) {
            return handlePost(request);
        } else if (HTTPRequestMethod.DELETE.equals(request.getMethod())) {
            return handleDelete(request);
        } else {
            // Si el método no está soportado devolvemos 405 Method Not Allowed
            return createErrorResponse(HTTPResponseStatus.S405, "Method Not Allowed");
        }
    }

    private HTTPResponse handleGet(HTTPRequest request) {
        String path = request.getResourceName();

        if ("".equals(path) || "/".equals(path)) {
            return createWelcomePage();
        } else if ("html".equals(path) || "/html".equals(path)) {
            String uuid = request.getResourceParameters().get("uuid");
            if (uuid != null) {
                return serveHtmlPage(uuid);
            } else {
                return createHtmlPageList();
            }
        } else if ("xsd".equals(path) || "/xsd".equals(path)) {
            String uuid = request.getResourceParameters().get("uuid");
            if (uuid != null) {
                return serveXsdDocument(uuid);
            } else {
                return createXsdDocumentList();
            }
        } else if ("xml".equals(path) || "/xml".equals(path)) {
            String uuid = request.getResourceParameters().get("uuid");
            if (uuid != null) {
                return serveXmlDocument(uuid);
            } else {
                return createXmlDocumentList();
            }
        } else if ("xslt".equals(path) || "/xslt".equals(path)) {
            String uuid = request.getResourceParameters().get("uuid");
            if (uuid != null) {
                return serveXsltDocument(uuid);
            } else {
                return createXsltDocumentList();
            }
        } else {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }
    }

    private HTTPResponse handlePost(HTTPRequest request) {
        String path = request.getResourceName();

        if ("html".equals(path) || "/html".equals(path)) {
            return handlePostHtml(request);
        } else if ("xsd".equals(path) || "/xsd".equals(path)) {
            return handlePostXsd(request);
        } else if ("xml".equals(path) || "/xml".equals(path)) {
            return handlePostXml(request);
        } else if ("xslt".equals(path) || "/xslt".equals(path)) {
            return handlePostXslt(request);
        }

        return createErrorResponse(HTTPResponseStatus.S405, "Method Not Allowed");
    }
    
    private HTTPResponse handlePostHtml(HTTPRequest request) {
        // -- Manejo de POST sobre /html --
        String htmlParam = request.getResourceParameters().get("html");

        // Si falta el parámetro 'html' devolvemos 400 Bad Request
        if (htmlParam == null || htmlParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }

        // Generar un UUID único para la nueva página
        String uuid = UUID.randomUUID().toString();

        // Guardar la página usando el DAO
        HTMLPageDAO pageDAO = server.getPageDAO();

        try {
            boolean saved = pageDAO.savePage(uuid, htmlParam);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            // Preparar la respuesta HTML informando del recurso creado
            HTTPResponse response = new HTTPResponse();
            response.setStatus(HTTPResponseStatus.S200);
            response.putParameter("Content-Type", "text/html");

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>Page Created</title></head>");
            html.append("<body>");
            html.append("<h1>Page created</h1>");
            html.append("<p>New page id: <a href=\"html?uuid=").append(uuid).append("\">").append(uuid)
                    .append("</a></p>");
            html.append("<p><a href=\"/html\">Ver lista de páginas</a></p>");
            html.append("</body>");
            html.append("</html>");

            response.setContent(html.toString());
            return response;
        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error en el POST HTML: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handlePostXsd(HTTPRequest request) {
        String xsdParam = request.getResourceParameters().get("xsd");

        if (xsdParam == null || xsdParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }

        String uuid = UUID.randomUUID().toString();
        XSDDocumentDAO xsdDAO = server.getXsdDAO();

        try {
            boolean saved = xsdDAO.saveDocument(uuid, xsdParam);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            HTTPResponse response = new HTTPResponse();
            response.setStatus(HTTPResponseStatus.S200);
            response.putParameter("Content-Type", "text/html");

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>XSD Created</title></head>");
            html.append("<body>");
            html.append("<h1>XSD document created</h1>");
            html.append("<p>New XSD id: <a href=\"xsd?uuid=").append(uuid).append("\">").append(uuid)
                    .append("</a></p>");
            html.append("<p><a href=\"/xsd\">Ver lista de documentos XSD</a></p>");
            html.append("</body>");
            html.append("</html>");

            response.setContent(html.toString());
            return response;
        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error en el POST XSD: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handlePostXml(HTTPRequest request) {
        String xmlParam = request.getResourceParameters().get("xml");

        if (xmlParam == null || xmlParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }

        String uuid = UUID.randomUUID().toString();
        XMLDocumentDAO xmlDAO = server.getXmlDAO();

        try {
            boolean saved = xmlDAO.saveDocument(uuid, xmlParam);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            HTTPResponse response = new HTTPResponse();
            response.setStatus(HTTPResponseStatus.S200);
            response.putParameter("Content-Type", "text/html");

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>XML Created</title></head>");
            html.append("<body>");
            html.append("<h1>XML document created</h1>");
            html.append("<p>New XML id: <a href=\"xml?uuid=").append(uuid).append("\">").append(uuid)
                    .append("</a></p>");
            html.append("<p><a href=\"/xml\">Ver lista de documentos XML</a></p>");
            html.append("</body>");
            html.append("</html>");

            response.setContent(html.toString());
            return response;
        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error en el POST XML: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handlePostXslt(HTTPRequest request) {
        String xsltParam = request.getResourceParameters().get("xslt");
        String xsdParam = request.getResourceParameters().get("xsd");

        // Validar parámetro xslt
        if (xsltParam == null || xsltParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }

        // Validar parámetro xsd (obligatorio para XSLT)
        if (xsdParam == null || xsdParam.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }

        // Verificar que el XSD existe
        XSDDocumentDAO xsdDAO = server.getXsdDAO();
        try {
            boolean xsdExists = xsdDAO.documentExists(xsdParam);
            if (!xsdExists) {
                return createErrorResponse(HTTPResponseStatus.S404, "Not Found");
            }
        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error verificando XSD: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }

        String uuid = UUID.randomUUID().toString();
        XSLTDocumentDAO xsltDAO = server.getXsltDAO();

        try {
            boolean saved = xsltDAO.saveDocument(uuid, xsltParam, xsdParam);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            HTTPResponse response = new HTTPResponse();
            response.setStatus(HTTPResponseStatus.S200);
            response.putParameter("Content-Type", "text/html");

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>XSLT Created</title></head>");
            html.append("<body>");
            html.append("<h1>XSLT document created</h1>");
            html.append("<p>New XSLT id: <a href=\"xslt?uuid=").append(uuid).append("\">").append(uuid)
                    .append("</a></p>");
            html.append("<p>Associated XSD: <a href=\"xsd?uuid=").append(xsdParam).append("\">").append(xsdParam)
                    .append("</a></p>");
            html.append("<p><a href=\"/xslt\">Ver lista de documentos XSLT</a></p>");
            html.append("</body>");
            html.append("</html>");

            response.setContent(html.toString());
            return response;
        } catch (SQLException e) {
            System.err.println("[ServiceThread] Error en el POST XSLT: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }

    private HTTPResponse handleDelete(HTTPRequest request) {
        String path = request.getResourceName();

        if ("html".equals(path) || "/html".equals(path)) {
            return handleDeleteHtml(request);
        } else if ("xsd".equals(path) || "/xsd".equals(path)) {
            return handleDeleteXsd(request);
        } else if ("xml".equals(path) || "/xml".equals(path)) {
            return handleDeleteXml(request);
        } else if ("xslt".equals(path) || "/xslt".equals(path)) {
            return handleDeleteXslt(request);
        } else {
            return createErrorResponse(HTTPResponseStatus.S404, "Not Found");
        }
    }
    
    private HTTPResponse handleDeleteHtml(HTTPRequest request) {
        String uuid = request.getResourceParameters().get("uuid");

        if (uuid == null || uuid.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
        }

        try {
            HTMLPageDAO pageDAO = server.getPageDAO();
            boolean deleted = pageDAO.deletePage(uuid);

            if (deleted) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");

                String html = "<html>" +
                        "<head><title>Page Deleted</title></head>" +
                        "<body>" +
                        "<h1>Page Deleted</h1>" +
                        "<p>The page with UUID " + uuid + " has been successfully deleted.</p>" +
                        "<p><a href='/html'>Ver lista de páginas</a></p>" +
                        "<p><a href='/'>Volver al inicio</a></p>" +
                        "</body>" +
                        "</html>";

                response.setContent(html);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "Page Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error eliminando página " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handleDeleteXsd(HTTPRequest request) {
        String uuid = request.getResourceParameters().get("uuid");

        if (uuid == null || uuid.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
        }

        try {
            XSDDocumentDAO xsdDAO = server.getXsdDAO();
            boolean deleted = xsdDAO.deleteDocument(uuid);

            if (deleted) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");

                String html = "<html>" +
                        "<head><title>XSD Deleted</title></head>" +
                        "<body>" +
                        "<h1>XSD Deleted</h1>" +
                        "<p>The XSD document with UUID " + uuid + " has been successfully deleted.</p>" +
                        "<p><a href='/xsd'>Ver lista de documentos XSD</a></p>" +
                        "<p><a href='/'>Volver al inicio</a></p>" +
                        "</body>" +
                        "</html>";

                response.setContent(html);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XSD Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error eliminando XSD " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handleDeleteXml(HTTPRequest request) {
        String uuid = request.getResourceParameters().get("uuid");

        if (uuid == null || uuid.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
        }

        try {
            XMLDocumentDAO xmlDAO = server.getXmlDAO();
            boolean deleted = xmlDAO.deleteDocument(uuid);

            if (deleted) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");

                String html = "<html>" +
                        "<head><title>XML Deleted</title></head>" +
                        "<body>" +
                        "<h1>XML Deleted</h1>" +
                        "<p>The XML document with UUID " + uuid + " has been successfully deleted.</p>" +
                        "<p><a href='/xml'>Ver lista de documentos XML</a></p>" +
                        "<p><a href='/'>Volver al inicio</a></p>" +
                        "</body>" +
                        "</html>";

                response.setContent(html);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XML Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error eliminando XML " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse handleDeleteXslt(HTTPRequest request) {
        String uuid = request.getResourceParameters().get("uuid");

        if (uuid == null || uuid.isEmpty()) {
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
        }

        try {
            XSLTDocumentDAO xsltDAO = server.getXsltDAO();
            boolean deleted = xsltDAO.deleteDocument(uuid);

            if (deleted) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");

                String html = "<html>" +
                        "<head><title>XSLT Deleted</title></head>" +
                        "<body>" +
                        "<h1>XSLT Deleted</h1>" +
                        "<p>The XSLT document with UUID " + uuid + " has been successfully deleted.</p>" +
                        "<p><a href='/xslt'>Ver lista de documentos XSLT</a></p>" +
                        "<p><a href='/'>Volver al inicio</a></p>" +
                        "</body>" +
                        "</html>";

                response.setContent(html);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XSLT Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error eliminando XSLT " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
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
                "<p>Bienvenido al servidor hibrido de documentos estructurados</p>" +
                "<div>" +
                "<h3>Tipos de documentos:</h3>" +
                "<ul>" +
                "<li><a href='/html'>Páginas HTML</a></li>" +
                "<li><a href='/xsd'>Documentos XSD</a></li>" +
                "<li><a href='/xml'>Documentos XML</a></li>" +
                "<li><a href='/xslt'>Documentos XSLT</a></li>" +
                "</ul>" +
                "</div>" +
                "<hr>" +
                "<p>Autores: Alejandro M Calvar Blanco y David Fraga Rincon</p>" + 
                "</body>" +
                "</html>";

        response.setContent(html);
        return response;
    }

    private HTTPResponse createHtmlPageList() {
        // primero se gestiona la logica de la bd
        Map<String, String> pages;

        try {
            HTMLPageDAO pageDAO = server.getPageDAO();
            pages = pageDAO.getAllPages();
        } catch (Exception e) {
            System.err.println("[ServiceThread] Error obteniendo lista de paginas : " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error (Database Error)");
        }
        //si la consulta a la bdfue exitosa, sigue
        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();

        html.append("<html>");
        html.append("<head><title>Lista de Paginas HTML</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Paginas HTML Disponibles</h1>");
        html.append("<div>");
        html.append("<ul>");


        //gestion del mapa de paginas 
        if (pages != null && !pages.isEmpty()) {
            for (String uuid : pages.keySet()) {
                html.append("<li>");
                html.append("<a href='/html?uuid=").append(uuid).append("'>")
                        .append(uuid).append("</a>");
                html.append("</li>");
            }
        } else {
            html.append("<li>No hay paginas disponibles</li>");
        }

        html.append("</ul>");
        html.append("</div>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse serveHtmlPage(String uuid) {
        try {
            HTMLPageDAO pageDAO = server.getPageDAO();
            String content = pageDAO.getPage(uuid);

            if (content != null) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "text/html");
                response.setContent(content);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "Page Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error sirviendo página " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse createXsdDocumentList() {
        Map<String, String> documents;

        try {
            XSDDocumentDAO xsdDAO = server.getXsdDAO();
            documents = xsdDAO.getAllDocuments();
        } catch (Exception e) {
            System.err.println("[ServiceThread] Error obteniendo lista de XSD: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error (Database Error)");
        }

        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><title>Lista de Documentos XSD</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Documentos XSD Disponibles</h1>");
        html.append("<div>");
        html.append("<ul>");

        if (documents != null && !documents.isEmpty()) {
            for (String uuid : documents.keySet()) {
                html.append("<li>");
                html.append("<a href='/xsd?uuid=").append(uuid).append("'>")
                        .append(uuid).append("</a>");
                html.append("</li>");
            }
        } else {
            html.append("<li>No hay documentos XSD disponibles</li>");
        }

        html.append("</ul>");
        html.append("</div>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse serveXsdDocument(String uuid) {
        try {
            XSDDocumentDAO xsdDAO = server.getXsdDAO();
            String content = xsdDAO.getDocument(uuid);

            if (content != null) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "application/xml");
                response.setContent(content);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XSD Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error sirviendo XSD " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse createXmlDocumentList() {
        Map<String, String> documents;

        try {
            XMLDocumentDAO xmlDAO = server.getXmlDAO();
            documents = xmlDAO.getAllDocuments();
        } catch (Exception e) {
            System.err.println("[ServiceThread] Error obteniendo lista de XML: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error (Database Error)");
        }

        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><title>Lista de Documentos XML</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Documentos XML Disponibles</h1>");
        html.append("<div>");
        html.append("<ul>");

        if (documents != null && !documents.isEmpty()) {
            for (String uuid : documents.keySet()) {
                html.append("<li>");
                html.append("<a href='/xml?uuid=").append(uuid).append("'>")
                        .append(uuid).append("</a>");
                html.append("</li>");
            }
        } else {
            html.append("<li>No hay documentos XML disponibles</li>");
        }

        html.append("</ul>");
        html.append("</div>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse serveXmlDocument(String uuid) {
        try {
            XMLDocumentDAO xmlDAO = server.getXmlDAO();
            String content = xmlDAO.getDocument(uuid);

            if (content != null) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "application/xml");
                response.setContent(content);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XML Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error sirviendo XML " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
    }
    
    private HTTPResponse createXsltDocumentList() {
        Map<String, String> documents;

        try {
            XSLTDocumentDAO xsltDAO = server.getXsltDAO();
            documents = xsltDAO.getAllDocuments();
        } catch (Exception e) {
            System.err.println("[ServiceThread] Error obteniendo lista de XSLT: " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error (Database Error)");
        }

        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><title>Lista de Documentos XSLT</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Documentos XSLT Disponibles</h1>");
        html.append("<div>");
        html.append("<ul>");

        if (documents != null && !documents.isEmpty()) {
            for (String uuid : documents.keySet()) {
                html.append("<li>");
                html.append("<a href='/xslt?uuid=").append(uuid).append("'>")
                        .append(uuid).append("</a>");
                html.append("</li>");
            }
        } else {
            html.append("<li>No hay documentos XSLT disponibles</li>");
        }

        html.append("</ul>");
        html.append("</div>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse serveXsltDocument(String uuid) {
        try {
            XSLTDocumentDAO xsltDAO = server.getXsltDAO();
            String content = xsltDAO.getDocument(uuid);

            if (content != null) {
                HTTPResponse response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S200);
                response.putParameter("Content-Type", "application/xml");
                response.setContent(content);
                return response;
            } else {
                return createErrorResponse(HTTPResponseStatus.S404, "XSLT Not Found");
            }
        } catch (Exception e) {
            System.err.println("Error sirviendo XSLT " + uuid + ": " + e.getMessage());
            return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
        }
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
        return Integer.parseInt(statusName.substring(1)); // Remove 'S' prefix
    }
}
