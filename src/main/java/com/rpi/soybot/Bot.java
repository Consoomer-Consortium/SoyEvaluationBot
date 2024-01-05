package com.rpi.soybot;

import com.rpi.soybot.command.CommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.io.IOException;
import java.sql.SQLException;

public class Bot {

    public Bot(String token) throws InterruptedException, SQLException, IOException {
        JDA jda = JDABuilder.createDefault(token).build().awaitReady();

        jda.addEventListener(CommandListener.getInstance(jda));
    }

}
