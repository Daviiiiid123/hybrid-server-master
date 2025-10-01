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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HybridServer implements AutoCloseable {
  private static final int SERVICE_PORT = 8888;
  private Thread serverThread;
  private boolean stop;
  //Nuevos atributos para A1
  private ExecutorService threadPool;
  private int numClients;
  private Map<String, String> pages; //Lista de páginas en memoria
  private Properties config; // Configuraciones del servidor
  private int port;
  //Manejo base de datos
  private String dbUrl;
  private String dbUser;
  private String dbPassword;

  public HybridServer() {
    // Inicializar con los parámetros por defecto
    this.numClients = 50;
    this.port = SERVICE_PORT;
    this.pages = new HashMap<>();
    this.config = null;
    this.threadPool = Executors.newFixedThreadPool(numClients);
  }

  public HybridServer(Map<String, String> pages) {
    // Inicializar con la base de datos en memoria conteniendo "pages"
    this.numClients = 50;
    this.port = SERVICE_PORT;
    this.pages = pages != null ? pages : new HashMap<>();
    this.config = null;
    this.threadPool = Executors.newFixedThreadPool(numClients);
  }

  public HybridServer(Properties properties) {
    // Inicializar con los parámetros recibidos
    this.config = properties;
    this.numClients = Integer.parseInt(properties.getProperty("numClients", "50"));
    this.port = Integer.parseInt(properties.getProperty("port", String.valueOf(SERVICE_PORT)));
    this.pages = new HashMap<>();
    this.threadPool = Executors.newFixedThreadPool(numClients);
    
    // Database properties (for future use)
    this.dbUrl = properties.getProperty("db.url");
    this.dbUser = properties.getProperty("db.user");
    this.dbPassword = properties.getProperty("db.password");
  }

  public int getPort() {
    return port;
  }
  
  // Getters for ServiceThread
  public Map<String, String> getPages() {
    return pages;
  }
  
  public Properties getConfig() {
    return config;
  }

  public void start() {
    this.serverThread = new Thread() {
      @Override
      public void run() {
        try (final ServerSocket serverSocket = new ServerSocket(getPort())) {
          System.out.println("ServerSocket creado y escuchando en puerto " + getPort());
          
          while (!stop) {
            try {
              Socket socket = serverSocket.accept();
              if (stop) {
                socket.close();
                break;
              }

              System.out.println("Nueva conexión aceptada desde: " + socket.getRemoteSocketAddress());
              // Usar pool de hilos para manejar cada cliente
              threadPool.submit(new ServiceThread(socket, HybridServer.this));
            } catch (IOException e) {
              if (!stop) {
                System.err.println("Error aceptando conexión: " + e.getMessage());
              }
            }
          }
          System.out.println("Servidor detenido correctamente");
        } catch (IOException e) {
          System.err.println("Error iniciando ServerSocket: " + e.getMessage());
          e.printStackTrace();
        }
      }
    };

    this.stop = false;
    this.serverThread.start();
  }

  @Override
  public void close() {
    // Liberar recursos: pool de hilos y servidor
    this.stop = true;

    // Cerrar pool de hilos
    if (threadPool != null) {
      threadPool.shutdown();
    }

    // Intentar despertar el hilo servidor de forma segura
    try (Socket socket = new Socket("localhost", getPort())) {
      // Esta conexión se hace, simplemente, para "despertar" el hilo servidor
    } catch (IOException e) {
      // Si no podemos conectar, el servidor probablemente ya está cerrado
      System.err.println("No se pudo conectar para despertar el servidor (probablemente ya cerrado): " + e.getMessage());
    }

    if (serverThread != null) {
      try {
        this.serverThread.join(5000); // Esperar máximo 5 segundos
      } catch (InterruptedException e) {
        System.err.println("Interrupción mientras se esperaba el cierre del servidor: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
      this.serverThread = null;
    }
  }
}
