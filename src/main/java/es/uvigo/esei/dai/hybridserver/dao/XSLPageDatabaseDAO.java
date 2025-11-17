package es.uvigo.esei.dai.hybridserver.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class XSLPageDatabaseDAO implements PageDAO{

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    public XSLPageDatabaseDAO(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
    
    @Override
    public Map<String, String> getAllPages() throws SQLException{
        Map<String, String> pages = new HashMap<>();
        final String sql = "SELECT uuid, content FROM XSL";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                pages.put(rs.getString("uuid"), rs.getString("content"));
            }
            
        } 
        
        return pages;
    }
    
    @Override
    public String getPage(String uuid) throws SQLException{
        if (uuid == null) {
            return null;
        }
        
        final String sql = "SELECT content FROM XSL WHERE uuid = ?";
        
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
    public boolean savePage(String uuid, String content) throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'savePage'");
    }

    public boolean savePageXSL(String uuid, String content, String uuidXsd) throws SQLException{
        if (uuid == null || content == null) {
            return false;
        }
        
        final String sql = "INSERT INTO XSL (uuid, content, xsd) VALUES (?, ?,?) " ;
                          

        // si el try falla, lanzará la excepción                          
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            stmt.setString(2, content);
            stmt.setString(3, uuidXsd);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } 
    }
    
    @Override
    public boolean deletePage(String uuid) throws SQLException{
        if (uuid == null) {
            return false;
        }
        
        final String sql = "DELETE FROM XSL WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        }
    }
    
    @Override
    public boolean pageExists(String uuid) throws SQLException{ //se sustituye el catch por esto, para permitir que luego el esrvidor pueda gestionar el error y mostrar
                                                            // el error correspondiente que en este caso seria 500
        if (uuid == null) {
            return false;
        }
        
        final String sql = "SELECT 1 FROM XSL WHERE uuid = ? LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        }
    }

   
}


