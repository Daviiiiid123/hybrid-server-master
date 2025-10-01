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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Launcher {
  public static void main(String[] args) {
    HybridServer server = null;
    
    try {
      if (args.length > 0) {
        // Si se proporciona un archivo de configuración, cargar las propiedades
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(args[0])) {
          properties.load(fis);
        }
        server = new HybridServer(properties);
        System.out.println("Servidor iniciado con configuración desde: " + args[0]);
      } else {
        // Crear algunas páginas HTML de ejemplo por defecto
        Map<String, String> pages = new HashMap<>();
        
        pages.put("page1", 
            "<html><head><title>Página 1</title></head>" +
            "<body><h1>Esta es la página 1</h1>" +
            "<p>Contenido de la primera página.</p>" +
            "<p><a href='/html'>Volver a la lista</a></p>" +
            "</body></html>");
            
        pages.put("page2", 
            "<html><head><title>Página 2</title></head>" +
            "<body><h1>Esta es la página 2</h1>" +
            "<p>Contenido de la segunda página.</p>" +
            "<p><a href='/html'>Volver a la lista</a></p>" +
            "</body></html>");
            
        pages.put("welcome", 
            "<html><head><title>Bienvenida</title></head>" +
            "<body><h1>¡Bienvenido al Hybrid Server!</h1>" +
            "<p>Esta es una página de bienvenida.</p>" +
            "<p><a href='/html'>Ver todas las páginas</a></p>" +
            "</body></html>");

        server = new HybridServer(pages);
        System.out.println("Servidor iniciado con páginas de ejemplo");
      }
      
      System.out.println("Hybrid Server iniciado en puerto " + server.getPort());
      System.out.println("URLs disponibles:");
      System.out.println("  - Página principal: http://localhost:" + server.getPort() + "/");
      System.out.println("  - Lista de páginas: http://localhost:" + server.getPort() + "/html");
      
      if (server.getPages() != null && !server.getPages().isEmpty()) {
        System.out.println("Páginas de ejemplo:");
        for (String uuid : server.getPages().keySet()) {
          System.out.println("  - http://localhost:" + server.getPort() + "/html?uuid=" + uuid);
        }
      }
      
      System.out.println("\nPresiona Enter para detener el servidor...");
      
      server.start();
      
      // Esperar input del usuario para detener el servidor
      try {
        System.in.read();
      } catch (IOException e) {
        System.err.println("Error leyendo entrada: " + e.getMessage());
      }
      
    } catch (IOException e) {
      System.err.println("Error al iniciar el servidor: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Error inesperado: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (server != null) {
        System.out.println("Deteniendo servidor...");
        server.close();
        System.out.println("Servidor detenido.");
      }
    }
  }
}
