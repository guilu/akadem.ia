INSERT INTO syllabuses (id, name, description, visibility)
VALUES ('11111111-1111-1111-1111-111111111000', 'Subalterno GVA 2026', 'Temario de la oposición a subalterno de la Comunidad Valenciana', 'GLOBAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'Tema 1 - Constitución Española I',
  'Preguntas sobre la Constitución Española',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111112',
  'Tema 2 - Constitución Española II',
  'Preguntas sobre la Constitución Española',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111113',
  'Tema 3: El Estatuto de Autonomía de la Comunitat Valenciana',
  'Preguntas sobre el Estatuto de Autonomía de la Comunitat Valenciana',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111114',
  'Tema 4: La Ley del Consell (I)',
  'Preguntas sobre la Ley del Consell (I)',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111115',
  'Tema 5: La Ley del Consell (II)',
  'Preguntas sobre la Ley del Consell (II)',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111116',
  'Tema 6: Igualdad, LGTBI y violencia sobre la mujer',
  'Preguntas sobre Igualdad, LGTBI y violencia sobre la mujer',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111117',
  'Tema 7: Transparencia y buen gobierno',
  'Preguntas sobre Transparencia y buen gobierno',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111118',
  'Tema 8: Procedimiento administrativo común',
  'Preguntas sobre el Procedimiento administrativo común',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111119',
  'Tema 9: Actos administrativos y recursos',
  'Preguntas sobre Actos administrativos y recursos',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111120',
  'Tema 10: Función pública valenciana',
  'Preguntas sobre la Función pública valenciana',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111121',
  'Tema 11: Condiciones de trabajo en la GVA',
  'Preguntas sobre las Condiciones de trabajo en la GVA',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111122',
  'Tema 12: Atención a la ciudadanía y registro',
  'Preguntas sobre la Atención a la ciudadanía y registro',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111123',
  'Tema 13: Control de acceso y protección de datos',
  'Preguntas sobre el Control de acceso y protección de datos',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111124',
  'Tema 14: Prevención de riesgos laborales',
  'Preguntas sobre la Prevención de riesgos laborales',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);
INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111125',
  'Tema 15: Seguridad digital en la GVA',
  'Preguntas sobre la Seguridad digital en la GVA',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
);


ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111201','11111111-1111-1111-1111-111111111111','Princípios fundamentales y valores superiores',2) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111202','11111111-1111-1111-1111-111111111111','Derechos y libertades fundamentales',3) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111203','11111111-1111-1111-1111-111111111111','Garantías y suspensión de derechos',4) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111204','11111111-1111-1111-1111-111111111111','Reforma constitucional',5) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111205','11111111-1111-1111-1111-111111111112','La Corona: funciones del Rey',6) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111206','11111111-1111-1111-1111-111111111112','Las Cortes Generales: composición y funciones',7) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111207','11111111-1111-1111-1111-111111111112','El Gobierno y la Administración',8) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111208','11111111-1111-1111-1111-111111111112','El Tribunal Constitucional y el Poder Judicial',9) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111209','11111111-1111-1111-1111-111111111113','Estructura y contenido del Estatuto',10) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111210','11111111-1111-1111-1111-111111111113','Competencias de la Generalitat',11) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111211','11111111-1111-1111-1111-111111111113','Instituciones: Les Corts, el Consell, el Síndic',12) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111212','11111111-1111-1111-1111-111111111113','Organización territorial valenciana',13) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111213','11111111-1111-1111-1111-111111111114','El President de la Generalitat',14) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111214','11111111-1111-1111-1111-111111111114','El Consell: composición y funcionamiento',15) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111215','11111111-1111-1111-1111-111111111114','Las Consellerias: organización',16) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111216','11111111-1111-1111-1111-111111111114','Órganos superiores y directivos',17) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111217','11111111-1111-1111-1111-111111111115','Administración territorial de la Generalitat',18) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111218','11111111-1111-1111-1111-111111111115','Sector público instrumental',19) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111219','11111111-1111-1111-1111-111111111115','Relaciones interadministrativas',20) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111220','11111111-1111-1111-1111-111111111115','Potestad normativa del Consell',21) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111221','11111111-1111-1111-1111-111111111116','Ley de Igualdad entre mujeres y hombres',22) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111222','11111111-1111-1111-1111-111111111116','Ley integral contra la violencia sobre la mujer',23) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111223','11111111-1111-1111-1111-111111111116','Ley de igualdad LGTBI de la Comunitat Valenciana',24) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111224','11111111-1111-1111-1111-111111111116','Planes de igualdad en la Administración',25) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111225','11111111-1111-1111-1111-111111111117','Ley de Transparencia (19/2013)',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111226','11111111-1111-1111-1111-111111111117','Ley valenciana de transparencia (2/2015)',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111227','11111111-1111-1111-1111-111111111117','Derecho de acceso a la información pública',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111228','11111111-1111-1111-1111-111111111117','Publicidad activa y buen gobierno',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111229','11111111-1111-1111-1111-111111111118','Ley 39/2015: ámbito y principios',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111230','11111111-1111-1111-1111-111111111118','Los interesados: capacidad y representación',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111231','11111111-1111-1111-1111-111111111118','Fases del procedimiento administrativo',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111232','11111111-1111-1111-1111-111111111118','Plazos, cómputo de términos y silencio',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111233','11111111-1111-1111-1111-111111111119','Requisitos y eficacia del acto administrativo',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111234','11111111-1111-1111-1111-111111111119','Nulidad y anulabilidad',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111235','11111111-1111-1111-1111-111111111119','Recurso de alzada y reposición',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111236','11111111-1111-1111-1111-111111111119','Recurso extraordinario de revisión',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111237','11111111-1111-1111-1111-111111111120','Ley de Función Pública Valenciana (4/2021)',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111238','11111111-1111-1111-1111-111111111120','Clases de personal al servicio de la Generalitat',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111239','11111111-1111-1111-1111-111111111120','Acceso al empleo público: sistemas selectivos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111240','11111111-1111-1111-1111-111111111120','Situaciones administrativas y provisión de puestos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111241','11111111-1111-1111-1111-111111111121','Jornada, horarios y permisos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111242','11111111-1111-1111-1111-111111111121','Vacaciones y licencias',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111243','11111111-1111-1111-1111-111111111121','Régimen retributivo',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111244','11111111-1111-1111-1111-111111111121','Régimen disciplinario',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111245','11111111-1111-1111-1111-111111111122','Derechos de los ciudadanos ante la Administración',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111246','11111111-1111-1111-1111-111111111122','Registro de entrada y salida',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111247','11111111-1111-1111-1111-111111111122','Quejas, sugerencias y reclamaciones',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111248','11111111-1111-1111-1111-111111111122','Atención presencial y telemática',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111249','11111111-1111-1111-1111-111111111123','RGPD: principios y bases de legitimación',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111250','11111111-1111-1111-1111-111111111123','Derechos ARCO-POL del interesado',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111251','11111111-1111-1111-1111-111111111123','Control de acceso a edificios públicos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111252','11111111-1111-1111-1111-111111111123','Videovigilancia y seguridad de datos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111253','11111111-1111-1111-1111-111111111124','Ley 31/1995 de PRL: conceptos básicos',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111254','11111111-1111-1111-1111-111111111124','Derechos y obligaciones de los trabajadores',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111255','11111111-1111-1111-1111-111111111124','Plan de prevención de la GVA',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111256','11111111-1111-1111-1111-111111111124','Riesgos específicos del puesto de subalterno',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111257','11111111-1111-1111-1111-111111111125','Esquema Nacional de Seguridad (ENS)',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111258','11111111-1111-1111-1111-111111111125','Política de seguridad de la información de la GVA',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111259','11111111-1111-1111-1111-111111111125','Uso seguro de dispositivos y contraseñas',26) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111260','11111111-1111-1111-1111-111111111125','Amenazas, incidentes y protocolos de actuación',26) ON CONFLICT DO NOTHING;
