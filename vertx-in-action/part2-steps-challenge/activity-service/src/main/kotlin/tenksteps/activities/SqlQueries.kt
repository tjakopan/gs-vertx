package tenksteps.activities

internal object SqlQueries {
  internal val insertStepEvent = "insert into stepevent values($1, $2, current_timestamp, $3)"

  internal val stepsCountForToday = "select current_timestamp, coalesce(sum(steps_count), 0) " +
      "from stepevent " +
      "where device_id = $1 and date_trunc('day', sync_timestamp) = date_trunc('day', current_timestamp)"

  internal val totalStepsCount = "select sum(steps_count) from stepevent where device_id = $1"

  internal val monthlyStepsCount = "select sum(steps_count) " +
      "from stepevent " +
      "where device_id = $1 and date_trunc('month', sync_timestamp) = $2::timestamp"

  internal val dailyStepsCount = "select sum(steps_count) " +
      "from stepevent " +
      "where device_id = $1 and date_trunc('day', sync_timestamp) = $2::timestamp"

  internal val rankingLast24Hours = "select device_id, sum(steps_count) as steps " +
      "from stepevent " +
      "where now() - sync_timestamp <= interval '24 hours' " +
      "group by device_id " +
      "order by steps desc"
}