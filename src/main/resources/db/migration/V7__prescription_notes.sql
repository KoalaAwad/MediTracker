-- Free-text instructions the user writes for themselves, e.g. "take with food"
ALTER TABLE prescription ADD COLUMN notes VARCHAR(500);
