package com.example.campusevent.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusevent.registration.entity.Registration;
import com.example.campusevent.registration.vo.RegistrationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    String REGISTRATION_VIEW_COLUMNS = """
            r.id,
            r.event_id,
            e.title AS event_title,
            e.location AS event_location,
            e.start_time AS event_start_time,
            e.end_time AS event_end_time,
            CASE
                WHEN e.id IS NULL OR e.deleted = 1 THEN 'DELETED'
                WHEN e.end_time <= CURRENT_TIMESTAMP THEN 'FINISHED'
                WHEN e.registration_deadline <= CURRENT_TIMESTAMP THEN 'CLOSED'
                WHEN e.current_participants >= e.max_participants THEN 'FULL'
                ELSE 'REGISTRATION_OPEN'
            END AS event_display_status,
            r.user_id,
            u.username AS student_username,
            u.name AS student_name,
            r.status,
            r.registration_time,
            r.cancelled_time
            """;

    @Select("""
            SELECT id, event_id, user_id, status, registration_time, cancelled_time, created_at, updated_at
            FROM registration
            WHERE event_id = #{eventId} AND user_id = #{userId}
            LIMIT 1
            """)
    Registration findByEventAndUser(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(1)
            FROM registration
            WHERE event_id = #{eventId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int countActiveByEventAndUser(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM registration WHERE event_id = #{eventId} AND status = 'ACTIVE'")
    int countActiveByEventId(@Param("eventId") Long eventId);

    @Insert("""
            INSERT INTO registration (event_id, user_id, status, registration_time, cancelled_time)
            VALUES (#{eventId}, #{userId}, 'ACTIVE', CURRENT_TIMESTAMP, NULL)
            """)
    int insertActive(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Update("""
            UPDATE registration
            SET status = 'ACTIVE',
                registration_time = CURRENT_TIMESTAMP,
                cancelled_time = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE event_id = #{eventId}
              AND user_id = #{userId}
              AND status = 'CANCELLED'
            """)
    int reactivateCancelled(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Update("""
            UPDATE registration
            SET status = 'CANCELLED',
                cancelled_time = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE event_id = #{eventId}
              AND user_id = #{userId}
              AND status = 'ACTIVE'
            """)
    int cancelActive(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT
            ${columns}
            FROM registration r
            JOIN event e ON r.event_id = e.id
            JOIN `user` u ON r.user_id = u.id
            WHERE r.event_id = #{eventId}
            <if test='status != null'>AND r.status = #{status}</if>
            ORDER BY r.registration_time DESC, r.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<RegistrationVO> pageEventRegistrations(
            @Param("eventId") Long eventId,
            @Param("status") String status,
            @Param("limit") long limit,
            @Param("offset") long offset,
            @Param("columns") String columns
    );

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM registration r
            WHERE r.event_id = #{eventId}
            <if test='status != null'>AND r.status = #{status}</if>
            </script>
            """)
    long countEventRegistrations(@Param("eventId") Long eventId, @Param("status") String status);

    @Select("""
            <script>
            SELECT
            ${columns}
            FROM registration r
            JOIN `user` u ON r.user_id = u.id
            LEFT JOIN event e ON r.event_id = e.id
            WHERE r.user_id = #{userId}
            <if test='status != null'>AND r.status = #{status}</if>
            ORDER BY r.registration_time DESC, r.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<RegistrationVO> pageMyRegistrations(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("limit") long limit,
            @Param("offset") long offset,
            @Param("columns") String columns
    );

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM registration r
            WHERE r.user_id = #{userId}
            <if test='status != null'>AND r.status = #{status}</if>
            </script>
            """)
    long countMyRegistrations(@Param("userId") Long userId, @Param("status") String status);
}
