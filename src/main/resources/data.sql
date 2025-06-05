-- ---------------------------------------------------------------------------
-- Demo dataset aligned with current entity fields (no max_users, no Conversation.status)
-- ---------------------------------------------------------------------------
TRUNCATE TABLE Conversations;
TRUNCATE TABLE Aliases;
TRUNCATE TABLE Users;
TRUNCATE TABLE Agents;

-- ---------------------------------------------------------------------------
-- Agents
-- ---------------------------------------------------------------------------
INSERT INTO Agents (name, status) VALUES
 ('Alice Agent',  'AVAILABLE'),
 ('Bob Agent',    'BREAK'),
 ('Carol Agent',  'AVAILABLE'),
 ('Dave Agent',   'LOGOUT'),
 ('Eve Agent',    'AVAILABLE'),
 ('Frank Agent',  'BREAK'),
 ('Grace Agent',  'LOGOUT'),
 ('Heidi Agent',  'AVAILABLE'),
 ('Ivan Agent',   'BREAK'),
 ('Judy Agent',   'AVAILABLE');

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------
INSERT INTO Users (name, email, phone, is_assigned) VALUES
 ('User One',   'user1@example.com',  '1000000001', TRUE),
 ('User Two',   'user2@example.com',  '1000000002', TRUE),
 ('User Three', 'user3@example.com',  '1000000003', FALSE),
 ('User Four',  'user4@example.com',  '1000000004', TRUE),
 ('User Five',  'user5@example.com',  '1000000005', FALSE),
 ('User Six',   'user6@example.com',  '1000000006', FALSE),
 ('User Seven', 'user7@example.com',  '1000000007', TRUE),
 ('User Eight', 'user8@example.com',  '1000000008', FALSE),
 ('User Nine',  'user9@example.com',  '1000000009', TRUE),
 ('User Ten',   'user10@example.com', '1000000010', FALSE);

-- ---------------------------------------------------------------------------
-- Aliases
-- ---------------------------------------------------------------------------
INSERT INTO Aliases (alias_key, alias_value) VALUES
 ('wlc', 'Welcome to Digicell!'),
 ('brb', 'Be right back.'),
 ('ty',  'Thank you!'),
 ('np',  'No problem.'),
 ('faq', 'Please check our FAQ at https://digicell.com/faq'),
 ('hrs', 'Our working hours are 9 AM – 5 PM.'),
 ('eta', 'Expected resolution time: 24 hours.'),
 ('oi',  'One instant!'),
 ('hpy', 'Happy to assist you.'),
 ('gn',  'Good night.');

-- ---------------------------------------------------------------------------
-- Conversations with JSON chat history
--  * chat_history column stores nested lists of messages as JSON
--  * Outer list represents conversation sessions
--  * Inner list contains messages within each session
-- ---------------------------------------------------------------------------
INSERT INTO Conversations (user_id, agent_id, start_time, end_time, chat_history) VALUES
-- Conversation 1 (User 1 with Agent 1) - Single session
(1, 1, NOW(), NULL, '[
  [
    {"role":"user","content":"Hi","timestamp":"2024-03-20T10:00:00"},
    {"role":"agent","content":"Hello! How can I help you?","timestamp":"2024-03-20T10:00:05"},
    {"role":"user","content":"I have a billing question","timestamp":"2024-03-20T10:01:00"},
    {"role":"agent","content":"I can help with that. What would you like to know?","timestamp":"2024-03-20T10:01:05"}
  ]
]'),

-- Conversation 2 (User 2 with Agent 2) - Single session
(2, 2, NOW(), NULL, '[
  [
    {"role":"user","content":"Need support with my device","timestamp":"2024-03-20T11:00:00"},
    {"role":"agent","content":"Sure, I can help with that. What seems to be the issue?","timestamp":"2024-03-20T11:00:05"},
    {"role":"user","content":"My phone is not charging","timestamp":"2024-03-20T11:01:00"},
    {"role":"agent","content":"Let me help you troubleshoot that.","timestamp":"2024-03-20T11:01:05"}
  ]
]'),

-- Conversation 3 (User 3 with Agent 1) - Multiple sessions
(3, 1, NOW(), NOW(), '[
  [
    {"role":"user","content":"I have a network issue","timestamp":"2024-03-20T12:00:00"},
    {"role":"agent","content":"I can help you with that. What are you experiencing?","timestamp":"2024-03-20T12:00:05"},
    {"role":"user","content":"Slow internet speed","timestamp":"2024-03-20T12:01:00"},
    {"role":"agent","content":"Let me check your connection.","timestamp":"2024-03-20T12:01:05"}
  ],
  [
    {"role":"user","content":"Is there any update?","timestamp":"2024-03-20T13:00:00"},
    {"role":"agent","content":"Yes, I found the issue. Let me explain.","timestamp":"2024-03-20T13:00:05"}
  ]
]'),

-- Conversation 4 (User 4 with Agent 3) - Single session
(4, 3, NOW(), NULL, '[
  [
    {"role":"user","content":"Hello, I need help with my plan","timestamp":"2024-03-20T14:00:00"},
    {"role":"agent","content":"Hi there! I can help you with your plan. What would you like to know?","timestamp":"2024-03-20T14:00:05"},
    {"role":"user","content":"I want to upgrade","timestamp":"2024-03-20T14:01:00"},
    {"role":"agent","content":"I can show you our available upgrade options.","timestamp":"2024-03-20T14:01:05"}
  ]
]'),

-- Conversation 5 (User 5 with Agent 4) - Multiple sessions
(5, 4, NOW(), NULL, '[
  [
    {"role":"user","content":"I have a question about my bill","timestamp":"2024-03-20T15:00:00"},
    {"role":"agent","content":"I can help you with that. What would you like to know?","timestamp":"2024-03-20T15:00:05"},
    {"role":"user","content":"Why is it higher than usual?","timestamp":"2024-03-20T15:01:00"},
    {"role":"agent","content":"Let me check your billing history.","timestamp":"2024-03-20T15:01:05"}
  ],
  [
    {"role":"user","content":"Did you find anything?","timestamp":"2024-03-20T16:00:00"},
    {"role":"agent","content":"Yes, I found the reason for the increase.","timestamp":"2024-03-20T16:00:05"}
  ]
]'),

-- Conversation 6 (User 6 with Agent 5) - Single session
(6, 5, NOW(), NULL, '[
  [
    {"role":"user","content":"Need help with my account","timestamp":"2024-03-20T17:00:00"},
    {"role":"agent","content":"I can help you with your account. What do you need?","timestamp":"2024-03-20T17:00:05"},
    {"role":"user","content":"Can\'t log in","timestamp":"2024-03-20T17:01:00"},
    {"role":"agent","content":"Let me help you reset your password.","timestamp":"2024-03-20T17:01:05"}
  ]
]'),

-- Conversation 7 (User 7 with Agent 6) - Single session
(7, 6, NOW(), NOW(), '[
  [
    {"role":"user","content":"Hello, I have a technical issue","timestamp":"2024-03-20T18:00:00"},
    {"role":"agent","content":"Hi! I can help with technical issues. What\'s the problem?","timestamp":"2024-03-20T18:00:05"},
    {"role":"user","content":"My app keeps crashing","timestamp":"2024-03-20T18:01:00"},
    {"role":"agent","content":"Let me help you troubleshoot that.","timestamp":"2024-03-20T18:01:05"}
  ]
]'),

-- Conversation 8 (User 8 with Agent 7) - Single session
(8, 7, NOW(), NULL, '[
  [
    {"role":"user","content":"Need information about new plans","timestamp":"2024-03-20T19:00:00"},
    {"role":"agent","content":"I can provide information about our new plans. What are you looking for?","timestamp":"2024-03-20T19:00:05"},
    {"role":"user","content":"Family plans","timestamp":"2024-03-20T19:01:00"},
    {"role":"agent","content":"Let me show you our family plan options.","timestamp":"2024-03-20T19:01:05"}
  ]
]'),

-- Conversation 9 (User 9 with Agent 8) - Single session
(9, 8, NOW(), NULL, '[
  [
    {"role":"user","content":"Hello, I need to change my address","timestamp":"2024-03-20T20:00:00"},
    {"role":"agent","content":"I can help you update your address. What\'s the new address?","timestamp":"2024-03-20T20:00:05"},
    {"role":"user","content":"123 New Street","timestamp":"2024-03-20T20:01:00"},
    {"role":"agent","content":"I\'ll update that for you.","timestamp":"2024-03-20T20:01:05"}
  ]
]'),

-- Conversation 10 (User 10 with Agent 9) - Single session
(10, 9, NOW(), NULL, '[
  [
    {"role":"user","content":"Hi, I want to cancel my service","timestamp":"2024-03-20T21:00:00"},
    {"role":"agent","content":"I\'m sorry to hear that. May I know the reason?","timestamp":"2024-03-20T21:00:05"},
    {"role":"user","content":"Moving to a different provider","timestamp":"2024-03-20T21:01:00"},
    {"role":"agent","content":"I understand. Let me help you with the cancellation process.","timestamp":"2024-03-20T21:01:05"}
  ]
]');

-- ---------------------------------------------------------------------------
-- ChatMessages table for storing individual messages
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ChatMessages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    timestamp DATETIME NOT NULL,
    session_id INT NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES Conversations(conversation_id)
);

-- ---------------------------------------------------------------------------
-- Demo data for ChatMessages
-- ---------------------------------------------------------------------------
INSERT INTO ChatMessages (conversation_id, role, content, timestamp, session_id) VALUES
-- Conversation 1 (User 1 with Agent 1)
(1, 'user', 'Hi', '2024-03-20 10:00:00', 1),
(1, 'agent', 'Hello! How can I help you?', '2024-03-20 10:00:05', 1),
(1, 'user', 'I have a billing question', '2024-03-20 10:01:00', 1),
(1, 'agent', 'I can help with that. What would you like to know?', '2024-03-20 10:01:05', 1),

-- Conversation 2 (User 2 with Agent 2)
(2, 'user', 'Need support with my device', '2024-03-20 11:00:00', 1),
(2, 'agent', 'Sure, I can help with that. What seems to be the issue?', '2024-03-20 11:00:05', 1),
(2, 'user', 'My phone is not charging', '2024-03-20 11:01:00', 1),
(2, 'agent', 'Let me help you troubleshoot that.', '2024-03-20 11:01:05', 1),

-- Conversation 3 (User 3 with Agent 1) - Multiple sessions
(3, 'user', 'I have a network issue', '2024-03-20 12:00:00', 1),
(3, 'agent', 'I can help you with that. What are you experiencing?', '2024-03-20 12:00:05', 1),
(3, 'user', 'Slow internet speed', '2024-03-20 12:01:00', 1),
(3, 'agent', 'Let me check your connection.', '2024-03-20 12:01:05', 1),
-- Second session for Conversation 3
(3, 'user', 'Is there any update?', '2024-03-20 13:00:00', 2),
(3, 'agent', 'Yes, I found the issue. Let me explain.', '2024-03-20 13:00:05', 2),

-- Conversation 4 (User 4 with Agent 3)
(4, 'user', 'Hello, I need help with my plan', '2024-03-20 14:00:00', 1),
(4, 'agent', 'Hi there! I can help you with your plan. What would you like to know?', '2024-03-20 14:00:05', 1),
(4, 'user', 'I want to upgrade', '2024-03-20 14:01:00', 1),
(4, 'agent', 'I can show you our available upgrade options.', '2024-03-20 14:01:05', 1),

-- Conversation 5 (User 5 with Agent 4) - Multiple sessions
(5, 'user', 'I have a question about my bill', '2024-03-20 15:00:00', 1),
(5, 'agent', 'I can help you with that. What would you like to know?', '2024-03-20 15:00:05', 1),
(5, 'user', 'Why is it higher than usual?', '2024-03-20 15:01:00', 1),
(5, 'agent', 'Let me check your billing history.', '2024-03-20 15:01:05', 1),
-- Second session for Conversation 5
(5, 'user', 'Did you find anything?', '2024-03-20 16:00:00', 2),
(5, 'agent', 'Yes, I found the reason for the increase.', '2024-03-20 16:00:05', 2),

-- Conversation 6 (User 6 with Agent 5)
(6, 'user', 'Need help with my account', '2024-03-20 17:00:00', 1),
(6, 'agent', 'I can help you with your account. What do you need?', '2024-03-20 17:00:05', 1),
(6, 'user', 'Can\'t log in', '2024-03-20 17:01:00', 1),
(6, 'agent', 'Let me help you reset your password.', '2024-03-20 17:01:05', 1),

-- Conversation 7 (User 7 with Agent 6)
(7, 'user', 'Hello, I have a technical issue', '2024-03-20 18:00:00', 1),
(7, 'agent', 'Hi! I can help with technical issues. What\'s the problem?', '2024-03-20 18:00:05', 1),
(7, 'user', 'My app keeps crashing', '2024-03-20 18:01:00', 1),
(7, 'agent', 'Let me help you troubleshoot that.', '2024-03-20 18:01:05', 1),

-- Conversation 8 (User 8 with Agent 7)
(8, 'user', 'Need information about new plans', '2024-03-20 19:00:00', 1),
(8, 'agent', 'I can provide information about our new plans. What are you looking for?', '2024-03-20 19:00:05', 1),
(8, 'user', 'Family plans', '2024-03-20 19:01:00', 1),
(8, 'agent', 'Let me show you our family plan options.', '2024-03-20 19:01:05', 1),

-- Conversation 9 (User 9 with Agent 8)
(9, 'user', 'Hello, I need to change my address', '2024-03-20 20:00:00', 1),
(9, 'agent', 'I can help you update your address. What\'s the new address?', '2024-03-20 20:00:05', 1),
(9, 'user', '123 New Street', '2024-03-20 20:01:00', 1),
(9, 'agent', 'I\'ll update that for you.', '2024-03-20 20:01:05', 1),

-- Conversation 10 (User 10 with Agent 9)
(10, 'user', 'Hi, I want to cancel my service', '2024-03-20 21:00:00', 1),
(10, 'agent', 'I\'m sorry to hear that. May I know the reason?', '2024-03-20 21:00:05', 1),
(10, 'user', 'Moving to a different provider', '2024-03-20 21:01:00', 1),
(10, 'agent', 'I understand. Let me help you with the cancellation process.', '2024-03-20 21:01:05', 1); 