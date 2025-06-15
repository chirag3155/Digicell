package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDocument {

    private String content; // The content of the chat message

    private String role; // The role of the message (e.g., user or assistant)

    // Use @JsonFormat to specify how the timestamp should be formatted
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String timestamp; // The timestamp when the message was sent

}
