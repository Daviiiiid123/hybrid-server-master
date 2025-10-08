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
package es.uvigo.esei.dai.hybridserver.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPResponse {
  private HTTPResponseStatus status;
  private String version;
  private String content;
  private final Map<String, String> parameters;
  
  public HTTPResponse() {
    this.status = HTTPResponseStatus.S200;
    this.version = "HTTP/1.1";
    this.content = "";
    this.parameters = new HashMap<>();
    
    // Default headers
    //this.parameters.put("Content-Type", "text/html; charset=UTF-8");
    //this.parameters.put("Server", "HybridServer/1.0");
  }

  public HTTPResponseStatus getStatus() {
    return status;
  }

  public void setStatus(HTTPResponseStatus status) {
    this.status = status;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content != null ? content : "";
    // Update Content-Length automatically
    putParameter("Content-Length", String.valueOf(this.content.getBytes().length));
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public String putParameter(String name, String value) {
    return parameters.put(name, value);
  }

  public boolean containsParameter(String name) {
    return parameters.containsKey(name);
  }

  public String removeParameter(String name) {
    return parameters.remove(name);
  }

  public void clearParameters() {
    parameters.clear();
  }

  public List<String> listParameters() {
    return new ArrayList<>(parameters.keySet());
  }

  public void print(Writer writer) throws IOException {
  PrintWriter printWriter = new PrintWriter(writer, false); // Disable auto-flush to prevent issues

  // Status line
  printWriter.print(version + " " + getStatusCode(status) + " " + getStatusMessage(status) + "\r\n");

  boolean contentEmpty = (content == null || content.isEmpty());

  // Sólo actualizar Content-Length si hay contenido
  if (!contentEmpty) {
    putParameter("Content-Length", String.valueOf(content.getBytes("UTF-8").length));
  }

  // Si no hay parámetros a imprimir (map vacío) -> línea en blanco y fin
  if (parameters.isEmpty()) {
    printWriter.print("\r\n");
    printWriter.flush();
    return;
  }

  // Imprimir cabeceras existentes (solo las que el objeto tiene)
  for (Map.Entry<String, String> param : parameters.entrySet()) {
    printWriter.print(param.getKey() + ": " + param.getValue() + "\r\n");
  }

  // Separador y posible cuerpo
  printWriter.print("\r\n");
  if (!contentEmpty) {
    printWriter.print(content);
    // Ensure we don't add extra newline
  }

  printWriter.flush();
}
  
  private int getStatusCode(HTTPResponseStatus status) {
    String statusName = status.name();
    return Integer.parseInt(statusName.substring(1)); // Remove 'S' prefix
  }
  
  private String getStatusMessage(HTTPResponseStatus status) {
    switch (status) {
      case S200: return "OK";
      case S201: return "Created";
      case S204: return "No Content";
      case S400: return "Bad Request";
      case S404: return "Not Found";
      case S405: return "Method Not Allowed";
      case S500: return "Internal Server Error";
      default: return "Unknown";
    }
  }

  @Override
  public String toString() {
    try (final StringWriter writer = new StringWriter()) {
      this.print(writer);

      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException("Unexpected I/O exception", e);
    }
  }
}
