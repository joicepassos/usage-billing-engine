package io.github.joicepassos.billing.metering.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
@SecurityRequirement(name = "apiKey")
class EventController {

    private final EventIngestion ingestion;

    EventController(EventIngestion ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping
    @Operation(summary = "Send a batch of up to 500 usage events",
            description = "Idempotent per transactionId. Each event gets its own result: accepted, duplicate "
                    + "(the original result is returned), conflict (409: same id, different payload) or rejected (422).")
    @ApiResponse(responseCode = "202", description = "At least one event was accepted")
    @ApiResponse(responseCode = "200", description = "No new event was accepted (for example, a retried batch)")
    @ApiResponse(responseCode = "422", description = "The batch does not follow the schema; nothing was stored")
    ResponseEntity<IngestionResult> ingest(@Valid @RequestBody IngestionRequests.EventBatch batch) {
        IngestionResult result = ingestion.ingest(batch.events());
        return ResponseEntity.status(result.accepted() > 0 ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result);
    }
}
