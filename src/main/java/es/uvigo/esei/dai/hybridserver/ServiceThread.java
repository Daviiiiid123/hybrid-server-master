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
import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.dao.HTMLPageDAO;
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
        
        // Abrimos flujos de lectura/escritura ligados al socket del cliente.
        // Se usan dentro del try-with-resources para cerrarlos automáticamente.
        try (InputStreamReader reader = new InputStreamReader(socket.getInputStream(), "UTF-8");
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8")) {

            System.out.println("[ServiceThread] Iniciando procesamiento de petición");

            // Parsear la petición HTTP recibida desde el cliente
            HTTPRequest request = new HTTPRequest(reader);

            System.out.println(
                    "[ServiceThread] Petición parseada: " + request.getMethod() + " " + request.getResourceName());

            // Procesar la petición y obtener la respuesta correspondiente
            HTTPResponse response = processRequest(request);

            System.out.println("[ServiceThread] Respuesta generada: " + response.getStatus());
            System.out.println("[ServiceThread] Content-Type: " + response.getParameters().get("Content-Type"));
            System.out.println("[ServiceThread] Content-Length: " + response.getParameters().get("Content-Length"));
            System.out.println("[ServiceThread] Contenido : " +
                    (response.getContent() != null
                            ? response.getContent().substring(0, Math.min(100, response.getContent().length()))
                            : "null"));

            // Enviar la respuesta al cliente
            response.print(writer);
            writer.flush();

            System.out.println("[ServiceThread] Respuesta enviada correctamente");

        } catch (Exception e) {
            System.err.println("[ServiceThread] Error procesando petición: " + e.getMessage());
            e.printStackTrace();
            // Si ocurre cualquier excepción durante el procesamiento, intentamos
            // enviar una respuesta 500 al cliente para informar del error y continuar.
            try (OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8")) {
                HTTPResponse errorResponse = new HTTPResponse();
                errorResponse.setStatus(HTTPResponseStatus.S500);
                errorResponse.putParameter("Content-Type", "text/html");
                errorResponse.setContent("<html><body><h1>500 Internal Server Error</h1></body></html>");
                errorResponse.print(writer);
            } catch (IOException ioException) {
                System.err.println("[ServiceThread] Error enviando respuesta de error: " + ioException.getMessage());
                // Si no podemos enviar la respuesta de error, no podemos hacer más;
                // simplemente dejamos que el hilo termine. No lanzamos la excepción
                // para evitar detener el servidor.
            }
        } finally {
            // En cualquier caso cerramos el socket del cliente.
            try {
                socket.close();
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
                return servePage(uuid);
            } else {
                return createPageList();
            }
        } else {
           
            return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
        }
    }

    private HTTPResponse handlePost(HTTPRequest request) {
        String path = request.getResourceName();

        if ("html".equals(path) || "/html".equals(path)) {
            // -- Manejo de POST sobre /html --
            // Se espera que el cuerpo esté codificado como
            // application/x-www-form-urlencoded
            // y que tenga el parámetro 'html' con el contenido HTML a almacenar.
            
            String htmlParam = request.getResourceParameters().get("html");

            // Si falta el parámetro 'html' devolvemos 400 Bad Request
            if (htmlParam == null || htmlParam.isEmpty()) {
                return createErrorResponse(HTTPResponseStatus.S400, "Bad Request");
            }

            // Generar un UUID único para la nueva página
            String uuid = UUID.randomUUID().toString();

            // Guardar la página usando el DAO
            HTMLPageDAO pageDAO = server.getPageDAO();
            boolean saved = pageDAO.savePage(uuid, htmlParam);

            if (!saved) {
                return createErrorResponse(HTTPResponseStatus.S500, "Internal Server Error");
            }

            // Preparar la respuesta HTML informando del recurso creado y proporcionando
            // un enlace para acceder a él (/html?uuid=<uuid>)
            HTTPResponse response = new HTTPResponse();
            response.setStatus(HTTPResponseStatus.S200); // 200 OK 
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
        }

        return createErrorResponse(HTTPResponseStatus.S405, "Method Not Allowed");
    }

    private HTTPResponse handleDelete(HTTPRequest request) {
        String path = request.getResourceName();

        if ("html".equals(path) || "/html".equals(path)) {
            String uuid = request.getResourceParameters().get("uuid");

            System.out.println("[DELETE] UUID solicitado: " + uuid);

            if (uuid == null || uuid.isEmpty()) {
                return createErrorResponse(HTTPResponseStatus.S400, "Bad Request - UUID parameter required");
            }

            try {
                HTMLPageDAO pageDAO = server.getPageDAO();

                // verificar si la página existe
                boolean exists = pageDAO.pageExists(uuid);
                System.out.println("[DELETE] Página existe? " + exists);

                boolean deleted = pageDAO.deletePage(uuid);
                System.out.println("[DELETE] Página eliminada? " + deleted);

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
        } else {
            return createErrorResponse(HTTPResponseStatus.S404, "Not Found");
        }
    }

    private HTTPResponse createWelcomePage() {
        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html"); // CSS
                                                            // ---------------------------------------------------------------------
        String html = "<html>" +
                "<head><title>Pagina Principal - Hybrid Server</title></head>" +
                "<body>" +
                "<h1>Hybrid Server</h1>" +
                "<h2>Servidor HTTP en Java</h2>" +
                "<p>Bienvenido al servidor hibrido de paginas HTML</p>" +
                "<div>" +
                "<a href='/html'>Ver Lista de Paginas</a>" +
                "</div>" +
                "</body>" +
                "</html>";

        response.setContent(html);
        return response;
    }

    private HTTPResponse createPageList() {
        HTTPResponse response = new HTTPResponse();
        response.setStatus(HTTPResponseStatus.S200);
        response.putParameter("Content-Type", "text/html");

        StringBuilder html = new StringBuilder();

        html.append("<html>");
        html.append("<head><title>Lista de Paginas</title></head>");
        html.append("<body>");
        html.append("<h1>Lista de Paginas HTML Disponibles</h1>");
        html.append("<div>");
        html.append("<ul>");

        try {
            HTMLPageDAO pageDAO = server.getPageDAO();
            Map<String, String> pages = pageDAO.getAllPages();

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
        } catch (Exception e) {
            html.append("<li>Error accediendo a las paginas: ").append(e.getMessage()).append("</li>");
        }

        html.append("</ul>");
        html.append("</div>");
        html.append("<p><a href='/'>Volver al inicio</a></p>");
        html.append("</body>");
        html.append("</html>");

        response.setContent(html.toString());
        return response;
    }

    private HTTPResponse servePage(String uuid) {
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
