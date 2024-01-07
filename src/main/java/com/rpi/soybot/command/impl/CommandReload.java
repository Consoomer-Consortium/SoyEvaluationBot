package com.rpi.soybot.command.impl;

import com.rpi.soybot.command.Command;
import com.rpi.soybot.command.CommandListener;
import com.rpi.soybot.database.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class CommandReload extends Command {

    public CommandReload(CommandListener commandListener) {
        super("reload", "Reload the questions JSON file", commandListener);
    }

    @Override
    public void onUse(JDA jda, SlashCommandInteractionEvent event) throws SQLException {
        Database database = this.getDatabase();

        try {
            int questionCount = database.setupDefaultQuestions();

            event.reply(questionCount + " Questions reloaded").queue();
        }
        catch(Exception e) {
            e.printStackTrace();
            event.reply("There was an error!").queue();
        }
    }

}
