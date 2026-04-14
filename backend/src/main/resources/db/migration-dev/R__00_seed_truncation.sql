-- Limpieza en orden seguro (respetando FKs)
truncate table exam_attempt_answers cascade;
truncate table exam_attempts cascade;
truncate table flashcard_review_log cascade;
truncate table flashcard_reviews cascade;
truncate table flashcards cascade;
truncate table answers cascade;
truncate table questions cascade;
truncate table units cascade;
truncate table subjects cascade;
truncate table syllabuses cascade;
truncate table users cascade;