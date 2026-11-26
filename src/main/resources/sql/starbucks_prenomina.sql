CREATE DATABASE IF NOT EXISTS starbucks_prenomina CHARACTER SET utf8mb4;
USE starbucks_prenomina;

CREATE TABLE IF NOT EXISTS `roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `rol_id` int(11) DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'ACTIVO',
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuario` (`usuario`),
  KEY `rol_id` (`rol_id`),
  CONSTRAINT `usuarios_ibfk_1` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `colaboradores` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `numero_empleado` varchar(20) NOT NULL,
  `nombre_completo` varchar(150) NOT NULL,
  `salario_diario` decimal(10,2) NOT NULL,
  `estado` varchar(20) DEFAULT 'ACTIVO',
  PRIMARY KEY (`id`),
  UNIQUE KEY `numero_empleado` (`numero_empleado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `periodos_quincenales` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `identificador` varchar(20) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `estado` varchar(20) DEFAULT 'CAPTURA',
  PRIMARY KEY (`id`),
  UNIQUE KEY `identificador` (`identificador`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dias_trabajados` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` int(11) DEFAULT NULL,
  `periodo_id` int(11) DEFAULT NULL,
  `dias_trabajados` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `colaborador_id` (`colaborador_id`),
  KEY `periodo_id` (`periodo_id`),
  CONSTRAINT `dias_trabajados_ibfk_1` FOREIGN KEY (`colaborador_id`) REFERENCES `colaboradores` (`id`),
  CONSTRAINT `dias_trabajados_ibfk_2` FOREIGN KEY (`periodo_id`) REFERENCES `periodos_quincenales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE dias_trabajados
  ADD UNIQUE INDEX IF NOT EXISTS uq_dias_periodo_colaborador (periodo_id, colaborador_id);

CREATE TABLE IF NOT EXISTS `tipos_incidencia` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `incidencias` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `colaborador_id` int(11) DEFAULT NULL,
  `periodo_id` int(11) DEFAULT NULL,
  `tipo_id` int(11) DEFAULT NULL,
  `fecha` date DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id`),
  KEY `colaborador_id` (`colaborador_id`),
  KEY `periodo_id` (`periodo_id`),
  KEY `tipo_id` (`tipo_id`),
  CONSTRAINT `incidencias_ibfk_1` FOREIGN KEY (`colaborador_id`) REFERENCES `colaboradores` (`id`),
  CONSTRAINT `incidencias_ibfk_2` FOREIGN KEY (`periodo_id`) REFERENCES `periodos_quincenales` (`id`),
  CONSTRAINT `incidencias_ibfk_3` FOREIGN KEY (`tipo_id`) REFERENCES `tipos_incidencia` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE incidencias
  ADD COLUMN IF NOT EXISTS fecha_fin DATE NULL,
  ADD COLUMN IF NOT EXISTS minutos_retardo INT NULL,
  ADD COLUMN IF NOT EXISTS clasificacion VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS autorizacion_documentada BOOLEAN NULL,
  ADD COLUMN IF NOT EXISTS tratamiento VARCHAR(150) NULL,
  ADD COLUMN IF NOT EXISTS referencia_documental VARCHAR(150) NULL;

CREATE TABLE IF NOT EXISTS `reglas_calculo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `detalles_prenomina` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `periodo_id` int(11) DEFAULT NULL,
  `colaborador_id` int(11) DEFAULT NULL,
  `dias_descuento` int(11) DEFAULT 0,
  `descuento_preliminar` decimal(12,2) DEFAULT 0.00,
  `estado` varchar(20) DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id`),
  KEY `periodo_id` (`periodo_id`),
  KEY `colaborador_id` (`colaborador_id`),
  CONSTRAINT `detalles_prenomina_ibfk_1` FOREIGN KEY (`periodo_id`) REFERENCES `periodos_quincenales` (`id`),
  CONSTRAINT `detalles_prenomina_ibfk_2` FOREIGN KEY (`colaborador_id`) REFERENCES `colaboradores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE reglas_calculo
  ADD COLUMN IF NOT EXISTS periodo_id INT NULL REFERENCES periodos_quincenales(id),
  ADD COLUMN IF NOT EXISTS tipo_id INT NULL REFERENCES tipos_incidencia(id),
  ADD COLUMN IF NOT EXISTS aplica_descuento BOOLEAN NULL,
  ADD COLUMN IF NOT EXISTS autorizado_por INT NULL REFERENCES usuarios(id),
  ADD COLUMN IF NOT EXISTS autorizado_en DATETIME NULL,
  ADD UNIQUE INDEX IF NOT EXISTS uq_regla_periodo_tipo (periodo_id, tipo_id);

ALTER TABLE detalles_prenomina
  ADD COLUMN IF NOT EXISTS salario_diario DECIMAL(10,2) NULL,
  ADD UNIQUE INDEX IF NOT EXISTS uq_prenomina_periodo_colaborador (periodo_id, colaborador_id);

CREATE TABLE IF NOT EXISTS `bitacora_cambios` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) DEFAULT NULL,
  `accion` varchar(100) NOT NULL,
  `entidad` varchar(50) NOT NULL,
  `identificador` int(11) NOT NULL,
  `fecha_hora` datetime DEFAULT current_timestamp(),
  `informacion_relevante` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `usuario_id` (`usuario_id`),
  CONSTRAINT `bitacora_cambios_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS revisiones_periodo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  periodo_id INT NOT NULL,
  usuario_id INT NOT NULL,
  observaciones TEXT,
  estado VARCHAR(20) NOT NULL,
  fecha_revision DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (periodo_id) REFERENCES periodos_quincenales(id),
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO roles (nombre) VALUES ('ADMINISTRADOR'), ('CAPTURISTA'), ('REVISOR');
INSERT IGNORE INTO tipos_incidencia (nombre)
VALUES ('Falta'), ('Retardo'), ('Permiso'), ('Vacaciones'), ('Incapacidad');
