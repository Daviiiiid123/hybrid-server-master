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
    
    // Validar parámetros de entrada
    if (args.length > 1) {
      System.err.println("Error: Demasiados parámetros.");
      System.err.println("Uso: java es.uvigo.esei.dai.hybridserver.Launcher [archivo_configuracion]");
      System.exit(1); 
    }
    
    try {
      if (args.length == 1) {
        // Si se proporciona un archivo de configuración, cargar las propiedades
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(args[0])) {
          properties.load(fis);
        }
        server = new HybridServer(properties);
        System.out.println("Servidor iniciado con BASE DE DATOS configuración desde: " + args[0]);
      } else {
        // Crear algunas páginas HTML de ejemplo por defecto
        System.out.println("Paginas de memoria");
        Map<String, String> pages = new HashMap<>();
        
        pages.put("page1", 
            "<html><head><title>Pagina 1 - Gatos</title></head>" +
           // "<body style='background-color: #ffe4e1; font-family: Arial;'>" +
           // "<h1 style='color: #ff6347;'>Pagina de los Gatos</h1>" +
            "<p>Los gatos son animales adorables y misteriosos.</p>" +
            "<p>Caracteristicas de los gatos:</p>" +
            "<ul>" +
            "<li>Independientes</li>" +
            "<li>Carinosos</li>" +
            "<li>Curiosos</li>" +
            "</ul>" +
            "<p><a href='/html'>Volver a la lista</a></p>" +
            "</body></html>");
            
        pages.put("page2", 
            "<html><head><title>Pagina 2 - Programacion</title></head>" +
            //"<body style='background-color: #e6f3ff; font-family: Courier;'>" +
           // "<h1 style='color: #0066cc;'>Mundo de la Programacion</h1>" +
            "<p>La programacion es el arte de resolver problemas con codigo.</p>" +
            "<p>Lenguajes populares:</p>" +
            "<ol>" +
            "<li>Python</li>" +
            "<li>Java</li>" +
            "<li>JavaScript</li>" +
            "</ol>" +
            "<p><a href='/html'>Volver a la lista</a></p>" +
            "</body></html>");
            
        pages.put("welcome", 
            "<html><head><title>Bienvenida - Hybrid Server</title></head>" +
           // "<body style='background-color: #f0fff0; font-family: Verdana;'>" +
           // "<h1 style='color: #228b22;'>Bienvenido al Hybrid Server!</h1>" +
            "<p>Este es un servidor HTTP desarrollado en Java.</p>" +
            "<p>Explora nuestras paginas:</p>" +
            "<ul>" +
            "<li><a href='/html?uuid=page1'>Pagina de los Gatos</a></li>" +
            "<li><a href='/html?uuid=page2'>Mundo de la Programacion</a></li>" +
            "</ul>" +
            "<p><a href='/html'>Ver todas las paginas</a></p>" +
            "</body></html>");

        server = new HybridServer(pages);
        System.out.println("Servidor iniciado con páginas de ejemplo");
      }
      
      System.out.println("Hybrid Server iniciado en puerto " + server.getPort());
      System.out.println("URLs disponibles:");
      System.out.println("  - Página principal: http://localhost:" + server.getPort() + "/");
      System.out.println("  - Lista de páginas: http://localhost:" + server.getPort() + "/html");
      
      try {
        Map<String, String> pages = server.getPageDAO().getAllPages();
        if (pages != null && !pages.isEmpty()) {
          System.out.println("Páginas de ejemplo:");
          for (String uuid : pages.keySet()) {
            System.out.println("  - http://localhost:" + server.getPort() + "/html?uuid=" + uuid);
          }
        }
      } catch (Exception e) {
        System.out.println("No se pudieron listar las páginas: " + e.getMessage());
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
