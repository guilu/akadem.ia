INSERT INTO syllabuses (id, name, description, visibility)
VALUES ('11111111-1111-1111-1111-111111111000', 'Subalterno GVA 2026', 'Temario de la oposición a subalterno de la Comunidad Valenciana', 'GLOBAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO subjects(id, name, description, visibility, owner_id, syllabus_id)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'Constitución Española',
  'Preguntas básicas sobre la Constitución Española',
  'GLOBAL',
  NULL,
  '11111111-1111-1111-1111-111111111000'
)
ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111201','11111111-1111-1111-1111-111111111111','Preámbulo',2) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111202','11111111-1111-1111-1111-111111111111','Título Preliminar',3) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111203','11111111-1111-1111-1111-111111111111','Título I · Capítulo I · De los españoles y los extranjeros (Arts. 10–13)',4) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111204','11111111-1111-1111-1111-111111111111','Título I · Capítulo II · Sección 1ª · Derechos fundamentales y libertades públicas (Arts. 14–29)',5) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111205','11111111-1111-1111-1111-111111111111','Título I · Capítulo II · Sección 2ª · Derechos y deberes de los ciudadanos (Arts. 30–38)',6) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111206','11111111-1111-1111-1111-111111111111','Título I · Capítulo III · Principios rectores de la política social y económica (Arts. 39–41)',7) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111207','11111111-1111-1111-1111-111111111111','Título I · Capítulo IV · Garantías de las libertades y derechos fundamentales (Arts. 42–50)',8) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111208','11111111-1111-1111-1111-111111111111','Título I · Capítulo V · Suspensión de los derechos y libertades (Arts. 51–55)',9) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111209','11111111-1111-1111-1111-111111111111','Título II · De la Corona (Arts. 56–65)',10) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111210','11111111-1111-1111-1111-111111111111','Título III · Capítulo I · De las Cámaras (Arts. 66–80)',11) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111211','11111111-1111-1111-1111-111111111111','Título III · Capítulo II · Elaboración de las leyes (Arts. 81–92)',12) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111212','11111111-1111-1111-1111-111111111111','Título III · Capítulo III · Tratados internacionales  (Arts. 93–96)',13) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111213','11111111-1111-1111-1111-111111111111','Título IV · Del Gobierno y de la Administración (Arts. 97–107)',14) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111214','11111111-1111-1111-1111-111111111111','Título V · Relaciones entre Gobierno y Cortes Generales (Arts. 108–116)',15) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111215','11111111-1111-1111-1111-111111111111','Título VI · Del Poder Judicial (Arts. 117–127)',16) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111216','11111111-1111-1111-1111-111111111111','Título VII · Economía y Hacienda (Arts. 128–136)',17) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111217','11111111-1111-1111-1111-111111111111','Título VIII · Capítulo I · Principios generales (Arts. 137–139)',18) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111218','11111111-1111-1111-1111-111111111111','Título VIII · Capítulo II · Administración local (Arts. 140–142)',19) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111219','11111111-1111-1111-1111-111111111111','Título VIII · Capítulo III · Comunidades Autónomas (Arts. 143–158)',20) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111220','11111111-1111-1111-1111-111111111111','Título IX · Del Tribunal Constitucional (Arts. 159–165)',21) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111221','11111111-1111-1111-1111-111111111111','Título X · De la reforma constitucional (Arts. 166–169)',22) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111222','11111111-1111-1111-1111-111111111111','Disposiciones adicionales',23) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111223','11111111-1111-1111-1111-111111111111','Disposiciones transitorias',24) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111224','11111111-1111-1111-1111-111111111111','Disposición derogatoria',25) ON CONFLICT DO NOTHING;
INSERT INTO units(id, subject_id, name, order_index) VALUES ('11111111-1111-1111-1111-111111111225','11111111-1111-1111-1111-111111111111','Disposición final',26) ON CONFLICT DO NOTHING;

