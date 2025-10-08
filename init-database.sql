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
CREATE TABLE IF NOT EXISTS html_pages (
    uuid VARCHAR(36) PRIMARY KEY COMMENT 'UUID único de la página',
    content TEXT NOT NULL COMMENT 'Contenido HTML de la página',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Almacena las páginas HTML del servidor híbrido';

-- Insertar algunas páginas de ejemplo
INSERT INTO html_pages (uuid, content) VALUES 
('example-page-1', '<html><head><title>Página de Ejemplo 1</title></head><body><h1>¡Hola desde la Base de Datos!</h1><p>Esta es una página de ejemplo almacenada en MySQL.</p><p><a href="/html">Volver a la lista</a></p></body></html>'),
('example-page-2', '<html><head><title>Página de Ejemplo 2</title></head><body><h1>Segunda Página</h1><p>Otra página de ejemplo con contenido diferente.</p><p><a href="/html">Ver todas las páginas</a></p></body></html>'),
('welcome-db', '<html><head><title>Bienvenida desde BD</title></head><body><h1>¡Bienvenido!</h1><p>Esta página se carga desde la base de datos MySQL.</p><p>Demuestra que el DAO funciona correctamente.</p><p><a href="/html">Ver lista completa</a></p></body></html>')
ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = CURRENT_TIMESTAMP;

-- Mostrar información de la base de datos creada
SELECT 'Base de datos hstestdb creada exitosamente' AS status;
SELECT COUNT(*) as total_pages FROM html_pages;
SELECT uuid, SUBSTRING(content, 1, 50) as content_preview, created_at FROM html_pages ORDER BY created_at;