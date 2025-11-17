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

import java.sql.SQLException;
import java.util.Map;

/**
 * Interface para el acceso a datos.
 * Permite implementaciones tanto en memoria como en base de datos.
 */
public interface PageDAO {
    
    /**
     * Obtiene todas las páginas HTML almacenadas.
     * @return Mapa con UUID como clave y contenido HTML como valor
     */
    Map<String, String> getAllPages()throws SQLException;
    
    /**
     * Obtiene una página HTML por su UUID.
     * @param uuid Identificador único de la página
     * @return Contenido HTML de la página, o null si no existe
     */
    String getPage(String uuid)throws SQLException;
    
    /**
     * Almacena una nueva página HTML.
     * @param uuid Identificador único de la página
     * @param content Contenido HTML de la página
     * @return true si se almacenó correctamente, false en caso contrario
     */
    boolean savePage(String uuid, String content) throws SQLException; 
    
    /**
     * Elimina una página HTML.
     * @param uuid Identificador único de la página a eliminar
     * @return true si se eliminó correctamente, false si no existía
     */
    boolean deletePage(String uuid)throws SQLException;
    
    /**
     * Verifica si existe una página con el UUID dado.
     * @param uuid Identificador único de la página
     * @return true si existe, false en caso contrario
     */
    boolean pageExists(String uuid)throws SQLException;
}