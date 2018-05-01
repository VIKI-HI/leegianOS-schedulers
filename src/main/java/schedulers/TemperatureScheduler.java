/*
 * Copyright (C) 2017. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package schedulers;

import de.linzn.leegianOS.LeegianOSApp;
import de.linzn.leegianOS.internal.ifaces.IScheduler;
import de.linzn.leegianOS.internal.lifeObjects.SchedulerSkillClient;
import de.linzn.leegianOS.internal.lifeObjects.SubSkill;
import org.json.JSONObject;
import skills.ComputerTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TemperatureScheduler implements IScheduler {

    private boolean terminated = false;

    @Override
    public void loadScheduler() {
        LeegianOSApp.leegianOSAppInstance.heartbeat.runRepeatTaskAsynchronous(() -> {
            checkValid();
            SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
            ComputerTemplate computerTemplate = new ComputerTemplate();
            Map map = new HashMap();
            SubSkill subSkill = new SubSkill(0, null, null, null, null, null, map);
            computerTemplate.setEnv(schedulerSkillClient, null, subSkill);

            map.put("hostName", "10.40.0.20");
            computerTemplate.getSystemTemperature();
        }, 10, 20, TimeUnit.SECONDS);
    }


    @Override
    public void terminateScheduler() {
        terminated = true;
    }

    @Override
    public UUID schedulerUUID() {
        return UUID.fromString("8d6c0b8f-9930-4f47-b516-45f116e161e4");
    }

    @Override
    public void addAnswerData(JSONObject data) {
        boolean needResponse = data.getJSONObject("dataValues").getBoolean("needResponse");
        String notificationText = data.getJSONObject("textValues").getString("notificationText");
        System.out.println("Answer:" +  notificationText);
    }

    private void checkValid() {
        if (terminated) {
            LeegianOSApp.logger(this.getClass().getSimpleName() + "->" + "terminate old scheduler ");
            Thread.currentThread().interrupt();
            Thread.currentThread().stop();
            return;
        }
    }
}
