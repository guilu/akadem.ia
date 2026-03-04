
INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('c5929461-d5b0-4bf7-8785-1c8cdfa11b18','11111111-1111-1111-1111-111111111202','¿Cuál es la forma política del Estado español?','Art. 1','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('e37ad5d3-e51a-4ebf-bb94-81c66be990e1','c5929461-d5b0-4bf7-8785-1c8cdfa11b18','República presidencial',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('ba088092-8ce4-4604-970f-1d8cb7bd6fd0','c5929461-d5b0-4bf7-8785-1c8cdfa11b18','Monarquía parlamentaria',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('1ea8c34b-e96b-49f0-8157-e3af17891221','c5929461-d5b0-4bf7-8785-1c8cdfa11b18','Monarquía absoluta',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('68f1d52a-cb9c-4444-875a-5c43ade49fcd','c5929461-d5b0-4bf7-8785-1c8cdfa11b18','República federal',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('6e6751eb-b673-4322-a876-606ac6ccb62f','11111111-1111-1111-1111-111111111202','¿Dónde reside la soberanía nacional según la Constitución?','Art. 1','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('79554af5-80eb-499f-ab4d-7ec724bb483d','6e6751eb-b673-4322-a876-606ac6ccb62f','En las Cortes Generales',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('34a9bce5-b22a-41f4-b952-9faf0c45597f','6e6751eb-b673-4322-a876-606ac6ccb62f','En el Gobierno',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('3ded7493-907e-42f5-9828-7bb15ec67e8b','6e6751eb-b673-4322-a876-606ac6ccb62f','En el pueblo español',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('41cb24f0-0045-4ba7-beaa-264d4b444235','6e6751eb-b673-4322-a876-606ac6ccb62f','En el Rey',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('2caf3af5-e03b-4e6e-b321-7344ffb08821','11111111-1111-1111-1111-111111111202','¿Qué principio reconoce el artículo 2 de la Constitución?','Art. 2','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('ded2edb4-7bda-4948-8b4f-e10a741c537d','2caf3af5-e03b-4e6e-b321-7344ffb08821','El derecho a la secesión',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('16913b9b-6efe-42ea-b492-2e41bf54576f','2caf3af5-e03b-4e6e-b321-7344ffb08821','El derecho a la autonomía de nacionalidades y regiones',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('60eb583a-0628-4a4a-b234-6c078f1c4193','2caf3af5-e03b-4e6e-b321-7344ffb08821','La supremacía del poder ejecutivo',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('7e099f3f-4355-4a2e-8e0b-808c91c63655','2caf3af5-e03b-4e6e-b321-7344ffb08821','El federalismo obligatorio',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('00b2a5de-b7e6-4d2d-a749-f64619a53a5f','11111111-1111-1111-1111-111111111202','¿Cuál es la lengua oficial del Estado?','Art. 3','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('c0e92551-2439-4e07-9b3d-b60c3c42a71b','00b2a5de-b7e6-4d2d-a749-f64619a53a5f','El castellano',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('6f5bfd5b-0739-4f85-b332-37c9ed3f1813','00b2a5de-b7e6-4d2d-a749-f64619a53a5f','El catalán',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('911cc08b-0f21-4177-8fb1-9291dfde78bb','00b2a5de-b7e6-4d2d-a749-f64619a53a5f','El gallego',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('dd18d569-fc0d-4b38-9481-8ec6967b6eab','00b2a5de-b7e6-4d2d-a749-f64619a53a5f','El euskera',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('d4671873-1193-4688-a9ce-e1b62aea2c80','11111111-1111-1111-1111-111111111202','¿Cuál es la bandera de España según la Constitución?','Art. 4','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('8112fdd0-5b7b-4f2b-b36a-078d27e9a939','d4671873-1193-4688-a9ce-e1b62aea2c80','Verde, blanca y roja',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('f614411f-bd1c-493c-b1f9-81602f451c2b','d4671873-1193-4688-a9ce-e1b62aea2c80','Roja, amarilla y roja',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('40819d6f-c7a3-48e5-b325-af526f8ddb47','d4671873-1193-4688-a9ce-e1b62aea2c80','Azul y blanca',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('f99bd2a4-bb6a-48b1-9de6-74916ddc5df0','d4671873-1193-4688-a9ce-e1b62aea2c80','Roja y blanca',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('122e4eff-e3be-4565-b07d-1cf847fefa6c','11111111-1111-1111-1111-111111111202','¿Qué función atribuye la Constitución a los partidos políticos?','Art. 6','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('ebbade25-773e-4c85-943b-e2d98e4cea50','122e4eff-e3be-4565-b07d-1cf847fefa6c','Ejercer el poder judicial',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('702e2984-be04-402f-8a0f-b196a0717b08','122e4eff-e3be-4565-b07d-1cf847fefa6c','Expresar el pluralismo político',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('74cfff35-d4bd-4048-a7b4-0960afb0a792','122e4eff-e3be-4565-b07d-1cf847fefa6c','Administrar las Fuerzas Armadas',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('83d11b31-2bd8-4dcc-85fd-780dde8f906b','122e4eff-e3be-4565-b07d-1cf847fefa6c','Controlar los medios públicos',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('30882d58-e62f-448c-96c7-d88cdb12b9ef','11111111-1111-1111-1111-111111111202','¿Qué organizaciones defienden los intereses económicos y sociales según la Constitución?','Art. 7','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('31271d84-839d-4775-94c0-fd3bad576a97','30882d58-e62f-448c-96c7-d88cdb12b9ef','Partidos políticos',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('6d8defed-9e09-4635-bfc9-6149e14b5e15','30882d58-e62f-448c-96c7-d88cdb12b9ef','Sindicatos y asociaciones empresariales',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('fd6d9783-3e69-4b56-b9d0-8ca1d78c6c4a','30882d58-e62f-448c-96c7-d88cdb12b9ef','Colegios profesionales',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('7c322784-f2c9-449e-b6bd-cef5485c2b2d','30882d58-e62f-448c-96c7-d88cdb12b9ef','Tribunales',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('cd538984-661a-4aea-a0cf-dbde0e846107','11111111-1111-1111-1111-111111111202','¿Cuál es la misión de las Fuerzas Armadas?','Art. 8','EASY') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('b3c20c39-f2a8-4910-b149-4cf482ba1559','cd538984-661a-4aea-a0cf-dbde0e846107','Legislar',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('a6f4d643-a7a2-4c00-935d-a4bd6863dc7c','cd538984-661a-4aea-a0cf-dbde0e846107','Garantizar la soberanía e integridad territorial',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('419d8074-74f9-4eac-980d-1cc627750653','cd538984-661a-4aea-a0cf-dbde0e846107','Dirigir la política exterior',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('be5d16cb-16b1-4534-992c-2ba46e7942e1','cd538984-661a-4aea-a0cf-dbde0e846107','Administrar justicia',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('5f3f39d5-e352-4f82-909c-ae03b04628ac','11111111-1111-1111-1111-111111111204','¿Qué derecho reconoce el artículo 14?','Art. 14','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('b908fcdb-9850-4a31-bdb5-65df1d1fe775','5f3f39d5-e352-4f82-909c-ae03b04628ac','La libertad de empresa',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('af5814fd-436c-4d2b-ad17-e7979eac0abc','5f3f39d5-e352-4f82-909c-ae03b04628ac','La igualdad ante la ley',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('10fa5fd2-c0dc-4b9d-8deb-d85dfb3ffaa2','5f3f39d5-e352-4f82-909c-ae03b04628ac','El derecho a la huelga',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('fe5ff996-6b3d-4518-ac95-e492bc49ac9a','5f3f39d5-e352-4f82-909c-ae03b04628ac','El derecho a la propiedad',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('dabb1f80-83ac-446e-b253-045ffceff713','11111111-1111-1111-1111-111111111204','¿Qué derecho reconoce el artículo 15?','Art. 15','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('99a03758-b617-45b3-be04-74dac17d4aac','dabb1f80-83ac-446e-b253-045ffceff713','Derecho a la educación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('32c098c7-0534-442b-9060-e46fd84f6010','dabb1f80-83ac-446e-b253-045ffceff713','Derecho a la vida e integridad física',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('66a6578c-059f-4953-9d0f-ec2fb13525b8','dabb1f80-83ac-446e-b253-045ffceff713','Derecho al trabajo',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('5d6b6f82-8a33-400d-bb1f-1371feccef7a','dabb1f80-83ac-446e-b253-045ffceff713','Derecho a la vivienda',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('9ab19f20-9471-4b5d-a5a2-30aaafc31380','11111111-1111-1111-1111-111111111204','¿Qué establece el artículo 16 sobre la confesión del Estado?','Art. 16','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('326f4bc0-2622-4818-b90e-fbc0e0b1a530','9ab19f20-9471-4b5d-a5a2-30aaafc31380','El Estado es confesional',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('1556f627-3438-434b-a589-b4b1c90e5c37','9ab19f20-9471-4b5d-a5a2-30aaafc31380','Ninguna confesión tendrá carácter estatal',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('ffdf1b68-bccb-4f6e-a3d8-d2e9fbb92741','9ab19f20-9471-4b5d-a5a2-30aaafc31380','El Estado es laico estricto',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('014125fa-1715-4ae0-9399-4d26091be14a','9ab19f20-9471-4b5d-a5a2-30aaafc31380','La religión oficial es la católica',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('06b79247-0728-4171-a35e-5d0f2932d6fc','11111111-1111-1111-1111-111111111204','¿Qué derecho protege el artículo 18?','Art. 18','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('bf22c907-0b05-4f56-9c37-393045bf8940','06b79247-0728-4171-a35e-5d0f2932d6fc','Huelga',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('8ffa94c8-2675-4ae2-94d9-d971643e0afc','06b79247-0728-4171-a35e-5d0f2932d6fc','Honor, intimidad y propia imagen',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('23f9e8bb-da6d-46b7-8ea4-ee26edc462f5','06b79247-0728-4171-a35e-5d0f2932d6fc','Educación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('ba6c657c-4855-46ff-9c77-e6446a282163','06b79247-0728-4171-a35e-5d0f2932d6fc','Participación política',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('2727fa2a-98a8-4116-adc0-ab47c91425be','11111111-1111-1111-1111-111111111204','¿Qué derecho regula el artículo 20?','Art. 20','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('89e1ba6e-fff0-42bd-b4fc-2c308fe689a5','2727fa2a-98a8-4116-adc0-ab47c91425be','Libertad sindical',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('b2de6fd1-8497-427b-87c6-2ee0c0235487','2727fa2a-98a8-4116-adc0-ab47c91425be','Libertad de expresión',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('58ba6adf-f531-47e6-81d6-ef7679e22468','2727fa2a-98a8-4116-adc0-ab47c91425be','Libertad de circulación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('2fd9d51b-1cbd-45b1-aad3-2743868fe1a6','2727fa2a-98a8-4116-adc0-ab47c91425be','Libertad de empresa',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('f8eb8aec-f85f-4e31-addd-75e85deeace3','11111111-1111-1111-1111-111111111204','¿Qué derecho recoge el artículo 21?','Art. 21','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('c5714e74-4d0b-45a8-b908-2d34012b9af2','f8eb8aec-f85f-4e31-addd-75e85deeace3','Derecho de reunión pacífica',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('edb173e4-ca47-442c-822b-a4e9ec4ddae6','f8eb8aec-f85f-4e31-addd-75e85deeace3','Derecho a la vivienda',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('cbdbaa10-1786-43c1-8398-484450a6bcd1','f8eb8aec-f85f-4e31-addd-75e85deeace3','Derecho al trabajo',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('60ef4091-8344-4d68-9222-3fd0d1328306','f8eb8aec-f85f-4e31-addd-75e85deeace3','Derecho a la propiedad',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('b296e6a5-36df-4c96-8807-8925cf3e678f','11111111-1111-1111-1111-111111111204','¿Qué derecho regula el artículo 22?','Art. 22','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('07143d12-150e-4bd5-8a63-452a76c5d21f','b296e6a5-36df-4c96-8807-8925cf3e678f','Derecho de asociación',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('38f850e8-6012-4d12-ab14-cb90e3294bfe','b296e6a5-36df-4c96-8807-8925cf3e678f','Derecho a la educación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('d6467a0c-e076-480b-95c5-ed8cb78ba672','b296e6a5-36df-4c96-8807-8925cf3e678f','Derecho al honor',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('4882abe5-9e34-4163-a61f-c4efc2e0ce75','b296e6a5-36df-4c96-8807-8925cf3e678f','Derecho a la tutela judicial',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('6c941b2f-b3cc-44f1-a148-4db75e2b1f4a','11111111-1111-1111-1111-111111111204','¿Qué derecho reconoce el artículo 23?','Art. 23','MEDIUM') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('e9fe466a-77df-43a9-8cec-afd459eb7e28','6c941b2f-b3cc-44f1-a148-4db75e2b1f4a','Propiedad privada',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('bf99d9ef-aa8f-4c34-b00b-2ee96d8f360c','6c941b2f-b3cc-44f1-a148-4db75e2b1f4a','Participar en los asuntos públicos',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('a6f3b6ca-37a3-4f7e-8ab4-c8b96bfb5bc7','6c941b2f-b3cc-44f1-a148-4db75e2b1f4a','Educación obligatoria',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('7441010f-0509-4e18-b678-759283298376','6c941b2f-b3cc-44f1-a148-4db75e2b1f4a','Libertad de empresa',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('c7bd91fd-9785-4b31-b930-2bf8becc3a6f','11111111-1111-1111-1111-111111111204','¿Qué garantiza el artículo 24?','Art. 24','HARD') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('4660b282-e24e-4cee-982c-3229317d78d7','c7bd91fd-9785-4b31-b930-2bf8becc3a6f','Tutela judicial efectiva',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('c6ed598a-0190-406f-95af-bfd64d86f610','c7bd91fd-9785-4b31-b930-2bf8becc3a6f','Libertad de empresa',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('84c3997b-3c8d-4347-9e16-dd9d6ca59d42','c7bd91fd-9785-4b31-b930-2bf8becc3a6f','Libertad de circulación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('6afec254-08ec-480d-b5f2-13c9a4c09226','c7bd91fd-9785-4b31-b930-2bf8becc3a6f','Derecho de huelga',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('a2b3b55e-2805-4fc0-94f1-a2c5d9c31741','11111111-1111-1111-1111-111111111204','¿Qué reconoce el artículo 27?','Art. 27','HARD') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('93c53a70-c272-44f9-bdfe-42fa41dcdc61','a2b3b55e-2805-4fc0-94f1-a2c5d9c31741','Derecho a la educación',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('d888252f-860b-44bd-8ec4-359fe7e9fc0d','a2b3b55e-2805-4fc0-94f1-a2c5d9c31741','Derecho a la vivienda',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('719be3a9-4e81-4216-8df6-df14e842e066','a2b3b55e-2805-4fc0-94f1-a2c5d9c31741','Derecho al trabajo',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('23ca9755-258e-4593-8c4e-2ea81abc2c31','a2b3b55e-2805-4fc0-94f1-a2c5d9c31741','Derecho a la salud',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('7fc81d3d-0040-448e-89c6-f8be5905136e','11111111-1111-1111-1111-111111111205','¿Qué establece el artículo 30?','Art. 30','HARD') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('a8119a2e-466f-4d7c-91e5-9a4c49cd1a89','7fc81d3d-0040-448e-89c6-f8be5905136e','Derecho y deber de defender España',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('396d9c2f-1696-4172-9290-a98e440d9fdc','7fc81d3d-0040-448e-89c6-f8be5905136e','Derecho a la vivienda',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('9a213c49-bc1a-4ff4-8808-a1704b8fd006','7fc81d3d-0040-448e-89c6-f8be5905136e','Derecho a la educación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('22cd9f5e-2cf8-42fe-ac49-0df792799d8e','7fc81d3d-0040-448e-89c6-f8be5905136e','Derecho al trabajo',false) ON CONFLICT DO NOTHING;

INSERT INTO questions(id, unit_id, text, explanation, difficulty) VALUES ('46e7c0be-5f5e-400f-a0e0-952122d42c2e','11111111-1111-1111-1111-111111111205','¿Qué reconoce el artículo 35?','Art. 35','HARD') ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('254a8eba-1e9b-4b4b-8274-3b2b9f4e8547','46e7c0be-5f5e-400f-a0e0-952122d42c2e','Derecho al trabajo',true) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('db01f914-ed42-484d-95ff-415492c07fcb','46e7c0be-5f5e-400f-a0e0-952122d42c2e','Derecho a la vivienda',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('3deef2f2-29b4-4e6c-acdb-74e940c4642f','46e7c0be-5f5e-400f-a0e0-952122d42c2e','Derecho a la educación',false) ON CONFLICT DO NOTHING;
INSERT INTO answers(id, question_id, text, correct) VALUES ('aa0e25c5-0536-4b3d-8779-d067b6aaf683','46e7c0be-5f5e-400f-a0e0-952122d42c2e','Derecho a la salud',false) ON CONFLICT DO NOTHING;
