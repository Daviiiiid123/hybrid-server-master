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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
  private HTTPRequestMethod method;
  private String resourceChain;
  private String resourceName;
  private Map<String, String> resourceParameters;
  private String httpVersion;
  private Map<String, String> headerParameters;
  private String content;
  private int contentLength;


  public HTTPRequest(Reader reader) throws IOException, HTTPParseException {
    this.resourceParameters = new HashMap<>();
    this.headerParameters = new HashMap<>();
    this.contentLength = 0;
    this.content = "";
    
    try {
      parseRequest(reader);
    } catch (Exception e) {
      throw new HTTPParseException("Error parsing HTTP request: " + e.getMessage());
    }
  }
  
  private void parseRequest(Reader reader) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(reader);
    
    // Parse request line: METHOD /resource?params HTTP/1.1
    String requestLine = bufferedReader.readLine();
    if (requestLine == null || requestLine.trim().isEmpty()) {
      throw new IOException("Empty request line");
    }
    
    parseRequestLine(requestLine);
    
    // Parse headers
    parseHeaders(bufferedReader);
    
    // Read content if exists
    if (contentLength > 0) {
      parseContent(bufferedReader);
    }
  }
  
  private void parseRequestLine(String requestLine) throws IOException {
    String[] parts = requestLine.split(" ");
    if (parts.length < 3) {
      throw new IOException("Invalid request line: " + requestLine);
    }
    
    // Parse method
    try {
      this.method = HTTPRequestMethod.valueOf(parts[0].toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IOException("Unsupported HTTP method: " + parts[0]);
    }
    
    // Parse resource and parameters
    this.resourceChain = parts[1];
    parseResource(parts[1]);
    
    // Parse HTTP version
    this.httpVersion = parts[2];
  }
  
  private void parseResource(String resourceWithParams) throws IOException {
    if (resourceWithParams.contains("?")) {
      String[] resourceParts = resourceWithParams.split("\\?", 2);
      this.resourceName = resourceParts[0];
      parseParameters(resourceParts[1]);
    } else {
      this.resourceName = resourceWithParams;
    }
  }
  
  private void parseParameters(String paramString) throws IOException {
    if (paramString == null || paramString.isEmpty()) return;
    
    String[] pairs = paramString.split("&");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2) {
        try {
          String key = URLDecoder.decode(keyValue[0], "UTF-8");
          String value = URLDecoder.decode(keyValue[1], "UTF-8");
          resourceParameters.put(key, value);
        } catch (Exception e) {
          // Skip malformed parameters
        }
      }
    }
  }
  
  private void parseHeaders(BufferedReader reader) throws IOException {
    String line;
    while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
      int colonIndex = line.indexOf(':');
      if (colonIndex > 0) {
        String headerName = line.substring(0, colonIndex).trim();
        String headerValue = line.substring(colonIndex + 1).trim();
        headerParameters.put(headerName.toLowerCase(), headerValue);
        
        // Capture Content-Length
        if ("content-length".equals(headerName.toLowerCase())) {
          try {
            this.contentLength = Integer.parseInt(headerValue);
          } catch (NumberFormatException e) {
            this.contentLength = 0;
          }
        }
      }
    }
  }
  
  private void parseContent(BufferedReader reader) throws IOException {
    char[] buffer = new char[contentLength];
    int totalRead = 0;
    while (totalRead < contentLength) {
      int read = reader.read(buffer, totalRead, contentLength - totalRead);
      if (read == -1) break;
      totalRead += read;
    }
    this.content = new String(buffer, 0, totalRead);
    
    // For POST, parse parameters from body if form-encoded
    String contentType = headerParameters.get("content-type");
    if (HTTPRequestMethod.POST.equals(method) && contentType != null && 
        contentType.contains("application/x-www-form-urlencoded")) {
      try {
        parseParameters(this.content);
      } catch (IOException e) {
        // Ignore malformed content parameters
      }
    }
  }

  public HTTPRequestMethod getMethod() {
    return method;
  }

  public String getResourceChain() {
    return resourceChain;
  }

  public String[] getResourcePath() {
    if (resourceName == null || resourceName.equals("/")) {
      return new String[0];
    }
    String path = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    return path.split("/");
  }

  public String getResourceName() {
    return resourceName;
  }

  public Map<String, String> getResourceParameters() {
    return resourceParameters;
  }

  public String getHttpVersion() {
    return httpVersion;
  }

  public Map<String, String> getHeaderParameters() {
    return headerParameters;
  }

  public String getContent() {
    return content;
  }

  public int getContentLength() {
    return contentLength;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append(this.getMethod().name()).append(' ')
      .append(this.getResourceChain()).append(' ').append(this.getHttpVersion()).append("\r\n");

    for (Map.Entry<String, String> param : this.getHeaderParameters().entrySet()) {
      sb.append(param.getKey()).append(": ").append(param.getValue()).append("\r\n");
    }

    if (this.getContentLength() > 0) {
      sb.append("\r\n").append(this.getContent());
    }

    return sb.toString();
  }
}
