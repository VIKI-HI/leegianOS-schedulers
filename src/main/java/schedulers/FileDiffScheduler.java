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
import de.linzn.leegianOS.internal.interfaces.IScheduler;
import de.linzn.leegianOS.internal.interfaces.ISkill;
import de.linzn.leegianOS.internal.objectDatabase.TimeData;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FileDiffScheduler implements IScheduler {

    private boolean alive = false;
    private LinkedList<JSONObject> data = new LinkedList<>();
    private HashMap<String, String> skill_md5 = new HashMap<>();
    private HashMap<UUID, String> scheduler_md5 = new HashMap<>();
    private String prefix = this.getClass().getSimpleName() + "->";


    @Override
    public void scheduler() {

        for (File classFile : Objects.requireNonNull(new File("skills").listFiles())) {
            try {
                String class_name = classFile.getName().replace(".class", "");
                ClassLoader cl = new URLClassLoader(new URL[]{new File("").toURI().toURL()});
                Class<ISkill> iSkillClass = (Class<ISkill>) cl.loadClass("skills." + Character.toUpperCase(class_name.charAt(0)) + class_name.substring(1));

                String md_5 = getMD5(classFile);
                // todo check if file is deleted
                LeegianOSApp.logger(prefix + "checkHashsum->" + iSkillClass.getSimpleName() + "->" + md_5, false);
                if (this.skill_md5.containsKey(iSkillClass.getSimpleName())) {
                    if (this.skill_md5.get(iSkillClass.getSimpleName()).equalsIgnoreCase(md_5)) {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSkillClass.getSimpleName() + "->" + "no changes", false);
                    } else {
                        this.skill_md5.put(iSkillClass.getSimpleName(), md_5);
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSkillClass.getSimpleName() + "->" + "changes detected", true);
                        LeegianOSApp.leegianOSAppInstance.skillProcessor.loadSkill(iSkillClass, false);
                    }
                } else {
                    this.skill_md5.put(iSkillClass.getSimpleName(), md_5);
                    if (!LeegianOSApp.leegianOSAppInstance.skillProcessor.skillList.containsKey(iSkillClass.getSimpleName())) {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSkillClass.getSimpleName() + "->" + "new checksum", true);
                        LeegianOSApp.leegianOSAppInstance.skillProcessor.loadSkill(iSkillClass, false);
                    } else {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSkillClass.getSimpleName() + "->" + "initial checksum", false);
                    }
                }
            } catch (ClassNotFoundException | MalformedURLException e) {
                e.printStackTrace();
            }
        }

        for (File classFile : Objects.requireNonNull(new File("schedulers").listFiles())) {
            try {
                String class_name = classFile.getName().replace(".class", "");
                ClassLoader cl = new URLClassLoader(new URL[]{new File("").toURI().toURL()});
                Class<IScheduler> iSchedulerClass = (Class<IScheduler>) cl.loadClass("schedulers." + Character.toUpperCase(class_name.charAt(0)) + class_name.substring(1));

                IScheduler iScheduler = iSchedulerClass.newInstance();

                String md_5 = getMD5(classFile);
                // todo check if file is deleted
                LeegianOSApp.logger(prefix + "checkHashsum->" + iSchedulerClass.getSimpleName() + "->" + md_5, false);
                if (this.scheduler_md5.containsKey(iScheduler.schedulerUUID())) {
                    if (this.scheduler_md5.get(iScheduler.schedulerUUID()).equalsIgnoreCase(md_5)) {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSchedulerClass.getSimpleName() + "->" + "no changes", false);
                    } else {
                        this.scheduler_md5.put(iScheduler.schedulerUUID(), md_5);
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSchedulerClass.getSimpleName() + "->" + "changes detected", true);
                        LeegianOSApp.leegianOSAppInstance.schedulerProcessor.loadScheduler(iScheduler.schedulerUUID(), iSchedulerClass, false);
                    }
                } else {
                    this.scheduler_md5.put(iScheduler.schedulerUUID(), md_5);
                    if (!LeegianOSApp.leegianOSAppInstance.schedulerProcessor.schedulersList.containsKey(iScheduler.schedulerUUID())) {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSchedulerClass.getSimpleName() + "->" + "new checksum", true);
                        LeegianOSApp.leegianOSAppInstance.schedulerProcessor.loadScheduler(iScheduler.schedulerUUID(), iSchedulerClass, false);
                    } else {
                        LeegianOSApp.logger(prefix + "checkHashsum->" + iSchedulerClass.getSimpleName() + "->" + "initial checksum", false);
                    }
                }
            } catch (ClassNotFoundException | MalformedURLException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loopback() {

    }

    @Override
    public TimeData scheduler_timer() {
        return new TimeData(10, 30, TimeUnit.SECONDS);
    }

    @Override
    public void addAnswerData(JSONObject json) {
        this.data.add(json);
    }


    @Override
    public UUID schedulerUUID() {
        return UUID.fromString("d62e7d8d-c040-4f50-b323-d58de9188964");
    }

    @Override
    public boolean is_alive() {
        return this.alive;
    }

    @Override
    public void set_alive(boolean b) {
        this.alive = b;
    }


    private String getMD5(File file) {
        String md5 = null;

        FileInputStream fileInputStream;

        try {
            fileInputStream = new FileInputStream(file);

            md5 = DigestUtils.md5Hex(IOUtils.toByteArray(fileInputStream));

            fileInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return md5;
    }

}
