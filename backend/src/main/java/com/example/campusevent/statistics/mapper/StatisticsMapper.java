package com.example.campusevent.statistics.mapper;

import com.example.campusevent.event.mapper.EventMapper;
import com.example.campusevent.statistics.vo.EventStatisticsVO;
import com.example.campusevent.statistics.vo.StatusDistributionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM event e
            WHERE e.deleted = 0
            <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
            </script>
            """)
    long countEvents(@Param("organizerId") Long organizerId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM registration r
            JOIN event e ON r.event_id = e.id
            WHERE e.deleted = 0
              AND r.status = 'ACTIVE'
            <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
            </script>
            """)
    long countActiveRegistrations(@Param("organizerId") Long organizerId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM (
                SELECT
                    """ + EventMapper.DISPLAY_STATUS_CASE + """
                    AS display_status
                FROM event e
                WHERE e.deleted = 0
                <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
            ) filtered
            WHERE filtered.display_status = #{displayStatus}
            </script>
            """)
    long countEventsByDisplayStatus(
            @Param("organizerId") Long organizerId,
            @Param("displayStatus") String displayStatus
    );

    @Select("""
            <script>
            SELECT
                e.id AS event_id,
                e.title,
                e.max_participants,
                e.current_participants,
                GREATEST(e.max_participants - e.current_participants, 0) AS remaining_participants,
                ROUND(
                    CASE
                        WHEN e.max_participants > 0 THEN e.current_participants * 100.0 / e.max_participants
                        ELSE 0
                    END,
                    2
                ) AS registration_rate,
                """ + EventMapper.DISPLAY_STATUS_CASE + """
                AS display_status
            FROM event e
            WHERE e.deleted = 0
            <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
            ORDER BY e.start_time DESC, e.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<EventStatisticsVO> pageEventStatistics(
            @Param("organizerId") Long organizerId,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    @Select("""
            <script>
            SELECT
                filtered.display_status AS status,
                COUNT(1) AS count
            FROM (
                SELECT
                    """ + EventMapper.DISPLAY_STATUS_CASE + """
                    AS display_status
                FROM event e
                WHERE e.deleted = 0
                <if test='organizerId != null'>AND e.organizer_id = #{organizerId}</if>
            ) filtered
            GROUP BY filtered.display_status
            </script>
            """)
    List<StatusDistributionVO> statusDistribution(@Param("organizerId") Long organizerId);
}
