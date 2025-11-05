-- Script para inicializar la base de datos del Hybrid Server
-- MySQL 8+

-- Crear base de datos
CREATE DATABASE IF NOT EXISTS hstestdb 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Usar la base de datos
USE hstestdb;

-- Crear usuario específico para la aplicación
CREATE USER IF NOT EXISTS 'hsdb'@'localhost' IDENTIFIED BY 'hsdbpass';
GRANT ALL PRIVILEGES ON hstestdb.* TO 'hsdb'@'localhost';
FLUSH PRIVILEGES;

-- Crear tabla para páginas HTML
-- IMPORTANTE: El nombre de la tabla debe ser 'HTML' (mayúsculas) para los tests
CREATE TABLE IF NOT EXISTS HTML (
    uuid VARCHAR(36) PRIMARY KEY COMMENT 'UUID único de la página',
    content TEXT NOT NULL COMMENT 'Contenido HTML de la página',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Almacena las páginas HTML del servidor híbrido';

-- Insertar páginas de prueba requeridas por los tests
-- Estas son las 10 páginas que espera ClientRequestsWithDatabaseTest
INSERT INTO HTML (uuid, content) VALUES 
('6df1047e-cf19-4a83-8cf3-38f5e53f7725', 'This is the html page 6df1047e-cf19-4a83-8cf3-38f5e53f7725.'),
('79e01232-5ea4-41c8-9331-1c1880a1d3c2', 'This is the html page 79e01232-5ea4-41c8-9331-1c1880a1d3c2.'),
('a35b6c5e-22d6-4707-98b4-462482e26c9e', 'This is the html page a35b6c5e-22d6-4707-98b4-462482e26c9e.'),
('3aff2f9c-0c7f-4630-99ad-27a0cf1af137', 'This is the html page 3aff2f9c-0c7f-4630-99ad-27a0cf1af137.'),
('77ec1d68-84e1-40f4-be8e-066e02f4e373', 'This is the html page 77ec1d68-84e1-40f4-be8e-066e02f4e373.'),
('8f824126-0bd1-4074-b88e-c0b59d3e67a3', 'This is the html page 8f824126-0bd1-4074-b88e-c0b59d3e67a3.'),
('c6c80c75-b335-4f68-b7a7-59434413ce6c', 'This is the html page c6c80c75-b335-4f68-b7a7-59434413ce6c.'),
('f959ecb3-6382-4ae5-9325-8fcbc068e446', 'This is the html page f959ecb3-6382-4ae5-9325-8fcbc068e446.'),
('2471caa8-e8df-44d6-94f2-7752a74f6819', 'This is the html page 2471caa8-e8df-44d6-94f2-7752a74f6819.'),
('fa0979ca-2734-41f7-84c5-e40e0886e408', 'This is the html page fa0979ca-2734-41f7-84c5-e40e0886e408.'),
-- Páginas de ejemplo adicionales
('example-page-1', '<html><head><title>Página de Ejemplo 1</title></head><body><h1>¡Hola desde la Base de Datos!</h1><p>Esta es una página de ejemplo almacenada en MySQL.</p><p><a href="/html">Volver a la lista</a></p></body></html>'),
('example-page-2', '<html><head><title>Página de Ejemplo 2</title></head><body><h1>Segunda Página</h1><p>Otra página de ejemplo con contenido diferente.</p><p><a href="/html">Ver todas las páginas</a></p></body></html>'),
('welcome-db', '<html><head><title>Bienvenida desde BD</title></head><body><h1>¡Bienvenido!</h1><p>Esta página se carga desde la base de datos MySQL.</p><p>Demuestra que el DAO funciona correctamente.</p><p><a href="/html">Ver lista completa</a></p></body></html>')
ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = CURRENT_TIMESTAMP;

-- Crear tabla para documentos XSD
CREATE TABLE IF NOT EXISTS XSD (
    uuid VARCHAR(36) PRIMARY KEY COMMENT 'UUID único del documento XSD',
    content TEXT NOT NULL COMMENT 'Contenido XSD del documento',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Almacena los documentos XSD del servidor híbrido';

-- Crear tabla para documentos XML
CREATE TABLE IF NOT EXISTS XML (
    uuid VARCHAR(36) PRIMARY KEY COMMENT 'UUID único del documento XML',
    content TEXT NOT NULL COMMENT 'Contenido XML del documento',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Almacena los documentos XML del servidor híbrido';

-- Crear tabla para documentos XSLT
-- Nota: xsd contiene el UUID del XSD asociado (puede estar en otro servidor, no hay FK)
CREATE TABLE IF NOT EXISTS XSLT (
    uuid VARCHAR(36) PRIMARY KEY COMMENT 'UUID único del documento XSLT',
    content TEXT NOT NULL COMMENT 'Contenido XSLT del documento',
    xsd CHAR(36) NOT NULL COMMENT 'UUID del XSD asociado (puede estar en otro servidor)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_xsd (xsd),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Almacena los documentos XSLT del servidor híbrido';

-- Mostrar información de la base de datos creada
SELECT 'Base de datos hstestdb creada exitosamente' AS status;
SELECT COUNT(*) as total_pages FROM HTML;
SELECT uuid, SUBSTRING(content, 1, 50) as content_preview, created_at FROM HTML ORDER BY created_at;