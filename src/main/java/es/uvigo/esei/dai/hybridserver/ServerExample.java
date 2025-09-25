/**
 *  HybridServer - Ejemplo de uso
 *  Copyright (C) 2025
 */
package es.uvigo.esei.dai.hybridserver;

import java.util.HashMap;
import java.util.Map;

public class ServerExample {
    public static void main(String[] args) {
        // Crear algunas páginas HTML de ejemplo
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
            "<body><h1>¡Bienvenido!</h1>" +
            "<p>Esta es una página de bienvenida personalizada.</p>" +
            "<p><a href='/html'>Ver todas las páginas</a></p>" +
            "</body></html>");

        // Crear y iniciar el servidor con las páginas en memoria
        HybridServer server = new HybridServer(pages);
        
        System.out.println("Iniciando Hybrid Server en puerto " + server.getPort());
        System.out.println("Páginas disponibles:");
        for (String uuid : pages.keySet()) {
            System.out.println("  - http://localhost:" + server.getPort() + "/html?uuid=" + uuid);
        }
        System.out.println("Página principal: http://localhost:" + server.getPort() + "/");
        System.out.println("Lista de páginas: http://localhost:" + server.getPort() + "/html");
        System.out.println("\nPresiona Enter para detener el servidor...");
        
        server.start();
        
        try {
            System.in.read(); // Esperar input del usuario
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Deteniendo servidor...");
        server.close();
        System.out.println("Servidor detenido.");
    }
}