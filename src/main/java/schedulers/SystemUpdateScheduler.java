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
import de.linzn.leegianOS.internal.objectDatabase.TimedTimeData;
import de.linzn.leegianOS.internal.objectDatabase.clients.SchedulerSkillClient;
import de.linzn.leegianOS.internal.objectDatabase.skillType.SecondarySkill;
import org.json.JSONObject;
import skills.UnixTemplate;
import skills.WhatsappTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class SystemUpdateScheduler implements IScheduler {

    private boolean alive = false;
    private LinkedList<JSONObject> data = new LinkedList<>();
    private String prefix = this.getClass().getSimpleName() + "->";


    @Override
    public void scheduler() {

        OBJSetting objSetting = new GetSetting("scheduler.systemupdate.hostnames").getSetting();
        LeegianOSApp.logger(prefix + "updateSystem->" + "prepare", true);
        for (int i = 0; i < objSetting.dataObject.getJSONArray("host_names").length(); i++) {
            JSONObject object = objSetting.dataObject.getJSONArray("host_names").getJSONObject(i);

            SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
            UnixTemplate unixTemplate = new UnixTemplate();
            Map map = new HashMap();
            SecondarySkill secondarySkill = new SecondarySkill(0, null, null, null, null, null, map);
            unixTemplate.setEnv(schedulerSkillClient, null, secondarySkill);
            map.put("hostName", object.getString("host_name"));
            map.put("port", object.getInt("port"));
            LeegianOSApp.logger(prefix + "updateSystem->" + object.getString("host_name") + ":" + object.getInt("port") + "->" + "upgrade", true);
            unixTemplate.upgradeUnixSystem();
        }
    }

    @Override
    public void loopback() {
        if (!data.isEmpty()) {
            JSONObject jsonObject = data.removeFirst();
            int exitCode = jsonObject.getJSONObject("dataValues").getInt("exitCode");
            String hostname = jsonObject.getJSONObject("dataValues").getString("hostname");
            int port = jsonObject.getJSONObject("dataValues").getInt("port");
            LeegianOSApp.logger(prefix + "updateSystem->" + hostname + ":" + port + "->" + "complete->::" + exitCode, true);
            if (exitCode != 0) {
                SchedulerSkillClient schedulerSkillClient = (SchedulerSkillClient) LeegianOSApp.leegianOSAppInstance.skillClientList.get(schedulerUUID());
                WhatsappTemplate whatsappTemplate = new WhatsappTemplate();
                Map map = new HashMap();
                SecondarySkill secondarySkill = new SecondarySkill(0, null, null, null, null, null, map);
                whatsappTemplate.setEnv(schedulerSkillClient, null, secondarySkill);

                OBJSetting objSetting = new GetSetting("scheduler.systemupdate.phone").getSetting();
                map.put("loginPhone", objSetting.dataObject.getString("login_phone"));
                map.put("loginPassphrase", objSetting.dataObject.getString("login_passphrase"));
                map.put("receiverPhone", objSetting.dataObject.getString("receiver_number"));
                map.put("message", "Es ist ein Fehler (Code: " + exitCode + ") beim Systemupdate von " + hostname + ":" + port + " aufgetreten!");
                whatsappTemplate.sendPhoneMessage();
            }
        }
    }

    @Override
    public TimeData scheduler_timer() {
        return new TimedTimeData(0, 2, 0);
    }


    @Override
    public void addAnswerData(JSONObject json) {
        this.data.add(json);
    }


    @Override
    public UUID schedulerUUID() {
        return UUID.fromString("efb91c90-f73f-4d83-ac3e-9080b174e253");
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
