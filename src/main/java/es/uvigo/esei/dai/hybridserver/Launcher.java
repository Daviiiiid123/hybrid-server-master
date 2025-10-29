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
       //  Si no hay argumentos, se usa la configuración por defecto.
       // Se llama al constructor vacío, que usa por defecto.
            System.out.println("Servidor iniciado con configuración por defecto ");
            server = new HybridServer();
       
      }
      
      System.out.println("Hybrid Server iniciado en puerto " + server.getPort());
      System.out.println("URLs disponibles:");
      System.out.println("  - Página principal: http://localhost:" + server.getPort() + "/");
      System.out.println("  - Lista de páginas: http://localhost:" + server.getPort() + "/html");
      
      //añadir ShutDown Hook
      //se ejecutara cuando la JVM se detenga (ej con Ctrl+C)


      //PREGUNTAR SI ESTA ES LA FORMA QUE QUIEREN Q USEMOS PARA CERRAR EL SERVIDOR
      final HybridServer finalServer = server; //// Necesario para usarlo en la lambda
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          System.out.println("\nDetectada señal de cierre. Deteniendo servidor ...");

          if(finalServer != null){
            finalServer.close();
            System.out.println("Servidor detenido");
          }
      }));
      
      server.start();
      
      
    } catch (IOException e) {
      System.err.println("Error al iniciar el servidor: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Error inesperado: " + e.getMessage());
      e.printStackTrace();
    } 
  }
}
