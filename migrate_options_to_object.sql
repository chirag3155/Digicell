-- Migration script to convert assistant_options from array to object format
-- This converts the existing JSON array format to JSON object format with questions

-- Backup existing data first
CREATE TABLE IF NOT EXISTS assistant_options_backup AS SELECT * FROM assistant_options;

-- Update the existing records to convert arrays to objects
UPDATE assistant_options 
SET options_json = JSON_OBJECT(
    'Special Offers', 'What are the special offers?',
    'DTH offers', 'What are the DTH offers?',
    'Digital apps', 'What are the digital apps available?',
    'Contact us', 'How can I contact you?',
    'Service plan and packages', 'What service plans and packages are available?',
    'Bestselling mobile devices', 'What are the bestselling mobile devices?',
    'Smart Solutions', 'What smart solutions do you offer?',
    'Live Sports', 'What live sports options are available?'
)
WHERE options_json = '["Special Offers", "DTH offers", "Digital apps", "Contact us", "Service plan and packages", "Bestselling mobile devices", "Smart Solutions", "Live Sports"]';

-- For any other variations, you can add similar UPDATE statements
-- or handle them programmatically in the application

-- Verify the changes
SELECT id, assistant_id, name, options_json FROM assistant_options LIMIT 3; 