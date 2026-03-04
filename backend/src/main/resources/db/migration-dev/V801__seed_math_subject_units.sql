-- ============ SUBJECTS ============
INSERT INTO subjects(id, name, description) VALUES
('00000000-0000-0000-0000-000000000001','Matematicas','Algebra y Aritmetica')
ON CONFLICT DO NOTHING;

-- ============ UNITS ============
INSERT INTO units(id, subject_id, name, order_index) VALUES
 ('00000000-0000-0000-0000-000000000101','00000000-0000-0000-0000-000000000001','Fracciones',1),
 ('00000000-0000-0000-0000-000000000102','00000000-0000-0000-0000-000000000001','Ecuaciones',2)
ON CONFLICT DO NOTHING;




