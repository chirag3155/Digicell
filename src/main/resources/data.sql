-- Insert Agents
INSERT INTO Agents (name, email, avatar_url, labels, status, created_at, updated_at) VALUES
('John Smith', 'john.smith@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=John', '["sales", "support"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sarah Johnson', 'sarah.j@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah', '["technical", "billing"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mike Wilson', 'mike.w@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Mike', '["support", "sales"]', 'BREAK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Emma Davis', 'emma.d@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Emma', '["technical"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('David Brown', 'david.b@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=David', '["billing", "support"]', 'LOGOUT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lisa Anderson', 'lisa.a@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Lisa', '["sales"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tom Harris', 'tom.h@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Tom', '["technical", "support"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Rachel Green', 'rachel.g@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Rachel', '["billing"]', 'BREAK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chris Martin', 'chris.m@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Chris', '["sales", "technical"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Anna White', 'anna.w@digicell.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Anna', '["support", "billing"]', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Clients
INSERT INTO Clients (name, email, phone, is_assigned, created_at) VALUES
('Robert Taylor', 'robert.t@email.com', '+1-555-0101', true, CURRENT_TIMESTAMP),
('Jennifer Lee', 'jennifer.l@email.com', '+1-555-0102', true, CURRENT_TIMESTAMP),
('Michael Clark', 'michael.c@email.com', '+1-555-0103', false, CURRENT_TIMESTAMP),
('Patricia Moore', 'patricia.m@email.com', '+1-555-0104', true, CURRENT_TIMESTAMP),
('James Wilson', 'james.w@email.com', '+1-555-0105', true, CURRENT_TIMESTAMP),
('Linda Martinez', 'linda.m@email.com', '+1-555-0106', false, CURRENT_TIMESTAMP),
('William Anderson', 'william.a@email.com', '+1-555-0107', true, CURRENT_TIMESTAMP),
('Elizabeth Thomas', 'elizabeth.t@email.com', '+1-555-0108', true, CURRENT_TIMESTAMP),
('Richard Jackson', 'richard.j@email.com', '+1-555-0109', false, CURRENT_TIMESTAMP),
('Susan White', 'susan.w@email.com', '+1-555-0110', true, CURRENT_TIMESTAMP);

-- Insert Aliases
INSERT INTO Aliases (alias_key, alias_value) VALUES
('wlc', 'Welcome to Digicell! How can I assist you today?'),
('ty', 'Thank you for your patience.'),
('brb', 'I''ll be right back with you in a moment.'),
('np', 'No problem at all!'),
('hth', 'Hope this helps!'),
('ttyl', 'Talk to you later!'),
('asap', 'I''ll get back to you as soon as possible.'),
('fyi', 'For your information:'),
('tks', 'Thanks for reaching out!'),
('gl', 'Good luck with everything!');

-- Insert Conversations (with sample chat history)
INSERT INTO Conversations (client_id, agent_id, intent, start_time, end_time, chat_history, chat_summary) VALUES
(1, 1, 'Billing Inquiry', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE), 
'[[{"role":"user","content":"I have a question about my bill","timestamp":"2024-03-20T10:00:00"},
   {"role":"agent","content":"I''d be happy to help with your billing question","timestamp":"2024-03-20T10:00:05"}]]',
'Customer inquired about billing details'),
(2, 2, 'Technical Support', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 45 MINUTE),
'[[{"role":"user","content":"My service is not working","timestamp":"2024-03-20T11:00:00"},
   {"role":"agent","content":"Let me help you troubleshoot","timestamp":"2024-03-20T11:00:05"}]]',
'Technical support provided for service issues'),
(3, 3, 'Product Information', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 20 MINUTE),
'[[{"role":"user","content":"Tell me about your plans","timestamp":"2024-03-20T12:00:00"},
   {"role":"agent","content":"Here are our current plans","timestamp":"2024-03-20T12:00:05"}]]',
'Customer requested information about service plans'),
(4, 4, 'Account Update', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE),
'[[{"role":"user","content":"I need to update my address","timestamp":"2024-03-20T13:00:00"},
   {"role":"agent","content":"I can help with that","timestamp":"2024-03-20T13:00:05"}]]',
'Customer updated account information'),
(5, 5, 'Complaint', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 60 MINUTE),
'[[{"role":"user","content":"I''m not satisfied with the service","timestamp":"2024-03-20T14:00:00"},
   {"role":"agent","content":"I apologize for the inconvenience","timestamp":"2024-03-20T14:00:05"}]]',
'Handled customer complaint'),
(6, 6, 'Sales Inquiry', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 25 MINUTE),
'[[{"role":"user","content":"What are your current promotions?","timestamp":"2024-03-20T15:00:00"},
   {"role":"agent","content":"Here are our latest offers","timestamp":"2024-03-20T15:00:05"}]]',
'Discussed current promotions'),
(7, 7, 'Technical Issue', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 40 MINUTE),
'[[{"role":"user","content":"My device is not connecting","timestamp":"2024-03-20T16:00:00"},
   {"role":"agent","content":"Let''s troubleshoot your connection","timestamp":"2024-03-20T16:00:05"}]]',
'Resolved device connection issue'),
(8, 8, 'Payment Issue', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 35 MINUTE),
'[[{"role":"user","content":"My payment was declined","timestamp":"2024-03-20T17:00:00"},
   {"role":"agent","content":"I''ll help you resolve this","timestamp":"2024-03-20T17:00:05"}]]',
'Assisted with payment problem'),
(9, 9, 'Service Upgrade', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 50 MINUTE),
'[[{"role":"user","content":"I want to upgrade my plan","timestamp":"2024-03-20T18:00:00"},
   {"role":"agent","content":"I''ll help you upgrade","timestamp":"2024-03-20T18:00:05"}]]',
'Processed service upgrade request'),
(10, 10, 'General Inquiry', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 20 MINUTE),
'[[{"role":"user","content":"I have a general question","timestamp":"2024-03-20T19:00:00"},
   {"role":"agent","content":"I''m here to help","timestamp":"2024-03-20T19:00:05"}]]',
'Answered general customer inquiry');

-- Insert Products
INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at) VALUES
('Basic Plan', 'Basic service plan with essential features', 29.99, 1000, NOW(), NOW()),
('Premium Plan', 'Premium service plan with advanced features', 49.99, 500, NOW(), NOW()),
('Enterprise Plan', 'Enterprise service plan with all features', 99.99, 200, NOW(), NOW());

-- Insert Orders
INSERT INTO orders (client_id, total_amount, status, created_at) VALUES
(1, 29.99, 'COMPLETED', NOW()),
(2, 49.99, 'PENDING', NOW()),
(3, 99.99, 'COMPLETED', NOW()),
(4, 29.99, 'CANCELLED', NOW()),
(5, 49.99, 'COMPLETED', NOW());

-- Insert Order Items
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(1, 1, 1, 29.99),
(2, 2, 1, 49.99),
(3, 3, 1, 99.99),
(4, 1, 1, 29.99),
(5, 2, 1, 49.99);

-- Insert Payments
INSERT INTO payments (order_id, amount, payment_method, status, created_at) VALUES
(1, 29.99, 'CREDIT_CARD', 'COMPLETED', NOW()),
(2, 49.99, 'DEBIT_CARD', 'PENDING', NOW()),
(3, 99.99, 'BANK_TRANSFER', 'COMPLETED', NOW()),
(4, 29.99, 'CREDIT_CARD', 'FAILED', NOW()),
(5, 49.99, 'DEBIT_CARD', 'COMPLETED', NOW()); 