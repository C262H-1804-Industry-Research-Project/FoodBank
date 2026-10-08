package foodbank.webapp.api;

import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    private ResultActions login(String email, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "role": "%s"}
                        """.formatted(email, password, role)));
    }

    @Test
    void recipientCanLogInAndUseTheToken() throws Exception {
        Recipient recipient = createRecipient();
        User user = userRepository.findById(recipient.getUserId()).orElseThrow();

        String response = login(user.getEmail(), PASSWORD, "recipient")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.user.role").value("recipient"))
                .andExpect(jsonPath("$.user.name").value("Test Recipient"))
                .andReturn().getResponse().getContentAsString();

        String token = response.replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        Recipient recipient = createRecipient();
        User user = userRepository.findById(recipient.getUserId()).orElseThrow();

        login(user.getEmail(), "wrong-password", "recipient")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIsUnauthorized() throws Exception {
        Recipient recipient = createRecipient();
        User user = userRepository.findById(recipient.getUserId()).orElseThrow();

        login(user.getEmail(), PASSWORD, "donor")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredRecipientCanLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/register-recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Person", "phone": "0400111222",
                                 "email": "new.person@example.com", "password": "%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk());

        login("new.person@example.com", PASSWORD, "recipient")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("New Person"));
    }

    @Test
    void loggedOutTokenIsRejected() throws Exception {
        Recipient recipient = createRecipient();
        String token = recipientToken(recipient);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/requests").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
