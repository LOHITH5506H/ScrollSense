package com.lohith.scrollsense.data

class UsageRepository(private val dao: ContentSegmentDao) {

    private var activeSegmentId: Long? = null
    private var activePkg: String? = null
    private var activeType: String? = null

    suspend fun startSegment(pkg: String, type: String, now: Long) {
        // Close previous segment if any
        activeSegmentId?.let {
            dao.closeSegment(it, now)
        }

        // create new segment where endTimeMs initially equals startTimeMs (will be updated on close)
        val id = dao.insert(ContentSegment(
            packageName = pkg,
            contentType = type,
            startTimeMs = now,
            endTimeMs = now
        ))
        activeSegmentId = id
        activePkg = pkg
        activeType = type
    }

    /**
     * If the detected (pkg,type) differs from current active, start a new one.
     */
    suspend fun maybeSwitch(pkg: String, type: String, now: Long) {
        if (activeSegmentId == null) {
            startSegment(pkg, type, now)
            return
        }
        if (pkg != activePkg || type != activeType) {
            startSegment(pkg, type, now)
        } else {
            // optionally extend active segment's endTimeMs to 'now' to keep it current
            activeSegmentId?.let { dao.closeSegment(it, now) }
            // re-open same segment? simpler: insert a short 'update' step not necessary because we close at new start
            // but we want a continuous record; so re-insert a new segment starting at previous end -> now
            // For simplicity, we'll update endTimeMs for continuity already done.
        }
    }

    suspend fun stop(now: Long) {
        activeSegmentId?.let { dao.closeSegment(it, now) }
        activeSegmentId = null
        activePkg = null
        activeType = null
    }
}
