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
 * Interface para el acceso a datos de documentos XSLT.
 * Permite implementaciones tanto en memoria como en base de datos.
 */
public interface XSLTDocumentDAO {
    
    /**
     * Obtiene todos los documentos XSLT almacenados.
     * @return Mapa con UUID como clave y contenido XSLT como valor
     */
    Map<String, String> getAllDocuments() throws SQLException;
    
    /**
     * Obtiene un documento XSLT por su UUID.
     * @param uuid Identificador único del documento
     * @return Contenido XSLT del documento, o null si no existe
     */
    String getDocument(String uuid) throws SQLException;
    
    /**
     * Almacena un nuevo documento XSLT con su XSD asociado.
     * @param uuid Identificador único del documento
     * @param content Contenido XSLT del documento
     * @param xsdUuid UUID del XSD asociado
     * @return true si se almacenó correctamente, false en caso contrario
     */
    boolean saveDocument(String uuid, String content, String xsdUuid) throws SQLException;
    
    /**
     * Elimina un documento XSLT.
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
    
    /**
     * Obtiene el UUID del XSD asociado a un documento XSLT.
     * @param uuid Identificador único del documento XSLT
     * @return UUID del XSD asociado, o null si el documento no existe
     */
    String getAssociatedXSD(String uuid) throws SQLException;
}
