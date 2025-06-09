# Digicell API Documentation

## Table of Contents
1. [Introduction](#introduction)
2. [Authentication](#authentication)
3. [Base URLs](#base-urls)
4. [API Endpoints](#api-endpoints)
   - [User Management](#user-management)
   - [Agent Management](#agent-management)
   - [Conversation Management](#conversation-management)
   - [Alias Management](#alias-management)
5. [Response Format](#response-format)
6. [Error Handling](#error-handling)
7. [Data Types](#data-types)

## Introduction
This document provides comprehensive documentation for the Digicell API, which enables management of users, agents, conversations, and aliases in a customer service platform.

## Authentication
All endpoints require authentication using Bearer token:
```
Authorization: Bearer <your_token>
```

## Base URLs
- Local: `http://localhost:8080`
- Sandbox: `https://eva-sandbox.bngrenew.com/digicel`

## API Endpoints

### User Management
Base path: `/api/v1/users`

#### Get All Users
```http
GET /api/v1/users
```
**Response:**
```json
{
    "status": 200,
    "message": "Users fetched successfully",
    "data": [
        {
            "userId": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "phone": "+1234567890",
            "isAssigned": true
        }
    ]
}
```

#### Get User by ID
```http
GET /api/v1/users/{user_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "User fetched successfully",
    "data": {
        "userId": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "phone": "+1234567890",
        "isAssigned": true
    }
}
```

#### Get Users by Assignment Status
```http
GET /api/v1/users/assigned?status=true
```
**Response:**
```json
{
    "status": 200,
    "message": "assigned users",
    "data": [
        {
            "userId": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "phone": "+1234567890",
            "isAssigned": true
        }
    ]
}
```

#### Get User Details with Conversations
```http
GET /api/v1/users/{user_id}/details
```
**Response:**
```json
{
    "status": 200,
    "message": "User details fetched successfully",
    "data": {
        "user": {
            "userId": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "phone": "+1234567890",
            "isAssigned": true,
            "createdAt": "2024-03-20T10:00:00",
            "updatedAt": "2024-03-20T10:00:00"
        },
        "conversations": [
            {
                "conversationId": 123,
                "agentId": 456,
                "agentName": "Agent Smith",
                "query": "How can I help you?",
                "startTime": "2024-03-20T18:01:00",
                "endTime": "2024-03-20T18:05:00",
                "chatHistory": [
                    [
                        {
                            "timestamp": "2024-03-20T18:01:00",
                            "content": "Hello, how can I help you?",
                            "role": "agent"
                        },
                        {
                            "timestamp": "2024-03-20T18:01:30",
                            "content": "I have a question about my order",
                            "role": "user"
                        }
                    ]
                ]
            }
        ]
    }
}
```

#### Get User Conversations
```http
GET /api/v1/users/{user_id}/conversations
```
**Response:**
```json
{
    "status": 200,
    "message": "User conversations fetched successfully",
    "data": [
        {
            "conversationId": 123,
            "agentId": 456,
            "agentName": "Agent Smith",
            "query": "How can I help you?",
            "startTime": "2024-03-20T18:01:00",
            "endTime": "2024-03-20T18:05:00"
        }
    ]
}
```

### Agent Management
Base path: `/api/v1/agents`

#### Create Agent
```http
POST /api/v1/agents
```
**Request Body:**
```json
{
    "name": "Agent Smith",
    "email": "agent.smith@example.com",
    "status": "AVAILABLE"
}
```
**Response:**
```json
{
    "status": 201,
    "message": "Agent created successfully",
    "data": {
        "agentId": 1,
        "name": "Agent Smith",
        "email": "agent.smith@example.com",
        "status": "AVAILABLE"
    }
}
```

#### Get All Agents
```http
GET /api/v1/agents
```
**Response:**
```json
{
    "status": 200,
    "message": "Agents retrieved successfully",
    "data": [
        {
            "agentId": 1,
            "name": "Agent Smith",
            "email": "agent.smith@example.com",
            "status": "AVAILABLE"
        }
    ]
}
```

#### Get Agent by ID
```http
GET /api/v1/agents/{id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent retrieved successfully",
    "data": {
        "agentId": 1,
        "name": "Agent Smith",
        "email": "agent.smith@example.com",
        "status": "AVAILABLE"
    }
}
```

#### Update Agent
```http
PUT /api/v1/agents/{id}
```
**Request Body:**
```json
{
    "name": "Agent Smith Updated",
    "email": "agent.smith.updated@example.com",
    "status": "BUSY"
}
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent updated successfully",
    "data": {
        "agentId": 1,
        "name": "Agent Smith Updated",
        "email": "agent.smith.updated@example.com",
        "status": "BUSY"
    }
}
```

#### Update Agent Status
```http
PATCH /api/v1/agents/{id}/status
```
**Request Body:**
```json
{
    "status": "AVAILABLE"
}
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent status updated successfully",
    "data": {
        "agentId": 1,
        "name": "Agent Smith",
        "email": "agent.smith@example.com",
        "status": "AVAILABLE"
    }
}
```

#### Delete Agent
```http
DELETE /api/v1/agents/{id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent deleted successfully",
    "data": null
}
```

#### Get Agent Details
```http
GET /api/v1/agents/{agentId}/details
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent details fetched successfully",
    "data": {
        "agent": {
            "agentId": 1,
            "name": "Agent Smith",
            "email": "agent.smith@example.com",
            "status": "AVAILABLE",
            "avatarUrl": "https://example.com/avatar.jpg",
            "labels": ["support", "sales"],
            "createdAt": "2024-03-20T10:00:00",
            "updatedAt": "2024-03-20T10:00:00"
        },
        "conversations": [
            {
                "conversationId": 123,
                "userId": 456,
                "userName": "John Doe",
                "startTime": "2024-03-20T18:01:00",
                "endTime": "2024-03-20T18:05:00",
                "query": "How can I help you?",
                "chatHistory": [
                    [
                        {
                            "timestamp": "2024-03-20T18:01:00",
                            "content": "Hello, how can I help you?",
                            "role": "agent"
                        },
                        {
                            "timestamp": "2024-03-20T18:01:30",
                            "content": "I have a question about my order",
                            "role": "user"
                        }
                    ]
                ]
            }
        ]
    }
}
```

#### Get Users by Agent
```http
GET /api/v1/agents/{agent_id}/users
```
**Response:**
```json
{
    "status": 200,
    "message": "users for agent",
    "data": [
        {
            "userId": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "phone": "+1234567890",
            "isAssigned": true
        }
    ]
}
```

#### Set Agent Available
```http
PATCH /api/v1/agents/{id}/available
```
**Response:**
```json
{
    "status": 200,
    "message": "Agent status updated successfully",
    "data": {
        "agentId": 1,
        "name": "Agent Smith",
        "email": "agent.smith@example.com",
        "status": "AVAILABLE"
    }
}
```

### Conversation Management
Base path: `/api/conversations`

#### Get All Conversations
```http
GET /api/conversations
```
**Response:**
```json
{
    "status": 200,
    "message": "conversations",
    "data": [
        {
            "conversationId": 123,
            "agentId": 456,
            "agentName": "Agent Smith",
            "query": "How can I help you?",
            "startTime": "2024-03-20T18:01:00",
            "endTime": "2024-03-20T18:05:00"
        }
    ]
}
```

#### Get Conversation by ID
```http
GET /api/conversations/{conversation_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Conversation fetched successfully",
    "data": {
        "conversationId": 123,
        "agentId": 456,
        "agentName": "Agent Smith",
        "query": "How can I help you?",
        "startTime": "2024-03-20T18:01:00",
        "endTime": "2024-03-20T18:05:00"
    }
}
```

#### Get Chat History by User
```http
GET /api/conversations/user/{user_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Chat history retrieved successfully",
    "data": [
        {
            "conversationId": 123,
            "agentId": 456,
            "agentName": "Agent Smith",
            "query": "How can I help you?",
            "startTime": "2024-03-20T18:01:00",
            "endTime": "2024-03-20T18:05:00",
            "chatHistory": [
                [
                    {
                        "timestamp": "2024-03-20T18:01:00",
                        "content": "Hello, how can I help you?",
                        "role": "assistant"
                    },
                    {
                        "timestamp": "2024-03-20T18:02:00",
                        "content": "I need help with my account",
                        "role": "user"
                    }
                ]
            ]
        }
    ]
}
```

#### Create Conversation
```http
POST /api/conversations
```
**Request Body:**
```json
{
    "userId": 1,
    "agentId": 1,
    "query": "How can I help you?",
    "startTime": "2024-03-20T18:01:00",
    "endTime": "2024-03-20T18:05:00",
    "chatHistory": [
        [
            {
                "timestamp": "2024-03-20T18:01:00",
                "content": "Hello, how can I help you?",
                "role": "assistant"
            }
        ]
    ]
}
```
**Response:**
```json
{
    "status": 201,
    "message": "Conversation created successfully",
    "data": {
        "conversationId": 123,
        "agentId": 456,
        "agentName": "Agent Smith",
        "query": "How can I help you?",
        "startTime": "2024-03-20T18:01:00",
        "endTime": "2024-03-20T18:05:00"
    }
}
```

#### Update Conversation
```http
PUT /api/conversations/{conversation_id}
```
**Request Body:**
```json
{
    "chatHistory": [
        [
            {
                "timestamp": "2024-03-20T18:01:00",
                "content": "Hello, how can I help you?",
                "role": "assistant"
            },
            {
                "timestamp": "2024-03-20T18:02:00",
                "content": "I need help with my account",
                "role": "user"
            }
        ]
    ],
    "endTime": "2024-03-20T18:05:00"
}
```
**Response:**
```json
{
    "status": 200,
    "message": "Conversation updated successfully",
    "data": {
        "conversationId": 123,
        "agentId": 456,
        "agentName": "Agent Smith",
        "query": "How can I help you?",
        "startTime": "2024-03-20T18:01:00",
        "endTime": "2024-03-20T18:05:00"
    }
}
```

#### Delete Conversation
```http
DELETE /api/conversations/{conversation_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Conversation with ID 123 has been deleted",
    "data": null
}
```

#### Get Conversations by Agent and User
```http
GET /api/conversations/agent/{agent_id}/user/{user_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "conversations for agent and user",
    "data": [
        {
            "conversationId": 123,
            "agentId": 456,
            "agentName": "Agent Smith",
            "query": "How can I help you?",
            "startTime": "2024-03-20T18:01:00",
            "endTime": "2024-03-20T18:05:00"
        }
    ]
}
```

#### Get Conversation Details
```http
GET /api/conversations/conversation/{conversation_id}/user/{user_id}
```
**Response:**
```json
{
    "status": 200,
    "message": "Conversation details retrieved successfully",
    "data": {
        "conversationId": 123,
        "agentId": 456,
        "agentName": "Agent Smith",
        "query": "How can I help you?",
        "startTime": "2024-03-20T18:01:00",
        "endTime": "2024-03-20T18:05:00",
        "chatHistory": [
            [
                {
                    "timestamp": "2024-03-20T18:01:00",
                    "content": "Hello, how can I help you?",
                    "role": "assistant"
                },
                {
                    "timestamp": "2024-03-20T18:02:00",
                    "content": "I need help with my account",
                    "role": "user"
                }
            ]
        ]
    }
}
```

### Alias Management
Base path: `/api/v1/aliases`

#### Create Alias
```http
POST /api/v1/aliases
```
**Request Body:**
```json
{
    "key": "wlc",
    "value": "Welcome!"
}
```
**Response:**
```json
{
    "status": 201,
    "message": "Alias created successfully",
    "data": {
        "key": "wlc",
        "value": "Welcome!"
    }
}
```

#### Get All Aliases
```http
GET /api/v1/aliases
```
**Response:**
```json
{
    "status": 200,
    "message": "aliases",
    "data": [
        {
            "key": "wlc",
            "value": "Welcome!"
        }
    ]
}
```

#### Get Alias by Key
```http
GET /api/v1/aliases/{key}
```
**Response:**
```json
{
    "status": 200,
    "message": "Alias fetched successfully",
    "data": {
        "key": "wlc",
        "value": "Welcome!"
    }
}
```

#### Update Alias
```http
PUT /api/v1/aliases/{key}
```
**Request Body:**
```json
{
    "value": "Welcome to Digicell!"
}
```
**Response:**
```json
{
    "status": 200,
    "message": "Alias updated successfully",
    "data": {
        "key": "wlc",
        "value": "Welcome to Digicell!"
    }
}
```

#### Delete Alias
```http
DELETE /api/v1/aliases/{key}
```
**Response:**
```json
{
    "status": 200,
    "message": "Alias deleted successfully",
    "data": null
}
```

## Response Format
All API responses follow this format:
```json
{
    "status": 200,
    "message": "Success message",
    "data": {
        // Response data
    }
}
```

## Error Handling
The API uses standard HTTP status codes and returns error responses in this format:
```json
{
    "status": 400,
    "message": "Error message",
    "data": null
}
```

Common error codes:
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 500: Internal Server Error

## Data Types

### User
```json
{
    "userId": "Long",
    "name": "String",
    "email": "String",
    "phone": "String",
    "isAssigned": "Boolean"
}
```

### Agent
```json
{
    "agentId": "Long",
    "name": "String",
    "email": "String",
    "status": "String (AVAILABLE/BUSY/OFFLINE)"
}
```

### Conversation
```json
{
    "conversationId": "Long",
    "agentId": "Long",
    "agentName": "String",
    "query": "String",
    "startTime": "DateTime (ISO-8601)",
    "endTime": "DateTime (ISO-8601)",
    "chatHistory": [
        [
            {
                "timestamp": "String (ISO-8601)",
                "content": "String",
                "role": "String (user/assistant)"
            }
        ]
    ]
}
```

### Alias
```json
{
    "key": "String",
    "value": "String"
}
``` 