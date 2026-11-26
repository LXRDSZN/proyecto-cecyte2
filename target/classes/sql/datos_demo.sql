-- Datos ficticios para el entorno académico local.
USE starbucks_prenomina;
START TRANSACTION;

INSERT IGNORE INTO colaboradores (numero_empleado, nombre_completo, salario_diario, estado)
VALUES ('DEMO-001','Valeria Montes',380.50,'ACTIVO'),
       ('DEMO-002','Emilio Cruz',345.00,'ACTIVO'),
       ('DEMO-003','Lucía Reyes',412.75,'ACTIVO'),
       ('DEMO-004','Daniel Ortega',365.00,'ACTIVO'),
       ('DEMO-005','Sofía Luna',390.25,'ACTIVO'),
       ('DEMO-006','Mateo Ríos',350.00,'INACTIVO');

SET @inicio_demo = DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL (DAY(CURRENT_DATE) - 1) DAY),
    INTERVAL IF(DAY(CURRENT_DATE) <= 15, 0, 15) DAY);
SET @fin_demo = IF(DAY(CURRENT_DATE) <= 15, DATE_ADD(@inicio_demo, INTERVAL 14 DAY), LAST_DAY(CURRENT_DATE));
SET @id_demo = CONCAT('DEMO-', DATE_FORMAT(@inicio_demo,'%Y-%m'), '-', IF(DAY(@inicio_demo)=1,1,2));
INSERT INTO periodos_quincenales (identificador, fecha_inicio, fecha_fin, estado)
SELECT @id_demo, @inicio_demo, @fin_demo, 'CAPTURA'
WHERE NOT EXISTS (
    SELECT 1 FROM periodos_quincenales
    WHERE identificador = @id_demo OR (fecha_inicio <= @fin_demo AND fecha_fin >= @inicio_demo)
);

INSERT IGNORE INTO dias_trabajados (colaborador_id, periodo_id, dias_trabajados)
SELECT c.id, p.id, DATEDIFF(p.fecha_fin,p.fecha_inicio) + 1 -
    CASE c.numero_empleado WHEN 'DEMO-001' THEN 1 WHEN 'DEMO-003' THEN 1 WHEN 'DEMO-004' THEN 2
         WHEN 'DEMO-005' THEN 2 ELSE 0 END
FROM colaboradores c CROSS JOIN periodos_quincenales p
WHERE c.numero_empleado IN ('DEMO-001','DEMO-002','DEMO-003','DEMO-004','DEMO-005')
  AND p.identificador = @id_demo AND p.estado = 'CAPTURA';

INSERT INTO incidencias (colaborador_id, periodo_id, tipo_id, fecha, fecha_fin, observaciones,
                         estado, minutos_retardo, clasificacion, autorizacion_documentada, tratamiento,
                         referencia_documental)
SELECT c.id, p.id, t.id, DATE_ADD(p.fecha_inicio, INTERVAL demo.dia DAY),
       IF(demo.dias = 1, NULL, DATE_ADD(p.fecha_inicio, INTERVAL (demo.dia + demo.dias - 1) DAY)),
       demo.nota, 'PENDIENTE', demo.minutos, demo.clasificacion, demo.autorizacion, demo.tratamiento, demo.referencia
FROM (
    SELECT 'DEMO-001' empleado, 'Falta' tipo, 2 dia, 1 dias, 'Falta registrada para el ejercicio' nota,
           NULL minutos, NULL clasificacion, NULL autorizacion, NULL tratamiento, NULL referencia
    UNION ALL SELECT 'DEMO-002','Retardo',4,1,'Registro de entrada',12,NULL,NULL,NULL,NULL
    UNION ALL SELECT 'DEMO-003','Permiso',6,1,'Solicitud ficticia',NULL,'Personal',1,'Pendiente de revisión',NULL
    UNION ALL SELECT 'DEMO-004','Vacaciones',8,2,'Periodo solicitado y aprobado',NULL,NULL,1,NULL,NULL
    UNION ALL SELECT 'DEMO-005','Incapacidad',10,2,'Referencia académica',NULL,'General',NULL,NULL,'DEMO-INC-01'
) demo JOIN colaboradores c ON c.numero_empleado = demo.empleado
JOIN tipos_incidencia t ON t.nombre = demo.tipo
JOIN periodos_quincenales p ON p.identificador = @id_demo AND p.estado = 'CAPTURA'
WHERE NOT EXISTS (
    SELECT 1 FROM incidencias i
    WHERE i.periodo_id = p.id AND i.colaborador_id = c.id AND i.tipo_id = t.id
);

COMMIT;
