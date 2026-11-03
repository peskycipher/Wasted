package me.lucky.wasted.trigger.geofence

import android.content.Context
import org.json.JSONObject

class StationStore(private val ctx: Context) {
    data class Station(val lon: Double, val lat: Double)

    private var stations: List<Station>? = null

    fun get(): List<Station> {
        stations?.let { return it }
        val arr = JSONObject(
            ctx.assets.open("police_stations.json").bufferedReader().readText()
        ).getJSONArray("stations")
        val list = ArrayList<Station>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // only operational facilities are fenced; non-operational are
            // historic points that may no longer be police premises
            if (o.optString("s") != "OPERATIONAL") continue
            val c = o.getJSONArray("c")
            list.add(Station(c.getDouble(0), c.getDouble(1)))
        }
        stations = list
        return list
    }
}