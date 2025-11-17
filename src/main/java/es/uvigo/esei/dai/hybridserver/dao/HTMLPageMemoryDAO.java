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
 * Implementación en memoria del DAO para páginas HTML.
 * Utiliza un ConcurrentHashMap para garantizar thread-safety.
 */
public class HTMLPageMemoryDAO implements PageDAO {
    
    private final Map<String, String> pages;
    
    public HTMLPageMemoryDAO() {
        this.pages = new ConcurrentHashMap<>();
    }
    
    public HTMLPageMemoryDAO(Map<String, String> initialPages) {
        this.pages = new ConcurrentHashMap<>();
        if (initialPages != null) {
            this.pages.putAll(initialPages);
        }
    }
    
    @Override
    public Map<String, String> getAllPages() {
        return new HashMap<>(pages);
    }
    
    @Override
    public String getPage(String uuid) {
        return pages.get(uuid);
    }
    
    @Override
    public boolean savePage(String uuid, String content) {
        if (uuid == null || content == null) {
            return false;
        }
        pages.put(uuid, content);
        return true;
    }
    
    @Override
    public boolean deletePage(String uuid) {
        if (uuid == null) {
            return false;
        }
        return pages.remove(uuid) != null;
    }
    
    @Override
    public boolean pageExists(String uuid) {
        return uuid != null && pages.containsKey(uuid);
    }
}