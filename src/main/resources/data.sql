-- Insert User Accounts
INSERT INTO user_accounts (user_name, password_hash, email, phone_number, is_active, status, created_by, created_at, updated_at) VALUES
('john_doe', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'john.doe@digicell.com', '+1234567890', true, 'AVAILABLE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('jane_smith', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'jane.smith@digicell.com', '+1234567891', true, 'AVAILABLE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('mike_wilson', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'mike.wilson@digicell.com', '+1234567892', true, 'BREAK', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('sarah_jones', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'sarah.jones@digicell.com', '+1234567893', true, 'LOGOUT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('alex_brown', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'alex.brown@digicell.com', '+1234567894', true, 'AVAILABLE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Clients
INSERT INTO clients (name, email, phone, is_assigned, created_at, updated_at) VALUES
('Acme Corp', 'contact@acmecorp.com', '+1987654321', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TechStart Inc', 'info@techstart.com', '+1987654322', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Global Services', 'support@globalservices.com', '+1987654323', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Innovate Solutions', 'hello@innovate.com', '+1987654324', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Future Systems', 'contact@futuresystems.com', '+1987654325', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Aliases
INSERT INTO aliases (alias_key, alias_value) VALUES
('wlc', 'Welcome to our service! How can I help you today?'),
('ty', 'Thank you for your patience.'),
('np', 'No problem at all!'),
('brb', 'I will be right back.'),
('gtg', 'Got to go, I will get back to you soon.');

-- Insert Conversations
INSERT INTO conversations (client_id, user_id, intent, start_time, end_time, chat_summary, chat_history) VALUES
(1, 1, 'SUPPORT', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), 'Product features inquiry. Discussed enterprise features and pricing. Demo scheduled.',
'[[{"role": "user", "content": "Hi, I am interested in learning more about your product features.", "timestamp": "2024-03-20T10:00:00"},
    {"role": "assistant", "content": "Hello! I would be happy to tell you about our product features. What specific aspects are you most interested in?", "timestamp": "2024-03-20T10:00:05"},
    {"role": "user", "content": "We are looking for enterprise-level features, particularly around integration capabilities.", "timestamp": "2024-03-20T10:01:00"},
    {"role": "assistant", "content": "Great! Our enterprise plan includes robust API integration, custom webhooks, and extensive documentation. Would you like me to share our pricing plans?", "timestamp": "2024-03-20T10:01:30"},
    {"role": "user", "content": "Yes, please share the pricing details.", "timestamp": "2024-03-20T10:02:00"},
    {"role": "assistant", "content": "I will send you our enterprise pricing brochure. Would you be interested in scheduling a demo to see these features in action?", "timestamp": "2024-03-20T10:02:30"},
    {"role": "user", "content": "Yes, that would be helpful. Can we schedule it for next week?", "timestamp": "2024-03-20T10:03:00"},
    {"role": "assistant", "content": "Absolutely! I will send you a calendar invite for next week.", "timestamp": "2024-03-20T10:03:30"}]]'),

(2, 2, 'SALES', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE), 'Enterprise plan inquiry for 100+ users. Pricing discussed. Contract review scheduled.',
'[[{"role": "user", "content": "Hello, we are a growing tech company with about 100 employees.", "timestamp": "2024-03-20T11:00:00"},
    {"role": "assistant", "content": "Welcome! I understand you are looking for a solution for your growing team. What are your current needs?", "timestamp": "2024-03-20T11:00:05"},
    {"role": "user", "content": "We need a scalable solution that can grow with us. What features do you offer for larger teams?", "timestamp": "2024-03-20T11:01:00"},
    {"role": "assistant", "content": "Our enterprise plan includes advanced team management, custom roles, and priority support. Would you like to see our pricing calculator?", "timestamp": "2024-03-20T11:01:30"},
    {"role": "user", "content": "Yes, please share the pricing for 100+ users.", "timestamp": "2024-03-20T11:02:00"},
    {"role": "assistant", "content": "I will send you our custom pricing proposal. Would you like to review the contract next week?", "timestamp": "2024-03-20T11:02:30"},
    {"role": "user", "content": "Yes, that works for us.", "timestamp": "2024-03-20T11:03:00"},
    {"role": "assistant", "content": "Great! I will schedule a contract review session for next week.", "timestamp": "2024-03-20T11:03:30"}]]'),

(3, 3, 'BILLING', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 45 MINUTE), 'Duplicate payment issue resolved. Refund processed. Payment method updated.',
'[[{"role": "user", "content": "We have an issue with our recent payment. It seems to have been charged twice.", "timestamp": "2024-03-20T12:00:00"},
    {"role": "assistant", "content": "I apologize for the inconvenience. Let me check your transaction history.", "timestamp": "2024-03-20T12:00:05"},
    {"role": "user", "content": "Yes, please verify the charges from last week.", "timestamp": "2024-03-20T12:01:00"},
    {"role": "assistant", "content": "I can see the duplicate charge. I will process a refund for the second charge immediately.", "timestamp": "2024-03-20T12:01:30"},
    {"role": "user", "content": "Thank you. Can you also update our payment method?", "timestamp": "2024-03-20T12:02:00"},
    {"role": "assistant", "content": "Of course. I will help you update your payment information. Could you provide the new card details?", "timestamp": "2024-03-20T12:02:30"},
    {"role": "user", "content": "Here are the new card details: XXXX-XXXX-XXXX-1234", "timestamp": "2024-03-20T12:03:00"},
    {"role": "assistant", "content": "Thank you. I have updated your payment method and processed the refund. You should see the refund in 3-5 business days.", "timestamp": "2024-03-20T12:03:30"}]]'),

(4, 4, 'TECHNICAL', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), 'API integration issues resolved. Authentication fixed. Training scheduled.',
'[[{"role": "user", "content": "We are having issues with the API integration. Getting authentication errors.", "timestamp": "2024-03-20T13:00:00"},
    {"role": "assistant", "content": "I will help you troubleshoot the authentication issues. Can you share the error message?", "timestamp": "2024-03-20T13:00:05"},
    {"role": "user", "content": "Error 401: Invalid credentials", "timestamp": "2024-03-20T13:01:00"},
    {"role": "assistant", "content": "Let me check your API configuration. Are you using the latest API key?", "timestamp": "2024-03-20T13:01:30"},
    {"role": "user", "content": "Yes, we generated a new key last week.", "timestamp": "2024-03-20T13:02:00"},
    {"role": "assistant", "content": "I see the issue. The API endpoint URL has changed. Let me provide you with the updated documentation.", "timestamp": "2024-03-20T13:02:30"},
    {"role": "user", "content": "Can you also help optimize our database queries?", "timestamp": "2024-03-20T13:03:00"},
    {"role": "assistant", "content": "I will analyze your queries and provide optimization suggestions. Would you like to schedule a training session for your team?", "timestamp": "2024-03-20T13:03:30"}]]'),

(5, 5, 'GENERAL', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE), 'Platform overview provided. Security features discussed. Case studies shared.',
'[[{"role": "user", "content": "Hello, we are considering your platform. Can you give us an overview?", "timestamp": "2024-03-20T14:00:00"},
    {"role": "assistant", "content": "Welcome! I would be happy to provide an overview of our platform. What aspects are you most interested in?", "timestamp": "2024-03-20T14:00:05"},
    {"role": "user", "content": "We are particularly concerned about security and compliance.", "timestamp": "2024-03-20T14:01:00"},
    {"role": "assistant", "content": "We are ISO 27001 certified and GDPR compliant. Would you like to see our security documentation?", "timestamp": "2024-03-20T14:01:30"},
    {"role": "user", "content": "Yes, please. Also, tell us about the mobile app.", "timestamp": "2024-03-20T14:02:00"},
    {"role": "assistant", "content": "Our mobile app includes all core features with offline capabilities. I will share our mobile app feature list.", "timestamp": "2024-03-20T14:02:30"},
    {"role": "user", "content": "Great! Can you share some case studies?", "timestamp": "2024-03-20T14:03:00"},
    {"role": "assistant", "content": "I will send you our success stories and case studies right away.", "timestamp": "2024-03-20T14:03:30"}]]');

-- Insert User Organization Permissions
INSERT INTO user_org_permissions (user_id, org_id, role) VALUES
(1, 1, 'ADMIN'),
(2, 1, 'MANAGER'),
(3, 1, 'AGENT'),
(4, 1, 'AGENT'),
(5, 1, 'AGENT');

-- Update some clients to be assigned
UPDATE clients SET is_assigned = true WHERE client_id IN (2, 4);

-- Add some labels to user accounts
UPDATE user_accounts SET labels = '["senior", "technical"]' WHERE user_id = 1;
UPDATE user_accounts SET labels = '["manager", "sales"]' WHERE user_id = 2;
UPDATE user_accounts SET labels = '["junior", "support"]' WHERE user_id = 3;
UPDATE user_accounts SET labels = '["senior", "billing"]' WHERE user_id = 4;
UPDATE user_accounts SET labels = '["junior", "general"]' WHERE user_id = 5; 