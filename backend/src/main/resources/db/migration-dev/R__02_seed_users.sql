-- ============ USERS ============
INSERT INTO users(id, email, password_hash, role, first_name, last_name, occupation) VALUES
                                 ('22222222-2222-2222-2222-222222222222','admin@akadem.ia','$2b$05$ZQaA2oiJaXCVFN540AU1du73.MrLGBMoacIKQ/.IJaDc2samK3eum','ADMIN',NULL,NULL,NULL),
                                 ('33333333-3333-3333-3333-333333333333','student@akadem.ia','$2b$05$ZQaA2oiJaXCVFN540AU1du73.MrLGBMoacIKQ/.IJaDc2samK3eum','STUDENT',NULL,NULL,NULL)
ON CONFLICT DO NOTHING;
