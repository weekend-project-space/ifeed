package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpSystemOneModelTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MockRestServiceServer server;
    private CppSystemOneModel model;
    private String responseJson;

    @BeforeEach
    void setUp() throws IOException {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://systemone.test");
        server = MockRestServiceServer.bindTo(builder).build();
        model = new CppSystemOneModel(builder.build(),"jev");
        try (var input = new ClassPathResource("systemone/response.json").getInputStream()) {
            responseJson = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }
    }

    @Test
    void sendsThreeQuestionTypesAndPreservesEveryResponseField() {
        server.expect(requestTo("http://systemone.test/v1/systemone"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "state": "I was charged twice this month, please refund one of them.",
                          "questions": {
                            "refund": {"type": "noul", "instructions": "Is the customer asking for a refund?"},
                            "team": {
                              "type": "choice", "instructions": "Which team should handle this?",
                              "criteria": {"billing": "Charges, refunds, invoices", "technical": "App or site faults",
                                           "fraud": "Suspected unauthorised use"}
                            },
                            "urgency": {
                              "type": "score", "instructions": "How urgent is this?",
                              "criteria": ["Can wait", "Today", "Blocking the customer now"]
                            }
                          }
                        }
                        """, true))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var response = model.call(request());

        assertEquals(".\\models\\jev\\Kev-4B-Q4_K_M.gguf", response.model());
        assertEquals(0.939753776101613, response.noul("refund").noul());
        assertEquals("billing", response.choice("team").choice());
        assertEquals(0.9083047782724814, response.choice("team").probabilities().get("billing"));
        assertEquals(0.862457167408722, response.choice("team").confidence());
        assertEquals(1.11361473647041, response.score("urgency").score());
        assertEquals(0.0, response.score("urgency").confidence());
        assertEquals("Blocking the customer now", response.score("urgency").legend().get("2"));
        assertEquals(0.3719847265828186, response.score("urgency").probabilities().get("2"));
        assertEquals(new SystemOneUsage(114, 0), response.usage());
        assertEquals("refund", response.getResult().questionName());
        assertEquals(response.noul("refund"), response.getResult().getOutput());
        assertEquals(response.getResult(), response.getResult().getMetadata());
        assertEquals(List.of("refund", "team", "urgency"),
                response.getResults().stream().map(SystemOneResult::questionName).toList());
        assertEquals(response.model(), response.getMetadata().getRequired("model"));
        assertEquals(response.usage(), response.getMetadata().getRequired("usage"));
        server.verify();
    }

    @Test
    void primaryResultFollowsRequestOrderEvenIfServerReordersAnswers() throws Exception {
        ObjectNode json = (ObjectNode) mapper.readTree(responseJson);
        ObjectNode answers = (ObjectNode) json.get("answers");
        ObjectNode reordered = mapper.createObjectNode();
        reordered.set("urgency", answers.get("urgency"));
        reordered.set("team", answers.get("team"));
        reordered.set("refund", answers.get("refund"));
        json.set("answers", reordered);
        respond(json.toString());

        var response = model.call(request());

        assertEquals("refund", response.getResult().questionName());
        assertEquals(List.of("refund", "team", "urgency"), new ArrayList<>(response.answers().keySet()));
        server.verify();
    }

    @Test
    void missingOrWrongTypeAccessThrowsInsteadOfReturningZero() {
        respond(responseJson);
        var response = model.call(request());

        assertThrows(IllegalArgumentException.class, () -> response.noul("missing"));
        assertThrows(IllegalArgumentException.class, () -> response.noul("team"));
        assertThrows(UnsupportedOperationException.class, () -> response.answers().clear());
        assertThrows(UnsupportedOperationException.class, () -> response.score("urgency").legend().clear());
    }

    @ParameterizedTest
    @ValueSource(strings = {"refund", "usage", "type", "noul", "confidence", "input_tokens", "score", "unknown_type"})
    void malformedOrIncompleteResponseFailsExplicitly(String field) throws Exception {
        ObjectNode json = (ObjectNode) mapper.readTree(responseJson);
        ObjectNode answers = (ObjectNode) json.get("answers");
        switch (field) {
            case "refund" -> answers.remove("refund");
            case "usage" -> json.remove("usage");
            case "type", "noul" -> ((ObjectNode) answers.get("refund")).remove(field);
            case "confidence", "score" -> ((ObjectNode) answers.get("urgency")).remove(field);
            case "input_tokens" -> ((ObjectNode) json.get("usage")).remove(field);
            case "unknown_type" -> ((ObjectNode) answers.get("refund")).put("type", "invented");
            default -> throw new IllegalArgumentException(field);
        }
        respond(json.toString());

        assertThrows(RuntimeException.class, () -> model.call(request()));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"type", "choice", "legend", "probabilities", "extra_answer"})
    void answersMustMatchRequestedTypesAndCriteria(String field) throws Exception {
        ObjectNode json = (ObjectNode) mapper.readTree(responseJson);
        ObjectNode answers = (ObjectNode) json.get("answers");
        switch (field) {
            case "type" -> answers.set("team", answers.get("refund"));
            case "choice" -> ((ObjectNode) answers.get("team")).put("choice", "invented");
            case "legend" -> ((ObjectNode) answers.get("urgency").get("legend")).put("2", "Invented level");
            case "probabilities" -> ((ObjectNode) answers.get("team").get("probabilities")).remove("fraud");
            case "extra_answer" -> answers.set("extra", answers.get("refund"));
            default -> throw new IllegalArgumentException(field);
        }
        respond(json.toString());

        assertThrows(IllegalStateException.class, () -> model.call(request()));
        server.verify();
    }

    @Test
    void emptyResponseAndHttpFailureAreNotSuccessfulAnswers() {
        server.expect(requestTo("http://systemone.test/v1/systemone")).andRespond(withNoContent());
        server.expect(requestTo("http://systemone.test/v1/systemone")).andRespond(withServerError());

        assertThrows(IllegalStateException.class, () -> model.call(request()));
        assertThrows(RestClientResponseException.class, () -> model.call(request()));
        server.verify();
    }

    @Test
    void ioFailureIsPropagated() {
        server.expect(requestTo("http://systemone.test/v1/systemone"))
                .andRespond(withException(new IOException("Timed out")));

        assertThrows(RestClientException.class, () -> model.call(request()));
        server.verify();
    }

    @Test
    void invalidParametersAreRejectedBeforeHttpCall() {
        assertThrows(IllegalArgumentException.class, () -> new SystemOneRequest(" ", request().questions()));
        assertThrows(IllegalArgumentException.class, () -> new SystemOneRequest("State", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SystemOneQuestion.noul(" "));
        assertThrows(IllegalArgumentException.class, () -> SystemOneQuestion.choice("Team?", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SystemOneQuestion.choice("Team?", Map.of(" ", "Description")));
        assertThrows(IllegalArgumentException.class, () -> SystemOneQuestion.score("Urgency?", List.of()));
        assertThrows(IllegalArgumentException.class, () -> SystemOneQuestion.score("Urgency?", List.of(" ")));
        assertThrows(IllegalArgumentException.class, () -> model.call(null));
        assertThrows(IllegalArgumentException.class, () ->
                new CppSystemOneModel("http://systemone.test","jev", Duration.ZERO, Duration.ofSeconds(30)));
        server.verify();
    }

    @Test
    void questionCollectionsAreCopiedAndScoreOrderIsPreserved() {
        Map<String, String> choices = new LinkedHashMap<>();
        choices.put("billing", "Charges");
        var choice = SystemOneQuestion.choice("Which team?", choices);
        choices.clear();
        List<String> levels = new ArrayList<>(List.of("Low", "High"));
        var score = SystemOneQuestion.score("Urgency?", levels);
        levels.clear();
        Map<String, SystemOneQuestion> questions = new LinkedHashMap<>();
        questions.put("team", choice);
        questions.put("urgency", score);
        var request = new SystemOneRequest("State", questions);
        questions.clear();

        assertEquals(Map.of("billing", "Charges"), choice.criteria());
        assertEquals(List.of("Low", "High"), score.criteria());
        assertEquals(List.of("team", "urgency"), new ArrayList<>(request.questions().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> request.questions().clear());
    }

    private void respond(String body) {
        server.expect(requestTo("http://systemone.test/v1/systemone"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private SystemOneRequest request() {
        Map<String, SystemOneQuestion> questions = new LinkedHashMap<>();
        questions.put("refund", SystemOneQuestion.noul("Is the customer asking for a refund?"));
        questions.put("team", SystemOneQuestion.choice("Which team should handle this?",
                Map.of("billing", "Charges, refunds, invoices", "technical", "App or site faults",
                        "fraud", "Suspected unauthorised use")));
        questions.put("urgency", SystemOneQuestion.score("How urgent is this?",
                List.of("Can wait", "Today", "Blocking the customer now")));
        return new SystemOneRequest("I was charged twice this month, please refund one of them.", questions);
    }
}
