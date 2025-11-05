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
 * Interface para el acceso a datos de documentos XML.
 * Permite implementaciones tanto en memoria como en base de datos.
 */
public interface XMLDocumentDAO {
    
    /**
     * Obtiene todos los documentos XML almacenados.
     * @return Mapa con UUID como clave y contenido XML como valor
     */
    Map<String, String> getAllDocuments() throws SQLException;
    
    /**
     * Obtiene un documento XML por su UUID.
     * @param uuid Identificador único del documento
     * @return Contenido XML del documento, o null si no existe
     */
    String getDocument(String uuid) throws SQLException;
    
    /**
     * Almacena un nuevo documento XML.
     * @param uuid Identificador único del documento
     * @param content Contenido XML del documento
     * @return true si se almacenó correctamente, false en caso contrario
     */
    boolean saveDocument(String uuid, String content) throws SQLException;
    
    /**
     * Elimina un documento XML.
     * @param uuid Identificador único del documento a eliminar
     * @return true si se eliminó correctamente, false si no existía
     */
    boolean deleteDocument(String uuid) throws SQLException;
    
    /**
     * Verifica si existe un documento con el UUID dado.
     * @param uuid Identificador único del documento
     * @return true si existe, false en caso contrario
     */
    boolean documentExists(String uuid) throws SQLException;
}
