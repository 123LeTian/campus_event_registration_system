package com.example.campusevent.registration;

import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.event.entity.Event;
import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.user.entity.User;
import com.example.campusevent.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studentRegistersSuccessfullyAndCountsIncrease() throws Exception {
        Long id = insertEvent("organizer1", "可报名活动", 2, 0);

        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentParticipants").value(1))
                .andExpect(jsonPath("$.data.remainingParticipants").value(1))
                .andExpect(jsonPath("$.data.registrationRate").value(50.00))
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(true));
        assertConsistent(id);
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        Long id = insertEvent("organizer1", "重复报名活动", 2, 0);
        register(id, "studentA");

        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("您已经报名该活动"));
        assertParticipants(id, 1);
        assertConsistent(id);
    }

    @Test
    void organizerAdminAndAnonymousCannotRegister() throws Exception {
        Long id = insertEvent("organizer1", "权限报名活动", 3, 0);

        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/events/{id}/registrations", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
        assertParticipants(id, 0);
    }

    @Test
    void fullClosedEndedAndDeletedEventsCannotBeRegistered() throws Exception {
        Long full = insertEvent("organizer1", "满员活动", 1, 1);
        Long closed = insertEvent("organizer1", "截止活动", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3), LocalDateTime.now().minusHours(1), 2, 0, false);
        Long ended = insertEvent("organizer1", "结束活动", LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(4), 2, 0, false);
        Long deleted = insertEvent("organizer1", "删除活动", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1), 2, 0, true);

        mockMvc.perform(post("/api/events/{id}/registrations", full).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("活动名额已满"));
        mockMvc.perform(post("/api/events/{id}/registrations", closed).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("报名已截止"));
        mockMvc.perform(post("/api/events/{id}/registrations", ended).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("活动已结束，不能报名"));
        mockMvc.perform(post("/api/events/{id}/registrations", deleted).header("Authorization", bearer("studentA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        assertParticipants(full, 1);
        assertParticipants(closed, 0);
        assertParticipants(ended, 0);
        assertParticipants(deleted, 0);
    }

    @Test
    void cancelRegistrationDecreasesCounts() throws Exception {
        Long id = insertEvent("organizer1", "取消活动", 2, 0);
        register(id, "studentA");

        mockMvc.perform(delete("/api/events/{id}/registrations/me", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentParticipants").value(0))
                .andExpect(jsonPath("$.data.remainingParticipants").value(2))
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(false));
        assertConsistent(id);
    }

    @Test
    void duplicateCancelReturnsConflictAndCountUnchanged() throws Exception {
        Long id = insertEvent("organizer1", "重复取消活动", 2, 0);
        register(id, "studentA");
        cancel(id, "studentA");

        mockMvc.perform(delete("/api/events/{id}/registrations/me", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("当前没有有效报名"));
        assertParticipants(id, 0);
        assertConsistent(id);
    }

    @Test
    void endedOrDeletedEventCannotBeCancelled() throws Exception {
        Long ended = insertEvent("organizer1", "结束取消活动", LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(4), 2, 1, false);
        Long deleted = insertEvent("organizer1", "删除取消活动", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1), 2, 1, true);
        insertRegistration(ended, "studentA", "ACTIVE");
        insertRegistration(deleted, "studentA", "ACTIVE");

        mockMvc.perform(delete("/api/events/{id}/registrations/me", ended).header("Authorization", bearer("studentA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("活动已结束，不能取消报名"));
        mockMvc.perform(delete("/api/events/{id}/registrations/me", deleted).header("Authorization", bearer("studentA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        assertParticipants(ended, 1);
        assertParticipants(deleted, 1);
    }

    @Test
    void cancelThenRegisterAgainReusesSeatAndActiveFlag() throws Exception {
        Long id = insertEvent("organizer1", "重新报名活动", 2, 0);
        register(id, "studentA");
        cancel(id, "studentA");

        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentParticipants").value(1))
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(true));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM registration WHERE event_id = ? AND user_id = ?", Integer.class, id, userId("studentA"))).isEqualTo(1);
        assertConsistent(id);
    }

    @Test
    void cancelledSeatCanBeTakenByAnotherStudent() throws Exception {
        Long id = insertEvent("organizer1", "名额恢复活动", 1, 0);
        register(id, "studentA");
        cancel(id, "studentA");

        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer("disabledUser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentParticipants").value(1))
                .andExpect(jsonPath("$.data.displayStatus").value("FULL"));
        assertConsistent(id);
    }

    @Test
    void registeredByCurrentUserIsAccurateInDetail() throws Exception {
        Long id = insertEvent("organizer1", "详情报名状态", 2, 0);
        register(id, "studentA");

        mockMvc.perform(get("/api/events/{id}", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(true));
        mockMvc.perform(get("/api/events/{id}", id).header("Authorization", bearer("disabledUser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(false));
        mockMvc.perform(get("/api/events/{id}", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(false));
    }

    @Test
    void organizerViewsOwnEventRegistrationList() throws Exception {
        Long id = insertEvent("organizer1", "组织者名单活动", 3, 1);
        insertRegistration(id, "studentA", "ACTIVE");
        insertRegistration(id, "disabledUser", "CANCELLED");

        mockMvc.perform(get("/api/events/{id}/registrations", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].eventTitle").value("组织者名单活动"))
                .andExpect(jsonPath("$.data.records[0].studentUsername").value("studentA"))
                .andExpect(jsonPath("$.data.records[0].studentName").value("学生A"))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));
    }

    @Test
    void organizerCannotViewOtherEventRegistrationListButAdminCan() throws Exception {
        Long id = insertEvent("admin", "管理员名单活动", 2, 1);
        insertRegistration(id, "studentA", "ACTIVE");

        mockMvc.perform(get("/api/events/{id}/registrations", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/events/{id}/registrations", id).header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].studentUsername").value("studentA"));
    }

    @Test
    void studentAndAnonymousCannotViewEventRegistrationList() throws Exception {
        Long id = insertEvent("organizer1", "名单权限活动", 2, 0);

        mockMvc.perform(get("/api/events/{id}/registrations", id).header("Authorization", bearer("studentA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/events/{id}/registrations", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void eventRegistrationListRejectsMissingDeletedAndInvalidStatus() throws Exception {
        Long deleted = insertEvent("organizer1", "已删除名单活动", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1), 2, 0, true);

        mockMvc.perform(get("/api/events/{id}/registrations", 999999L).header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(get("/api/events/{id}/registrations", deleted).header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(get("/api/events/{id}/registrations", deleted).param("status", "BAD").header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound());

        Long id = insertEvent("organizer1", "非法状态名单活动", 2, 0);
        mockMvc.perform(get("/api/events/{id}/registrations", id).param("status", "BAD").header("Authorization", bearer("admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void eventRegistrationStatusFiltersAndPaginationAreCorrect() throws Exception {
        Long id = insertEvent("organizer1", "名单筛选活动", 3, 1);
        insertRegistration(id, "studentA", "ACTIVE");
        insertRegistration(id, "disabledUser", "CANCELLED");

        mockMvc.perform(get("/api/events/{id}/registrations", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));
        mockMvc.perform(get("/api/events/{id}/registrations", id).param("status", "CANCELLED").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("CANCELLED"));
        mockMvc.perform(get("/api/events/{id}/registrations", id).param("status", "ALL").param("pageSize", "1").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    void studentViewsOnlyOwnRegistrationsWithEventFields() throws Exception {
        Long own = insertEvent("organizer1", "我的报名活动", 3, 1);
        Long other = insertEvent("organizer1", "别人的报名活动", 3, 1);
        insertRegistration(own, "studentA", "ACTIVE");
        insertRegistration(other, "disabledUser", "ACTIVE");

        mockMvc.perform(get("/api/my/registrations").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].eventTitle").value("我的报名活动"))
                .andExpect(jsonPath("$.data.records[0].eventLocation").value("测试地点"))
                .andExpect(jsonPath("$.data.records[0].eventStartTime").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].eventEndTime").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].eventDisplayStatus").value("REGISTRATION_OPEN"))
                .andExpect(jsonPath("$.data.records[0].studentUsername").value("studentA"))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));
    }

    @Test
    void myRegistrationStatusFiltersAndPaginationAreCorrect() throws Exception {
        Long active = insertEvent("organizer1", "我的有效报名", 3, 1);
        Long cancelled = insertEvent("organizer1", "我的取消报名", 3, 0);
        insertRegistration(active, "studentA", "ACTIVE");
        insertRegistration(cancelled, "studentA", "CANCELLED");

        mockMvc.perform(get("/api/my/registrations").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));
        mockMvc.perform(get("/api/my/registrations").param("status", "CANCELLED").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("CANCELLED"));
        mockMvc.perform(get("/api/my/registrations").param("status", "ALL").param("pageSize", "1").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    void organizerAndAdminCannotAccessMyRegistrations() throws Exception {
        mockMvc.perform(get("/api/my/registrations").header("Authorization", bearer("organizer1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/my/registrations").header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void cancelledRegistrationAppearsCancelledInMyRegistrations() throws Exception {
        Long id = insertEvent("organizer1", "取消后我的报名", 2, 0);
        register(id, "studentA");
        cancel(id, "studentA");

        mockMvc.perform(get("/api/my/registrations").param("status", "CANCELLED").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].eventTitle").value("取消后我的报名"))
                .andExpect(jsonPath("$.data.records[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.records[0].cancelledTime").isNotEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentLastSeatDoesNotOversell() throws Exception {
        Long id = insertEvent("organizer1", "最后名额并发", 1, 0);
        try {
            List<Integer> statuses = runConcurrently(
                    () -> responseStatus(post("/api/events/" + id + "/registrations").header("Authorization", bearer("studentA"))),
                    () -> responseStatus(post("/api/events/" + id + "/registrations").header("Authorization", bearer("disabledUser")))
            );

            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());
            assertParticipants(id, 1);
            assertConsistent(id);
        } finally {
            cleanupEvent(id);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameUserDuplicateRegistrationOnlySucceedsOnce() throws Exception {
        Long id = insertEvent("organizer1", "同用户并发", 2, 0);
        try {
            List<Integer> statuses = runConcurrently(
                    () -> responseStatus(post("/api/events/" + id + "/registrations").header("Authorization", bearer("studentA"))),
                    () -> responseStatus(post("/api/events/" + id + "/registrations").header("Authorization", bearer("studentA")))
            );

            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());
            assertParticipants(id, 1);
            assertConsistent(id);
        } finally {
            cleanupEvent(id);
        }
    }

    private void register(Long id, String username) throws Exception {
        mockMvc.perform(post("/api/events/{id}/registrations", id).header("Authorization", bearer(username)))
                .andExpect(status().isOk());
    }

    private void cancel(Long id, String username) throws Exception {
        mockMvc.perform(delete("/api/events/{id}/registrations/me", id).header("Authorization", bearer(username)))
                .andExpect(status().isOk());
    }

    private Long insertEvent(String username, String title, int maxParticipants, int currentParticipants) {
        return insertEvent(
                username,
                title,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(5).plusHours(2),
                LocalDateTime.now().plusDays(4),
                maxParticipants,
                currentParticipants,
                false
        );
    }

    private Long insertEvent(String username, String title, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline, int maxParticipants, int currentParticipants, boolean deleted) {
        Event event = new Event();
        event.setTitle(title);
        event.setCopywriting("测试活动文案");
        event.setDescription("测试活动介绍");
        event.setLocation("测试地点");
        event.setOrganizerId(userId(username));
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setRegistrationDeadline(registrationDeadline);
        event.setMaxParticipants(maxParticipants);
        event.setCurrentParticipants(currentParticipants);
        event.setStatus("PUBLISHED");
        event.setDeleted(deleted);
        eventMapper.insert(event);
        return event.getId();
    }

    private void insertRegistration(Long eventId, String username, String status) {
        if ("ACTIVE".equals(status)) {
            jdbcTemplate.update("INSERT INTO registration (event_id, user_id, status, registration_time) VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP)", eventId, userId(username));
        } else {
            jdbcTemplate.update("INSERT INTO registration (event_id, user_id, status, registration_time, cancelled_time) VALUES (?, ?, 'CANCELLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", eventId, userId(username));
        }
    }

    private String bearer(String username) {
        User user = userMapper.findByUsername(username);
        return "Bearer " + jwtTokenProvider.createToken(user);
    }

    private Long userId(String username) {
        return userMapper.findByUsername(username).getId();
    }

    private void assertParticipants(Long eventId, int expected) {
        Event event = eventMapper.selectById(eventId);
        assertThat(event.getCurrentParticipants()).isEqualTo(expected);
    }

    private void assertConsistent(Long eventId) {
        Integer active = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM registration WHERE event_id = ? AND status = 'ACTIVE'", Integer.class, eventId);
        Integer current = jdbcTemplate.queryForObject("SELECT current_participants FROM event WHERE id = ?", Integer.class, eventId);
        assertThat(current).isEqualTo(active);
    }

    private void cleanupEvent(Long eventId) {
        jdbcTemplate.update("DELETE FROM registration WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM event WHERE id = ?", eventId);
    }

    private int responseStatus(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        var response = mockMvc.perform(builder).andReturn().getResponse();
        String content = response.getContentAsString();
        if (!content.isBlank()) {
            objectMapper.readTree(content);
        }
        return response.getStatus();
    }

    @SafeVarargs
    private List<Integer> runConcurrently(CheckedSupplier<Integer>... tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = java.util.Arrays.stream(tasks)
                    .map(task -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return task.get();
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new java.util.ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
