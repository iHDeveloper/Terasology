/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.logic.title;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.console.Console;
import org.terasology.logic.console.CoreMessageType;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;

@RegisterSystem(RegisterMode.ALWAYS)
public class TitleSystem extends BaseComponentSystem implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TitleSystem.class);

    private static final ResourceUrn UI_URL = new ResourceUrn("engine:title");

    @In
    private Console console;

    @In
    private NUIManager nuiManager;

    private TitleScreen titleScreen;
    private Thread thread;
    private float currentStay;
    private boolean alive;
    private String lastTitle = "";
    private String lastSubtitle = "";

    @Override
    public void initialise() {
        thread = new Thread(this);
        thread.setName("TITLE-STAY");
        thread.start();
        alive = true;
        logger.info("Initialised the title system!");
    }

    @Override
    public void shutdown() {
        alive = false;
        thread.interrupt();
        logger.info("Successfully! shut down the title system!");
    }

    @Override
    public void run() {
        while (alive) {
            if (currentStay <= 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) { } // Ignore because this may happen because of #shutdown() which it doesn't matter
                continue;
            }
            currentStay -= 1f;
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) { } // Ignore because this may happen because of #shutdown() which it doesn't matter
            if (currentStay <= 0 && (titleScreen != null)) {
                currentStay = 0;
                hide();
                continue;
            }
        }
        currentStay = 0; // Make sure to make it 0 even if the title got hidden
    }

    @Command(
            value = "title",
            shortDescription = "Use the title feature",
            helpText = "<title:subtitle:stay:reset> <value>"
    )
    public void titleCommand(@CommandParam("type") String type, @CommandParam(value = "value", required = false) String value) {
        if (type.equalsIgnoreCase("reset")) {
            if (titleScreen != null) {
                hide();
                console.addMessage("Done! Reset the title screen.");
                return;
            }
        }
        if (value != null) {
            if (type.equalsIgnoreCase("title")) {
                show(value, lastSubtitle, currentStay);
                lastTitle = value;
                console.addMessage("Done! The current title value is " + value);
            } else if (type.equalsIgnoreCase("subtitle")) {
                show(lastTitle, value, currentStay);
                lastSubtitle = value;
                console.addMessage("Done! The current subtitle value is " + value);
            } else if (type.equalsIgnoreCase("stay")) {
                try {
                    float stay = Float.parseFloat(value);
                    show(lastTitle, lastSubtitle, stay);
                    console.addMessage("Done! The current stay value is " + stay);
                } catch (NumberFormatException ex) {
                    console.addMessage("I can't understand the stay value", CoreMessageType.ERROR);
                }
            }
        } else {
            console.addMessage("Failed! Can't find the value to use it.");
        }
    }

    /**
     * Show a title screen to the player.
     *
     * @param title will be shown in bigger font
     * @param subtitle will be shown in small font
     * @param stay how much do you wanna from it to stay for the player ( in milliseconds )
     */
    public void show(String title, String subtitle, float stay) {
        build();
        currentStay = stay;
        titleScreen.setTitle(title);
        titleScreen.setSubtitle(subtitle);
        titleScreen.update();
    }

    /**
     * Reset the title screen
     */
    public void hide() {
        build();
        titleScreen.setTitle("");
        titleScreen.setSubtitle("");
        titleScreen.update();
        nuiManager.removeOverlay(UI_URL);
        titleScreen = null;
        currentStay = 0;
    }

    private void build() {
        if (titleScreen == null) {
            titleScreen = nuiManager.addOverlay(UI_URL, TitleScreen.class);
        }
    }

}
