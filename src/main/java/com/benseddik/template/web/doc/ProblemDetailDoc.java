package com.benseddik.template.web.doc;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Erreur conforme RFC 7807 (Problem Details)")
public class ProblemDetailDoc {
    @Schema(example = "about:blank")
    private String type;
    @Schema(example = "Bad Request")
    private String title;
    @Schema(example = "400")
    private Integer status;
    @Schema(example = "Invalid request content.")
    private String detail;
    @Schema(example = "/api/v1/template")
    private String instance;
}