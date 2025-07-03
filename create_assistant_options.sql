-- Create assistant_options table
CREATE TABLE assistant_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    assistant_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    options_json JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (assistant_id) REFERENCES assistant_configuration(id)
);

-- Insert data from assistant_configuration with JSON options
-- Note: Replace 'tenant_id_column' with the actual tenant column name from assistant_configuration
-- If tenant_id doesn't exist in assistant_configuration, use a default value or add it manually
INSERT INTO assistant_options (assistant_id, name, tenant_id, options_json)
SELECT 
    id as assistant_id,
    name,
    COALESCE(tenant_id, 'default_tenant') as tenant_id,  -- Adjust this based on your table structure
    JSON_ARRAY(
        'Special Offers',
        'DTH offers', 
        'Digital apps',
        'Contact us',
        'Service plan and packages',
        'Bestselling mobile devices',
        'Smart Solutions',
        'Live Sports'
    ) as options_json
FROM assistant_configuration;

-- Verify the data
SELECT 
    id,
    assistant_id,
    name,
    tenant_id,
    JSON_PRETTY(options_json) as formatted_options
FROM assistant_options
LIMIT 5;

-- Query with JOIN to show relationship
SELECT 
    ao.id,
    ao.assistant_id,
    ao.name as assistant_name,
    ao.tenant_id,
    ac.name as original_assistant_name,
    JSON_LENGTH(ao.options_json) as total_options
FROM assistant_options ao
JOIN assistant_configuration ac ON ao.assistant_id = ac.id
LIMIT 5;

-- Query to check specific JSON values
SELECT 
    id,
    assistant_id,
    name,
    tenant_id,
    JSON_EXTRACT(options_json, '$[0]') as first_option,
    JSON_EXTRACT(options_json, '$[3]') as fourth_option,
    JSON_LENGTH(options_json) as total_options
FROM assistant_options;

-- Alternative: If you want the JSON as an object instead of array
-- UPDATE assistant_options 
-- SET options_json = JSON_OBJECT(
--     'categories', JSON_ARRAY(
--         'Special Offers',
--         'DTH offers', 
--         'Digital apps',
--         'Contact us',
--         'Service plan and packages',
--         'Bestselling mobile devices',
--         'Smart Solutions',
--         'Live Sports'
--     )
-- ); 