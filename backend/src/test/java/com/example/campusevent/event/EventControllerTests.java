package com.example.campusevent.event;

import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.event.entity.Event;
import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerTests {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void organizerCreatesEventSuccessfully() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("组织者活动", "大学生活动中心", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("组织者活动"))
                .andExpect(jsonPath("$.data.organizerName").value("活动组织者"))
                .andExpect(jsonPath("$.data.currentParticipants").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.displayStatus").value("REGISTRATION_OPEN"))
                .andExpect(jsonPath("$.data.registeredByCurrentUser").value(false));
    }

    @Test
    void adminCreatesEventSuccessfully() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("管理员活动", "报告厅", 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerName").value("系统管理员"));
    }

    @Test
    void studentCannotCreateUpdateOrDeleteEvent() throws Exception {
        Long id = createEvent("organizer1", "权限活动", 20);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("studentA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("学生活动", "教室", 20)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("studentA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("学生编辑", "教室", 20)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(delete("/api/events/{id}", id)
                        .header("Authorization", bearer("studentA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void anonymousCannotCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("匿名活动", "教室", 20)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void createValidationRejectsBadInput() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("   ", "教室", 20)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("人数非法", "教室", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void timeValidationRejectsInvalidRanges() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("截止非法", "教室", start, start.plusHours(2), start.plusMinutes(10), 20)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("结束非法", "教室", start, start.minusHours(1), start.minusDays(1), 20)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void organizerUpdatesOwnEventSuccessfully() throws Exception {
        Long id = createEvent("organizer1", "待编辑活动", 20);
        ObjectNode changes = (ObjectNode) objectMapper.readTree(validPayload("编辑后的活动", "新地点", 25));
        changes.put("copywriting", "修改后的活动文案");
        changes.put("description", "修改后的活动介绍");

        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value("编辑后的活动"))
                .andExpect(jsonPath("$.data.copywriting").value("修改后的活动文案"))
                .andExpect(jsonPath("$.data.description").value("修改后的活动介绍"))
                .andExpect(jsonPath("$.data.location").value("新地点"));

        mockMvc.perform(get("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value("编辑后的活动"))
                .andExpect(jsonPath("$.data.copywriting").value("修改后的活动文案"))
                .andExpect(jsonPath("$.data.description").value("修改后的活动介绍"))
                .andExpect(jsonPath("$.data.location").value("新地点"));
    }

    @Test
    void organizerCannotUpdateOthersEvent() throws Exception {
        Long id = createEvent("admin", "管理员的活动", 20);

        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("越权编辑", "新地点", 20)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/events/{id}", id)
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("管理员的活动"))
                .andExpect(jsonPath("$.data.location").value("大学生活动中心"));
    }

    @Test
    void adminUpdatesAnyEventSuccessfully() throws Exception {
        Long id = createEvent("organizer1", "组织者活动", 20);

        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("管理员编辑", "报告厅", 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("管理员编辑"));
    }

    @Test
    void deleteIsSoftDeleteAndHiddenFromList() throws Exception {
        Long id = createEvent("organizer1", "待删除活动", 20);

        mockMvc.perform(delete("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk());

        Event deleted = eventMapper.selectById(id);
        assertThat(deleted.getDeleted()).isTrue();

        mockMvc.perform(get("/api/events")
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].id", not(hasItem(id.intValue()))));
    }

    @Test
    void detailReturnsComputedFields() throws Exception {
        Long id = createEvent("organizer1", "详情活动", 4);

        mockMvc.perform(get("/api/events/{id}", id)
                        .header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerName").value("活动组织者"))
                .andExpect(jsonPath("$.data.remainingParticipants").value(4))
                .andExpect(jsonPath("$.data.registrationRate").value(0))
                .andExpect(jsonPath("$.data.displayStatus").value("REGISTRATION_OPEN"));
    }

    @Test
    void endedEventCannotBeUpdated() throws Exception {
        Long id = insertEvent("organizer1", "已结束活动", LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(4), 20, 0);

        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("编辑已结束", "教室", 20)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void maxParticipantsCannotBeLowerThanCurrentParticipants() throws Exception {
        Long id = insertEvent("organizer1", "已有报名活动", LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(2), LocalDateTime.now().plusDays(4), 10, 3);

        mockMvc.perform(put("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("已有报名活动", "教室", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void organizerMineListOnlyReturnsOwnEvents() throws Exception {
        Long own = createEvent("organizer1", "自己的活动", 20);
        Long other = createEvent("admin", "别人的活动", 20);

        mockMvc.perform(get("/api/events?mine=true")
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].id", hasItem(own.intValue())))
                .andExpect(jsonPath("$.data.records[*].id", not(hasItem(other.intValue()))));
    }

    @Test
    void anonymousCannotReadListOrDetail() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/events/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void organizerCannotDeleteOthersButAdminCan() throws Exception {
        Long id = createEvent("admin", "Admin owned", 20);
        mockMvc.perform(delete("/api/events/{id}", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
        Long own = createEvent("organizer1", "Organizer owned", 20);
        mockMvc.perform(delete("/api/events/{id}", own).header("Authorization", bearer("admin")))
                .andExpect(status().isOk());
        assertThat(eventMapper.selectById(own).getDeleted()).isTrue();
        mockMvc.perform(get("/api/events/{id}", own).header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/events/{id}", own).header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON).content(validPayload("Deleted", "Room", 20)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sharedHallAndAdminListIncludeAllOrganizers() throws Exception {
        Long own = createEvent("organizer1", "Shared own", 20);
        Long other = createEvent("admin", "Shared admin", 20);
        for (String user : new String[]{"organizer1", "admin", "studentA"}) {
            mockMvc.perform(get("/api/events").header("Authorization", bearer(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records[*].id", hasItem(own.intValue())))
                    .andExpect(jsonPath("$.data.records[*].id", hasItem(other.intValue())));
        }
    }

    @Test
    void participantsLockTimesButAllowContentEdit() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5).withNano(0);
        Long id = insertEvent("organizer1", "Locked", start, start.plusHours(2), start.minusDays(1), 10, 3);
        mockMvc.perform(put("/api/events/{id}", id).header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload("Changed", "Room", start.plusHours(1), start.plusHours(3), start.minusDays(1), 10)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40900));
        mockMvc.perform(put("/api/events/{id}", id).header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload("Changed", "Room", start, start.plusHours(2), start.minusDays(1), 10)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Changed"));
    }

    @Test
    void statusPriorityAndRateAreCalculatedByBackend() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long full = insertEvent("organizer1", "Full", now.plusDays(2), now.plusDays(3), now.plusDays(1), 3, 3);
        Long closed = insertEvent("organizer1", "Closed", now.plusDays(2), now.plusDays(3), now.minusHours(1), 3, 3);
        Long finished = insertEvent("organizer1", "Finished", now.minusDays(2), now.minusDays(1), now.minusDays(3), 3, 3);
        Long partial = insertEvent("organizer1", "Partial", now.plusDays(2), now.plusDays(3), now.plusDays(1), 3, 1);
        Long[] ids = {full, closed, finished, partial};
        String[] statuses = {"FULL", "CLOSED", "FINISHED", "REGISTRATION_OPEN"};
        for (int i = 0; i < ids.length; i++) {
            mockMvc.perform(get("/api/events/{id}", ids[i]).header("Authorization", bearer("studentA")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.displayStatus").value(statuses[i]));
        }
        mockMvc.perform(get("/api/events/{id}", partial).header("Authorization", bearer("studentA")))
                .andExpect(jsonPath("$.data.remainingParticipants").value(2))
                .andExpect(jsonPath("$.data.registrationRate").value(33.33));
    }

    @Test
    void keywordPaginationUsesSameCountFilterAndCapsPageSize() throws Exception {
        createEvent("organizer1", "Unique phase4 matching", 20);
        createEvent("admin", "Unrelated", 20);
        mockMvc.perform(get("/api/events").param("keyword", "Unique phase4").param("pageSize", "1")
                        .header("Authorization", bearer("studentA")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
        mockMvc.perform(get("/api/events").param("page", "0").param("pageSize", "100")
                        .header("Authorization", bearer("studentA")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(50));
    }

    @Test
    void phase8KeywordSearchMatchesTitleDescriptionAndLocation() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long title = insertEvent("organizer1", "phase8-title-magic", "普通介绍", "普通地点", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 20, 0, false);
        Long description = insertEvent("organizer1", "普通标题", "phase8-description-magic", "普通地点", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 20, 0, false);
        Long location = insertEvent("organizer1", "普通标题", "普通介绍", "phase8-location-magic", now.plusDays(5), now.plusDays(5).plusHours(2), now.plusDays(4), 20, 0, false);

        mockMvc.perform(get("/api/events").param("keyword", " phase8-title-magic ").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(title.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-description-magic").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(description.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-location-magic").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(location.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-no-match").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    void phase8DisplayStatusFiltersAreAppliedBeforePaginationAndCount() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long openA = insertEvent("organizer1", "phase8-status-open-a", "phase8-status", "地点", now.plusDays(8), now.plusDays(8).plusHours(2), now.plusDays(7), 10, 1, false);
        Long openB = insertEvent("organizer1", "phase8-status-open-b", "phase8-status", "地点", now.plusDays(7), now.plusDays(7).plusHours(2), now.plusDays(6), 10, 2, false);
        Long full = insertEvent("organizer1", "phase8-status-full", "phase8-status", "地点", now.plusDays(6), now.plusDays(6).plusHours(2), now.plusDays(5), 3, 3, false);
        Long closed = insertEvent("organizer1", "phase8-status-closed", "phase8-status", "地点", now.plusDays(5), now.plusDays(5).plusHours(2), now.minusHours(1), 10, 1, false);
        Long finished = insertEvent("organizer1", "phase8-status-finished", "phase8-status", "地点", now.minusDays(3), now.minusDays(2), now.minusDays(4), 10, 1, false);
        Long deleted = insertEvent("organizer1", "phase8-status-deleted", "phase8-status", "地点", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3), 10, 1, true);

        mockMvc.perform(get("/api/events").param("keyword", "phase8-status").param("status", "REGISTRATION_OPEN").param("pageSize", "1").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].displayStatus").value("REGISTRATION_OPEN"));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-status").param("status", "FULL").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(full.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-status").param("status", "CLOSED").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(closed.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-status").param("status", "FINISHED").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(finished.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-status").header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(openA.intValue())))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(openB.intValue())))
                .andExpect(jsonPath("$.data.records[*].id", not(hasItem(deleted.intValue()))));
    }

    @Test
    void phase8DeletedAndInvalidStatusRespectPermissions() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Long deleted = insertEvent("organizer1", "phase8-deleted-visible-admin", "phase8-deleted", "地点", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3), 10, 1, true);

        mockMvc.perform(get("/api/events").param("keyword", "phase8-deleted").param("status", "DELETED").header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(deleted.intValue())));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-deleted").param("status", "DELETED").header("Authorization", bearer("organizer1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/events").param("status", "BROKEN").header("Authorization", bearer("studentA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void phase8MineAndRoleBoundariesAreEnforced() throws Exception {
        Long own = createEvent("organizer1", "phase8-mine-own", 20);
        Long adminOwned = createEvent("admin", "phase8-mine-admin", 20);

        mockMvc.perform(get("/api/events").param("keyword", "phase8-mine").param("mine", "true").header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].id", hasItem(own.intValue())))
                .andExpect(jsonPath("$.data.records[*].id", not(hasItem(adminOwned.intValue()))));
        mockMvc.perform(get("/api/events").param("keyword", "phase8-mine").header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].id", hasItem(own.intValue())))
                .andExpect(jsonPath("$.data.records[*].id", hasItem(adminOwned.intValue())));
        mockMvc.perform(get("/api/events").param("mine", "true").header("Authorization", bearer("studentA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void phase8KeywordStatusAndPaginationComposeCorrectly() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertEvent("organizer1", "phase8-combo-open-new", "phase8-combo", "地点", now.plusDays(9), now.plusDays(9).plusHours(2), now.plusDays(8), 10, 1, false);
        Long openSecond = insertEvent("organizer1", "phase8-combo-open-second", "phase8-combo", "地点", now.plusDays(8), now.plusDays(8).plusHours(2), now.plusDays(7), 10, 1, false);
        insertEvent("organizer1", "phase8-combo-full", "phase8-combo", "地点", now.plusDays(7), now.plusDays(7).plusHours(2), now.plusDays(6), 1, 1, false);
        insertEvent("organizer1", "普通组合活动", "普通介绍", "地点", now.plusDays(6), now.plusDays(6).plusHours(2), now.plusDays(5), 10, 1, false);

        mockMvc.perform(get("/api/events")
                        .param("keyword", "phase8-combo")
                        .param("status", "REGISTRATION_OPEN")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", bearer("studentA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(openSecond.intValue()))
                .andExpect(jsonPath("$.data.records[0].displayStatus").value("REGISTRATION_OPEN"));
    }

    @Test
    void softDeletePreservesRegistrationHistory() throws Exception {
        Long id = createEvent("organizer1", "History retained", 20);
        Long studentId = userMapper.findByUsername("studentA").getId();
        jdbcTemplate.update("INSERT INTO registration (event_id, user_id, status) VALUES (?, ?, 'ACTIVE')", id, studentId);
        mockMvc.perform(delete("/api/events/{id}", id).header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk());
        assertThat(eventMapper.selectById(id).getDeleted()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM registration WHERE event_id = ? AND user_id = ?", Integer.class, id, studentId)).isEqualTo(1);
    }

    private Long createEvent(String username, String title, int maxParticipants) throws Exception {
        String response = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(title, "大学生活动中心", maxParticipants)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private Long insertEvent(String username, String title, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline, int maxParticipants, int currentParticipants) {
        Event event = new Event();
        event.setTitle(title);
        event.setCopywriting("测试活动文案");
        event.setDescription("测试活动介绍");
        event.setLocation("测试地点");
        event.setOrganizerId(userMapper.findByUsername(username).getId());
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setRegistrationDeadline(registrationDeadline);
        event.setMaxParticipants(maxParticipants);
        event.setCurrentParticipants(currentParticipants);
        event.setStatus("PUBLISHED");
        event.setDeleted(false);
        eventMapper.insert(event);
        return event.getId();
    }

    private Long insertEvent(String username, String title, String description, String location, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline, int maxParticipants, int currentParticipants, boolean deleted) {
        Event event = new Event();
        event.setTitle(title);
        event.setCopywriting("测试活动文案");
        event.setDescription(description);
        event.setLocation(location);
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

    private String bearer(String username) {
        return "Bearer " + jwtTokenProvider.createToken(userMapper.findByUsername(username));
    }

    private String validPayload(String title, String location, int maxParticipants) {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        return payload(title, location, start, start.plusHours(2), start.minusDays(1), maxParticipants);
    }

    private String payload(String title, String location, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime registrationDeadline, int maxParticipants) {
        return """
                {
                  "title": "%s",
                  "copywriting": "测试活动文案",
                  "description": "测试活动介绍",
                  "location": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "registrationDeadline": "%s",
                  "maxParticipants": %d
                }
                """.formatted(
                title,
                location,
                startTime.format(FORMATTER),
                endTime.format(FORMATTER),
                registrationDeadline.format(FORMATTER),
                maxParticipants
        );
    }

}
