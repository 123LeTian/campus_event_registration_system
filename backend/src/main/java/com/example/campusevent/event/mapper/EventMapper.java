package com.example.campusevent.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusevent.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EventMapper extends BaseMapper<Event> {

    String EVENT_COLUMNS = "e.id, e.title, e.copywriting, e.description, e.location, e.organizer_id, e.start_time, e.end_time, "
            + "e.registration_deadline, e.max_participants, e.current_participants, e.status, e.deleted, "
            + "e.created_at, e.updated_at, u.name AS organizer_name";

    String DISPLAY_STATUS_CASE = """
            CASE
                WHEN e.deleted = 1 THEN 'DELETED'
                WHEN e.end_time &lt;= CURRENT_TIMESTAMP THEN 'FINISHED'
                WHEN e.registration_deadline &lt;= CURRENT_TIMESTAMP THEN 'CLOSED'
                WHEN e.current_participants >= e.max_participants THEN 'FULL'
                ELSE 'REGISTRATION_OPEN'
            END
            """;

    @Select("SELECT " + EVENT_COLUMNS + " FROM event e LEFT JOIN `user` u ON e.organizer_id = u.id WHERE e.id = #{id} LIMIT 1")
    Event findDetailById(@Param("id") Long id);

    @Select("SELECT " + EVENT_COLUMNS + " FROM event e LEFT JOIN `user` u ON e.organizer_id = u.id WHERE e.id = #{id} FOR UPDATE")
    Event findDetailByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE event
            SET current_participants = current_participants + 1
            WHERE id = #{id}
              AND deleted = 0
              AND status = 'PUBLISHED'
              AND current_participants < max_participants
              AND registration_deadline > CURRENT_TIMESTAMP
              AND end_time > CURRENT_TIMESTAMP
            """)
    int increaseParticipantsIfAvailable(@Param("id") Long id);

    @Update("""
            UPDATE event
            SET current_participants = current_participants - 1
            WHERE id = #{id}
              AND current_participants > 0
            """)
    int decreaseParticipantsIfPositive(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, title, copywriting, description, location, organizer_id, start_time, end_time,
                   registration_deadline, max_participants, current_participants, status, deleted,
                   created_at, updated_at, organizer_name
            FROM (
                SELECT
                    e.id, e.title, e.copywriting, e.description, e.location, e.organizer_id, e.start_time, e.end_time,
                    e.registration_deadline, e.max_participants, e.current_participants, e.status, e.deleted,
                    e.created_at, e.updated_at, u.name AS organizer_name,
                    """ + DISPLAY_STATUS_CASE + """
                    AS display_status
                FROM event e LEFT JOIN `user` u ON e.organizer_id = u.id
                WHERE 1 = 1
                <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
                <if test='keyword != null and keyword != ""'>
                AND (e.title LIKE CONCAT('%', #{keyword}, '%') OR e.copywriting LIKE CONCAT('%', #{keyword}, '%') OR e.description LIKE CONCAT('%', #{keyword}, '%') OR e.location LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            ) filtered
            WHERE 1 = 1
            <if test='status != null'>AND filtered.display_status = #{status}</if>
            <if test='includeDeleted == false'>AND filtered.deleted = 0</if>
            ORDER BY filtered.start_time DESC, filtered.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Event> pageEvents(
            @Param("organizerId") Long organizerId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM (
                SELECT e.deleted,
                    """ + DISPLAY_STATUS_CASE + """
                    AS display_status
                FROM event e
                WHERE 1 = 1
                <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
                <if test='keyword != null and keyword != ""'>
                AND (e.title LIKE CONCAT('%', #{keyword}, '%') OR e.copywriting LIKE CONCAT('%', #{keyword}, '%') OR e.description LIKE CONCAT('%', #{keyword}, '%') OR e.location LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            ) filtered
            WHERE 1 = 1
            <if test='status != null'>AND filtered.display_status = #{status}</if>
            <if test='includeDeleted == false'>AND filtered.deleted = 0</if>
            </script>
            """)
    long countEvents(
            @Param("organizerId") Long organizerId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("includeDeleted") boolean includeDeleted
    );
}
