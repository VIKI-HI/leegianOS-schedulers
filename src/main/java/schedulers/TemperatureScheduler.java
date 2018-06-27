/*
 * Copyright (C) 2018. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 */

package schedulers;

import de.linzn.leegianOS.LeegianOSApp;
import de.linzn.leegianOS.internal.databaseAccess.GetSetting;
import de.linzn.leegianOS.internal.interfaces.IScheduler;
import de.linzn.leegianOS.internal.objectDatabase.OBJSetting;
import de.linzn.leegianOS.internal.objectDatabase.TimeData;
import de.linzn.leegianOS.internal.objectDatabase.clients.SchedulerSkillClient;
import de.linzn.leegianOS.internal.objectDatabase.skillType.SecondarySkill;
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

    private boolean alive = false;
    private LinkedList<JSONObject> data = new LinkedList<>();
    private int[] heat = {70, 80, 90};
    private int last = 0;

    @Override
    public void scheduler() {

        SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
        ComputerTemplate computerTemplate = new ComputerTemplate();
        Map map = new HashMap();
        SecondarySkill secondarySkill = new SecondarySkill(0, null, null, null, null, null, map);
        computerTemplate.setEnv(schedulerSkillClient, null, secondarySkill);

        OBJSetting objSetting = new GetSetting("scheduler.temperature.hostSystem").getSetting();
        map.put("hostName", objSetting.dataObject.getString("host_name"));
        computerTemplate.getSystemTemperature();
    }

    @Override
    public void loopback() {

        if (!data.isEmpty()) {
            JSONObject jsonObject = data.removeFirst();
            JSONArray jsonArray = jsonObject.getJSONObject("dataValues").getJSONArray("temperatures");
            double hotCore = -1;
            for (int i = 0; i < jsonArray.length(); i++) {
                double value = jsonArray.getDouble(i);
                if (hotCore < value) {
                    hotCore = value;
                }
            }
            if (hotCore >= heat[0]) {
                SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
                WhatsappTemplate whatsappTemplate = new WhatsappTemplate();
                Map map = new HashMap();
                SecondarySkill secondarySkill = new SecondarySkill(0, null, null, null, null, null, map);
                whatsappTemplate.setEnv(schedulerSkillClient, null, secondarySkill);

                OBJSetting objSetting = new GetSetting("scheduler.temperature.phone").getSetting();
                map.put("loginPhone", objSetting.dataObject.getString("login_phone"));
                map.put("loginPassphrase", objSetting.dataObject.getString("login_passphrase"));
                map.put("receiverPhone", objSetting.dataObject.getString("receiver_number"));

                if (hotCore >= heat[1]) {
                    if (hotCore >= heat[2]) {
                        if (last < 3) {
                            last = 3;
                            map.put("message", "Die Temperatur des Hostsystems liegt mit " + hotCore + "°C im kritischen Bereich!");
                            whatsappTemplate.sendPhoneMessage();
                        }
                    } else {
                        if (last < 2) {
                            last = 2;
                            map.put("message", "Die Temperatur des Prozessors ist gefährlich heiß. " + hotCore + "°C");
                            whatsappTemplate.sendPhoneMessage();
                        }
                    }
                } else {
                    if (last < 1) {
                        last = 1;
                        map.put("message", "Der Prozessor des Hostsystems ist mit " + hotCore + "°C ungewöhnlich heiß.");
                        whatsappTemplate.sendPhoneMessage();
                    }
                }
            } else {

                if (last > 0) {
                    last = 0;
                    SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
                    WhatsappTemplate whatsappTemplate = new WhatsappTemplate();
                    Map map = new HashMap();
                    SecondarySkill secondarySkill = new SecondarySkill(0, null, null, null, null, null, map);
                    whatsappTemplate.setEnv(schedulerSkillClient, null, secondarySkill);

                    OBJSetting objSetting = new GetSetting("scheduler.temperature.phone").getSetting();
                    map.put("loginPhone", objSetting.dataObject.getString("login_phone"));
                    map.put("loginPassphrase", objSetting.dataObject.getString("login_passphrase"));
                    map.put("receiverPhone", objSetting.dataObject.getString("receiver_number"));

                    map.put("message", "Die Temperatur des Prozessors ist wieder normal. " + hotCore + "°C");
                    whatsappTemplate.sendPhoneMessage();
                }
            }
        }
    }

    @Override
    public TimeData scheduler_timer() {
        return new TimeData(10, 20, TimeUnit.SECONDS);
    }

    @Override
    public void addAnswerData(JSONObject json) {
        this.data.add(json);
    }


    @Override
    public UUID schedulerUUID() {
        return UUID.fromString("8d6c0b8f-9930-4f47-b516-45f116e161e4");
    }

    @Override
    public boolean is_alive() {
        return this.alive;
    }

    @Override
    public void set_alive(boolean b) {
        this.alive = b;
    }

}
