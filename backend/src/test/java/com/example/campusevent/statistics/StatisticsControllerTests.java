package com.example.campusevent.statistics;

import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.event.entity.Event;
import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatisticsControllerTests {

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

    @Test
    void organizerOverviewOnlyCountsOwnActiveNonDeletedData() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long ownOpen = insertEvent("organizer1", "统计-自己的报名中", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3), 10, 2, false);
        Long ownFinished = insertEvent("organizer1", "统计-自己的已结束", now.minusDays(4), now.minusDays(3), now.minusDays(5), 10, 1, false);
        Long ownDeleted = insertEvent("organizer1", "统计-自己的已删除", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 10, 1, true);
        Long adminOpen = insertEvent("admin", "统计-管理员活动", now.plusDays(6), now.plusDays(6).plusHours(2), now.plusDays(5), 10, 1, false);
        insertRegistration(ownOpen, "studentA", "ACTIVE");
        insertRegistration(ownOpen, "disabledUser", "ACTIVE");
        insertRegistration(ownFinished, "studentA", "CANCELLED");
        insertRegistration(ownDeleted, "studentA", "ACTIVE");
        insertRegistration(adminOpen, "studentA", "ACTIVE");

        mockMvc.perform(get("/api/statistics/overview").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(2))
                .andExpect(jsonPath("$.data.totalRegistrations").value(2))
                .andExpect(jsonPath("$.data.openEvents").value(1))
                .andExpect(jsonPath("$.data.finishedEvents").value(1));
    }

    @Test
    void adminOverviewCountsAllNonDeletedEventsAndActiveRegistrations() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long organizerOpen = insertEvent("organizer1", "统计-组织者报名中", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3), 10, 2, false);
        Long adminFinished = insertEvent("admin", "统计-管理员已结束", now.minusDays(4), now.minusDays(3), now.minusDays(5), 10, 1, false);
        Long deleted = insertEvent("admin", "统计-管理员已删除", now.plusDays(6), now.plusDays(6).plusHours(2), now.plusDays(5), 10, 1, true);
        insertRegistration(organizerOpen, "studentA", "ACTIVE");
        insertRegistration(organizerOpen, "disabledUser", "ACTIVE");
        insertRegistration(adminFinished, "studentA", "ACTIVE");
        insertRegistration(deleted, "studentA", "ACTIVE");

        mockMvc.perform(get("/api/statistics/overview").header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(2))
                .andExpect(jsonPath("$.data.totalRegistrations").value(3))
                .andExpect(jsonPath("$.data.openEvents").value(1))
                .andExpect(jsonPath("$.data.finishedEvents").value(1));
    }

    @Test
    void studentAndAnonymousCannotAccessStatistics() throws Exception {
        mockMvc.perform(get("/api/statistics/overview").header("Authorization", bearer("studentA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void statusDistributionIncludesAllVisibleStatusesAndExcludesDeleted() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertEvent("organizer1", "统计-报名中", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3), 10, 1, false);
        insertEvent("organizer1", "统计-满员", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 2, 2, false);
        insertEvent("organizer1", "统计-截止", now.plusDays(6), now.plusDays(6).plusHours(2), now.minusHours(1), 2, 2, false);
        insertEvent("organizer1", "统计-结束", now.minusDays(4), now.minusDays(3), now.minusDays(5), 2, 2, false);
        insertEvent("organizer1", "统计-删除", now.plusDays(7), now.plusDays(7).plusHours(2), now.plusDays(6), 2, 2, true);

        mockMvc.perform(get("/api/statistics/status-distribution").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.status=='REGISTRATION_OPEN')].label", hasItem("报名中")))
                .andExpect(jsonPath("$.data[?(@.status=='REGISTRATION_OPEN')].count", hasItem(1)))
                .andExpect(jsonPath("$.data[?(@.status=='FULL')].count", hasItem(1)))
                .andExpect(jsonPath("$.data[?(@.status=='CLOSED')].count", hasItem(1)))
                .andExpect(jsonPath("$.data[?(@.status=='FINISHED')].count", hasItem(1)))
                .andExpect(jsonPath("$.data[*].status", not(hasItem("DELETED"))));
    }

    @Test
    void eventStatisticsListCalculatesRateRemainingPaginationAndScope() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long first = insertEvent("organizer1", "统计-列表新活动", now.plusDays(8), now.plusDays(8).plusHours(2), now.plusDays(7), 4, 2, false);
        Long second = insertEvent("organizer1", "统计-列表旧活动", now.plusDays(7), now.plusDays(7).plusHours(2), now.plusDays(6), 3, 1, false);
        Long other = insertEvent("admin", "统计-列表他人活动", now.plusDays(9), now.plusDays(9).plusHours(2), now.plusDays(8), 5, 2, false);
        insertEvent("organizer1", "统计-列表删除活动", now.plusDays(10), now.plusDays(10).plusHours(2), now.plusDays(9), 5, 2, true);
        insertRegistration(first, "studentA", "ACTIVE");
        insertRegistration(first, "disabledUser", "ACTIVE");

        mockMvc.perform(get("/api/statistics/events")
                        .param("page", "1")
                        .param("pageSize", "1")
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].eventId").value(first.intValue()))
                .andExpect(jsonPath("$.data.records[0].remainingParticipants").value(2))
                .andExpect(jsonPath("$.data.records[0].registrationRate").value(50.00))
                .andExpect(jsonPath("$.data.records[0].displayStatus").value("REGISTRATION_OPEN"));

        mockMvc.perform(get("/api/statistics/events")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].eventId").value(second.intValue()))
                .andExpect(jsonPath("$.data.records[*].eventId", not(hasItem(other.intValue()))));

        mockMvc.perform(get("/api/statistics/events")
                        .param("pageSize", "100")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(50))
                .andExpect(jsonPath("$.data.records[*].eventId", hasItem(first.intValue())))
                .andExpect(jsonPath("$.data.records[*].eventId", hasItem(second.intValue())))
                .andExpect(jsonPath("$.data.records[*].eventId", hasItem(other.intValue())));
    }

    @Test
    void displayStatusPriorityMatchesEventModule() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long finishedAndFull = insertEvent("organizer1", "统计-结束优先满员", now.minusDays(4), now.minusDays(3), now.minusDays(5), 2, 2, false);
        Long closedAndFull = insertEvent("organizer1", "统计-截止优先满员", now.plusDays(4), now.plusDays(4).plusHours(2), now.minusHours(1), 2, 2, false);
        Long full = insertEvent("organizer1", "统计-满员优先开放", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 2, 2, false);

        mockMvc.perform(get("/api/statistics/events").param("pageSize", "50").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.eventId==" + finishedAndFull + ")].displayStatus", hasItem("FINISHED")))
                .andExpect(jsonPath("$.data.records[?(@.eventId==" + closedAndFull + ")].displayStatus", hasItem("CLOSED")))
                .andExpect(jsonPath("$.data.records[?(@.eventId==" + full + ")].displayStatus", hasItem("FULL")));
    }

    private Long insertEvent(String username, String title, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline, int maxParticipants, int currentParticipants, boolean deleted) {
        Event event = new Event();
        event.setTitle(title);
        event.setCopywriting("统计测试活动文案");
        event.setDescription("统计测试活动介绍");
        event.setLocation("统计测试地点");
        event.setOrganizerId(userMapper.findByUsername(username).getId());
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
            jdbcTemplate.update("INSERT INTO registration (event_id, user_id, status, registration_time) VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP)", eventId, userMapper.findByUsername(username).getId());
        } else {
            jdbcTemplate.update("INSERT INTO registration (event_id, user_id, status, registration_time, cancelled_time) VALUES (?, ?, 'CANCELLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", eventId, userMapper.findByUsername(username).getId());
        }
    }

    private String bearer(String username) {
        return "Bearer " + jwtTokenProvider.createToken(userMapper.findByUsername(username));
    }
}
