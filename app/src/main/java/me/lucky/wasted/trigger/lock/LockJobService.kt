package me.lucky.wasted.trigger.lock

import android.app.job.JobParameters
import android.app.job.JobService

import me.lucky.wasted.Trigger
import me.lucky.wasted.fire.Fire

class LockJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Fire(this).fire(Trigger.LOCK)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean { return true }
}