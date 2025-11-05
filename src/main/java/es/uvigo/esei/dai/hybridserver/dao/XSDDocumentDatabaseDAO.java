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
 * Implementación en base de datos MySQL del DAO para documentos XSD.
 */
public class XSDDocumentDatabaseDAO implements XSDDocumentDAO {
    
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    public XSDDocumentDatabaseDAO(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
    
    @Override
    public Map<String, String> getAllDocuments() throws SQLException {
        Map<String, String> documents = new HashMap<>();
        final String sql = "SELECT uuid, content FROM XSD";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                documents.put(rs.getString("uuid"), rs.getString("content"));
            }
        }
        
        return documents;
    }
    
    @Override
    public String getDocument(String uuid) throws SQLException {
        if (uuid == null) {
            return null;
        }
        
        final String sql = "SELECT content FROM XSD WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
        }
        
        return null;
    }
    
    @Override
    public boolean saveDocument(String uuid, String content) throws SQLException {
        if (uuid == null || content == null) {
            return false;
        }
        
        final String sql = "INSERT INTO XSD (uuid, content) VALUES (?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            stmt.setString(2, content);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    @Override
    public boolean deleteDocument(String uuid) throws SQLException {
        if (uuid == null) {
            return false;
        }
        
        final String sql = "DELETE FROM XSD WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    @Override
    public boolean documentExists(String uuid) throws SQLException {
        if (uuid == null) {
            return false;
        }
        
        final String sql = "SELECT 1 FROM XSD WHERE uuid = ? LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
