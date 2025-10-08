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
package es.uvigo.esei.dai.hybridserver.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación en base de datos MySQL del DAO para páginas HTML.
 * Crea una nueva conexión para cada operación para evitar problemas si la BD se cae.
 */
public class HTMLPageDatabaseDAO implements HTMLPageDAO {
    
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    public HTMLPageDatabaseDAO(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        
        // Intentar crear la tabla si no existe
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        final String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS HTML (" +
            "uuid VARCHAR(36) PRIMARY KEY, " +
            "content TEXT NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            
            stmt.executeUpdate();
            System.out.println("Base de datos inicializada correctamente");
            
        } catch (SQLException e) {
            System.err.println("Error inicializando la base de datos: " + e.getMessage());
        }
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
    
    @Override
    public Map<String, String> getAllPages() {
        Map<String, String> pages = new HashMap<>();
        final String sql = "SELECT uuid, content FROM HTML";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                pages.put(rs.getString("uuid"), rs.getString("content"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo todas las páginas: " + e.getMessage());
        }
        
        return pages;
    }
    
    @Override
    public String getPage(String uuid) {
        if (uuid == null) {
            return null;
        }
        
        final String sql = "SELECT content FROM HTML WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error obteniendo página " + uuid + ": " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public boolean savePage(String uuid, String content) {
        if (uuid == null || content == null) {
            return false;
        }
        
        final String sql = "INSERT INTO HTML (uuid, content) VALUES (?, ?) " +
                          "ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = CURRENT_TIMESTAMP";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            stmt.setString(2, content);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error guardando página " + uuid + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean deletePage(String uuid) {
        if (uuid == null) {
            return false;
        }
        
        final String sql = "DELETE FROM HTML WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error eliminando página " + uuid + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean pageExists(String uuid) {
        if (uuid == null) {
            return false;
        }
        
        final String sql = "SELECT 1 FROM HTML WHERE uuid = ? LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("Error verificando existencia de página " + uuid + ": " + e.getMessage());
            return false;
        }
    }
}