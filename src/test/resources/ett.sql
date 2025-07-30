SELECT
    TO_CHAR(sn.BEGIN_INTERVAL_TIME, 'DD-MON HH24:MI') AS interval_start,
    ROUND(SUM(ash_cpu.cpu_count) / COUNT(*), 2) AS estimated_cpus_needed
FROM (
    SELECT snap_id,
           COUNT(*) AS cpu_count
    FROM dba_hist_active_sess_history
    WHERE session_state = 'ON CPU'
    GROUP BY snap_id
) ash_cpu
JOIN dba_hist_snapshot sn ON sn.snap_id = ash_cpu.snap_id
WHERE sn.BEGIN_INTERVAL_TIME > SYSDATE - 1
GROUP BY TO_CHAR(sn.BEGIN_INTERVAL_TIME, 'DD-MON HH24:MI')
ORDER BY interval_start;