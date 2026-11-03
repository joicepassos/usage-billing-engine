package io.github.joicepassos.billing.shared.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailsAdviceTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(new ProblemDetailsAdvice())
            .addFilters(new RequestIdFilter())
            .build();

    @Test
    void validationFailuresAreProblemDetailsWith422AndTheInvalidFields() throws Exception {
        mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
                        .header(RequestIdFilter.HEADER, "req-12345678")
                        .content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://usage-billing-engine/errors/validation"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.quantity", hasItem("must be greater than 0")))
                .andExpect(jsonPath("$.requestId").value("req-12345678"));
    }

    @Test
    void unexpectedFailuresAreGeneric500sWithoutInternalDetails() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong. Quote the request id when reporting it."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void unsafeIncomingRequestIdsAreReplaced() throws Exception {
        mvc.perform(get("/boom").header(RequestIdFilter.HEADER, "<script>"))
                .andExpect(header().string(RequestIdFilter.HEADER,
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
    }

    record SampleRequest(@NotBlank String name, @Positive int quantity) {
    }

    @RestController
    static class SampleController {

        @PostMapping("/sample")
        SampleRequest create(@Valid @RequestBody SampleRequest request) {
            return request;
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
