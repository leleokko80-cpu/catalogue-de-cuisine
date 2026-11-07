package com.recipecatalog.backend.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthError(String message, Map<String, String> errors) {
}
