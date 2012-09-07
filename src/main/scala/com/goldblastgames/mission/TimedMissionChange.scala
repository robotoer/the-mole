package com.goldblastgames.mission

import java.util.Timer
import java.util.TimerTask

import com.github.oetzi.echo.core.EventSource

// TODO(Issue-36): actually generate missions
class TimedMissionChange(period: Long) extends EventSource[Mission] {
  val timer = new Timer() 
    class MissionTimerTask() extends TimerTask {
      override def run(): Unit = {
        val mission = Mission.dummyMission
        occur(mission)
      }
    }

    // Wait _period_ milliseconds before starting
    timer.schedule(new MissionTimerTask(), period, period)  
}

