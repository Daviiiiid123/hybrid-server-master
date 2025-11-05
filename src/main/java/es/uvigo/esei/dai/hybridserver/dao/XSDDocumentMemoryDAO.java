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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del DAO para documentos XSD.
 * Utiliza un ConcurrentHashMap para garantizar thread-safety.
 */
public class XSDDocumentMemoryDAO implements XSDDocumentDAO {
    
    private final Map<String, String> documents;
    
    public XSDDocumentMemoryDAO() {
        this.documents = new ConcurrentHashMap<>();
    }
    
    public XSDDocumentMemoryDAO(Map<String, String> initialDocuments) {
        this.documents = new ConcurrentHashMap<>();
        if (initialDocuments != null) {
            this.documents.putAll(initialDocuments);
        }
    }
    
    @Override
    public Map<String, String> getAllDocuments() {
        return new HashMap<>(documents);
    }
    
    @Override
    public String getDocument(String uuid) {
        return documents.get(uuid);
    }
    
    @Override
    public boolean saveDocument(String uuid, String content) {
        if (uuid == null || content == null) {
            return false;
        }
        documents.put(uuid, content);
        return true;
    }
    
    @Override
    public boolean deleteDocument(String uuid) {
        if (uuid == null) {
            return false;
        }
        return documents.remove(uuid) != null;
    }
    
    @Override
    public boolean documentExists(String uuid) {
        return uuid != null && documents.containsKey(uuid);
    }
}
