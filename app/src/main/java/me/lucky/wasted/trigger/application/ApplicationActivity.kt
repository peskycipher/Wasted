package me.lucky.wasted.trigger.application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import me.lucky.wasted.Trigger
import me.lucky.wasted.fire.Fire

class ApplicationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fire(this).fire(Trigger.APPLICATION)
        finishAndRemoveTask()
    }
}