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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.uvigo.esei.dai.hybridserver.dao.HTMLPageDAO;
import es.uvigo.esei.dai.hybridserver.dao.HTMLPageDatabaseDAO;
import es.uvigo.esei.dai.hybridserver.dao.HTMLPageMemoryDAO;

public class HybridServer implements AutoCloseable {
  private static final int SERVICE_PORT = 8888;
  private Thread serverThread;
  private boolean stop;
  private final ExecutorService threadPool;
  private final int numClients;
  private HTMLPageDAO pageDAO; // DAO para almacenamiento de páginas
  private final Properties config; // Configuraciones del servidor
  private final int port;
  private Configuration conf;

  public HybridServer() { //REVISAR ESTO, EN CASO DE 0 PARAMETROS SE USA BD O MEMORIA
    // Inicializar con los parámetros por defecto
    this.numClients = 50;
    this.port = SERVICE_PORT;
    this.config = null;
    this.threadPool = Executors.newFixedThreadPool(numClients);
    
    // Usar DAO en memoria por defecto
    this.pageDAO = new HTMLPageMemoryDAO();
  }
/*
  public HybridServer(Map<String, String> pages) {
    // Inicializar con la base de datos en memoria conteniendo "pages"
    this.numClients = 50;
    this.port = SERVICE_PORT;
    this.config = null;
    this.threadPool = Executors.newFixedThreadPool(numClients);
    
    // Usar DAO en memoria con páginas iniciales
    this.pageDAO = new HTMLPageMemoryDAO(pages);
  }
 */

  public HybridServer(Configuration conf) {
    // Inicializar con la configuración recibida del fichero XML
    this.conf = conf;
    this.numClients = conf.getNumClients();
    this.port = conf.getHttpPort();
    this.config = null;
    this.threadPool = Executors.newFixedThreadPool(numClients);
    
    // Determinar tipo de DAO según configuración
    String dbUrl = conf.getDbURL();
    String dbUser = conf.getDbUser();
    String dbPassword = conf.getDbPassword();
    
    if (dbUrl != null && dbUser != null && dbPassword != null) {
      // Intentar usar base de datos
      try {
        this.pageDAO = new HTMLPageDatabaseDAO(dbUrl, dbUser, dbPassword);
        System.out.println("Usando almacenamiento en base de datos: " + dbUrl);
      } catch (Exception e) {
        System.err.println("Error conectando con base de datos, usando memoria: " + e.getMessage());
        this.pageDAO = new HTMLPageMemoryDAO();
      }
    } else {
      // Usar memoria si no hay configuración de BD
      this.pageDAO = new HTMLPageMemoryDAO();
      System.out.println("Usando almacenamiento en memoria");
    }
  }


  public HybridServer(Properties properties) {
    // Inicializar con los parámetros recibidos
    this.config = properties;
    this.numClients = Integer.parseInt(properties.getProperty("numClients", "50"));
    this.port = Integer.parseInt(properties.getProperty("port", String.valueOf(SERVICE_PORT)));
    this.threadPool = Executors.newFixedThreadPool(numClients);
    
    // Determinar tipo de DAO según configuración
    String dbUrl = properties.getProperty("db.url");
    String dbUser = properties.getProperty("db.user");
    String dbPassword = properties.getProperty("db.password");
    
    if (dbUrl != null && dbUser != null && dbPassword != null) {
      // Intentar usar base de datos
      try {
        this.pageDAO = new HTMLPageDatabaseDAO(dbUrl, dbUser, dbPassword);
        System.out.println("Usando almacenamiento en base de datos: " + dbUrl);
      } catch (Exception e) {
        System.err.println("Error conectando con base de datos, usando memoria: " + e.getMessage());
        this.pageDAO = new HTMLPageMemoryDAO();
      }
    } else {
      // Usar memoria si no hay configuración de BD
      this.pageDAO = new HTMLPageMemoryDAO();
      System.out.println("Usando almacenamiento en memoria");
    }
  }

  public int getPort() {
    return port;
  }
  
  // Getters for ServiceThread
  public HTMLPageDAO getPageDAO() {
    return pageDAO;
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
              threadPool.execute(new ServiceThread(socket, HybridServer.this));
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
        this.serverThread.join();
      } catch (InterruptedException e) {
        System.err.println("Interrupción mientras se esperaba el cierre del servidor: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
      this.serverThread = null;
    }
  }
}