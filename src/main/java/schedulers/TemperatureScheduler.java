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
import de.linzn.whatsappApi.WhatsappClient;
import org.json.JSONArray;
import org.json.JSONObject;
import skills.ComputerTemplate;
import skills.WhatsappTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TemperatureScheduler implements IScheduler {

    private boolean terminated = false;
    private LinkedList<JSONObject> data = new LinkedList<>();
    private int[] heat = {30, 70, 80};
    private String testNumber = "";

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
    public void loopback() {
        checkValid();
        if (!data.isEmpty()) {
            JSONObject jsonObject = data.removeFirst();
            JSONArray jsonArray = jsonObject.getJSONObject("dataValues").getJSONArray("temperatures");
            double hotCore = -1;
            for (int i = 0; i < jsonArray.length(); i++){
                double value = jsonArray.getDouble(i);
                if (hotCore < value){
                    hotCore = value;
                }
            }
            if (hotCore >= heat[0]){

                SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
                WhatsappTemplate whatsappTemplate = new WhatsappTemplate();
                Map map = new HashMap();
                SubSkill subSkill = new SubSkill(0, null, null, null, null, null, map);
                whatsappTemplate.setEnv(schedulerSkillClient, null, subSkill);

                map.put("loginPhone", "xxx");
                map.put("loginPassphrase", "xxx");
                map.put("receiverPhone", "xxx");

                if (hotCore >= heat[1]){
                    if (hotCore >= heat[2]){
                        map.put("message", "CRITICAL: Core temp over " + hotCore + "°C");
                    } else {
                        map.put("message", "WARNING: Core over " + hotCore + "°C");
                    }
                } else {
                    map.put("message", "INFO: Core over " + hotCore + "°C");
                }
                whatsappTemplate.sendPhoneMessage();
            }
        }
    }

    @Override
    public void addAnswerData(JSONObject json) {
        this.data.add(json);
    }


    @Override
    public void terminateScheduler() {
        terminated = true;
    }

    @Override
    public UUID schedulerUUID() {
        return UUID.fromString("8d6c0b8f-9930-4f47-b516-45f116e161e4");
    }


    private void checkValid() {
        if (terminated) {
            LeegianOSApp.logger(this.getClass().getSimpleName() + "->" + "terminate old scheduler ");
            Thread.currentThread().interrupt();
            Thread.currentThread().stop();
        }
    }
}
