package com.api.digicell.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "conversations")
public class ConversationDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long conversationId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text)
    private String userName;

    @Field(type = FieldType.Long)
    private Long clientId;

    @Field(type = FieldType.Text)
    private String clientName;

    @Field(type = FieldType.Text)
    private String intent;

    @Field(type = FieldType.Text)
    private List<String> labels;

    @Field(type = FieldType.Text)
    private String chatSummary;

    @Field(type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String startTime;

    @Field(type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String endTime;

    @Field(type = FieldType.Nested)
    private List<ChatMessageDocument> chatHistory;
}